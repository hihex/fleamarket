package hihex.fleamarket
import com.android.build.gradle.BaseExtension
import com.android.builder.core.BuilderConstants
import com.android.builder.signing.SignedJarBuilder
import com.android.ide.common.signing.KeystoreHelper
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import hihex.fleamarket.model.Channel
import hihex.fleamarket.model.ResValue
import hihex.fleamarket.utils.FileOps
import hihex.fleamarket.utils.LoggerAppendable
import hihex.fleamarket.utils.Xml
import hihex.fleamarket.utils.Zip
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.FileTree
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.*
import org.w3c.dom.Element
import org.w3c.dom.Node

import java.nio.file.Files
import java.util.jar.JarEntry
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@CompileStatic
class AssembleChannelTask extends DefaultTask {
    private static final Pattern IGNORED_ASSET_PATTERN = ~/^\.|\.scc$|^(?i:thumbs\.db|CVS|picasa.ini)$/

    Channel channel

    File buildTools
    File androidJar

    @Optional
    @InputFiles
    FileTree newResources

    @InputFiles
    Collection<File> newAssets

    @InputDirectory
    File oldResources

    @InputFile
    File oldManifest

    @InputFiles
    @Optional
    List<File> oldApks

    @OutputFile
    File outputFile

    void initialize(final Channel newChannel) {
        channel = newChannel

        updateSdk()

        final outputFilename = newChannel.filename.call(newChannel)
        outputFile = new File(project.buildDir, "outputs/flea-market/${outputFilename}")

        newResources = (FileTree) newChannel.resources.sum { project.fileTree(it) }
        newAssets = newChannel.assets.values()

        final oldResDir = newChannel.intermediateDirName
        oldResources = new File(project.buildDir, "intermediates/res/merged/${oldResDir}")
        oldManifest = new File(project.buildDir, "intermediates/manifests/full/${oldResDir}/AndroidManifest.xml")
        oldApks = newChannel.getInputApks(project)

        dependsOn(newChannel.packageTaskName)
    }

    @TaskAction
    void assemble() {
        // Make sure the intermediate folders are ready.
        outputFile.parentFile.mkdirs()
        final tempDir = temporaryDir

        FileOps.clearDirectory(tempDir)

        writeManifest(tempDir)
        writeResValues(tempDir)
        copyAssets(tempDir)

        def apk = aapt(tempDir)
        apk = sign(tempDir, apk)
        zipAlign(apk)
    }

    private void updateSdk() {
        // Ref: http://stackoverflow.com/a/28161842/ to get the SDK directory.
        final android = (BaseExtension) project.property('android')
        android.with {
            androidJar = new File(sdkDirectory, "platforms/$compileSdkVersion/android.jar")
            buildTools = new File(sdkDirectory, "build-tools/$buildToolsVersion")

            assert androidJar.isFile()
            assert buildTools.isDirectory()

            if (!channel.signingConfig) {
                channel.signingConfig = signingConfigs.findByName(BuilderConstants.DEBUG)
            }
        }
    }

    private void writeManifest(final File tempDir) {
        final manifest = Xml.read(oldManifest)
        channel.manifestModifications.each {
            it.execute(manifest, channel)
        }
        new File(tempDir, 'AndroidManifest.xml').withWriter {
            Xml.write(manifest, it)
        }
    }

    private void writeResValues(final File tempDir) {
        new File(tempDir, 'res').mkdirs()

        oldResources.listFiles((FilenameFilter) { dir, String fn -> fn.startsWith('values') }).each { File folder ->
            final folderName = folder.name
            final String qualifier = (folderName == 'values') ? null : folderName[7..-1]
            final values = Xml.read(new File(folder, "${folderName}.xml"))



            values.documentElement.childNodes.each { Node node ->
                if (node.nodeType == Node.ELEMENT_NODE) {
                    final element = (Element) node
                    if (element.tagName == 'declare-styleable') {
                        element.parentNode.removeChild(element)
                    } else if (element.hasAttributeNS(null, 'name')) {
                        final resValue = new ResValue(qualifier, element)
                        channel.valueReplacements.each {
                            it.execute(resValue, channel)
                        }
                    }
                }
            }

            new File(tempDir, "res/${folderName}/values.xml").with {
                parentFile.mkdirs()
                withWriter { Xml.write(values, it) }
            }
        }
    }

    private void copyAssets(final File tempDir) {
        new File(tempDir, "assets").mkdir()
        channel.assets.each { targetName, source ->
            final target = new File(tempDir, "assets/$targetName")
            target.parentFile.mkdirs()

            final sourcePath = source.absoluteFile.toPath()
            final targetPath = target.toPath()
            try {
                Files.createSymbolicLink(targetPath, sourcePath)
            } catch (UnsupportedOperationException ignored) {
                FileOps.copyRecursively(source, target)
            }
        }
    }

    private File aapt(final File tempDir) {
        checkResourceSubset()

        final unalignedApk = new File(tempDir, 'unaligned.apk')
        final oldApk = oldApks.find { it.isFile() }

        // We need to clear the file modification time of the zip file to ensure updating is successful.
        Files.copy(oldApk.toPath(), unalignedApk.toPath())
        Zip.clearTimestamps(unalignedApk)

        execute([
                new File(buildTools, 'aapt'), 'p', '-u', '-v', '--no-crunch',
                '-M', "$tempDir/AndroidManifest.xml",
                '-F', unalignedApk,
                '-I', androidJar,
                '-A', "$tempDir/assets",
                '-S', "$tempDir/res",
                *channel.resources.collect { ['-S', it] }.flatten(),
                '-S', oldResources
        ])

        unalignedApk
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private File sign(final File tempDir, final File unsignedApk) {
        final certInfo = channel.signingConfig.with {
            KeystoreHelper.getCertificateInfo(storeType, storeFile, storePassword, keyPassword, keyAlias)
        }

        final signedApk = new File(tempDir, 'signed.apk')

        signedApk.withOutputStream { output ->
            final builder = new SignedJarBuilder(output, certInfo.key, certInfo.certificate, '1.3.0', 'Android Gradle 1.3.0')

            // There is some bug with the ZipInputStream handling the file generated by aapt: The AndroidManifest.xml
            // entry is duplicated. Using ZipFile won't cause this problem. Since SignedJarBuilder.writeZip() uses
            // ZipInputStream, we cannot use this convenient method.

            final zf = new ZipFile(unsignedApk)
            zf.entries().each { ZipEntry entry ->
                final jarEntry

                switch (entry.name) {
                case 'META-INF/MANIFEST.MF':
                case ~/META-INF\/\w+.(?:SF|RSA|DSA|EC)/:
                    break
                default:
                    jarEntry = (entry.method == JarEntry.STORED) ? new JarEntry(entry) : new JarEntry(entry.name)
                    builder.invokeMethod('writeEntry', [zf.getInputStream (entry), jarEntry])
                    break
                }
            }
            builder.close()
        }

        signedApk
    }

    private void zipAlign(final File signedApk) {
        // For some unknown reason we may need to call 'zipalign' multiple times to correctly align file. Perhaps
        // related to the double-entry bug mentioned above.

        execute([new File(buildTools, 'zipalign'), '-v', '-f', '4', signedApk, outputFile])
    }

    private void execute(final List args) {
        logger.info(args.join(' '))

        args.execute().with {
            final stdout = new LoggerAppendable(logger, LogLevel.DEBUG)
            final stderr = new LoggerAppendable(logger, LogLevel.ERROR)
            waitForProcessOutput(stdout, stderr)
            assert exitValue() == 0
        }
    }

    private void checkResourceSubset() {
        final oldResPath = oldResources.toPath()
        channel.resources.each { res ->
            final resPath = res.toPath()
            res.eachDir { d ->
                if (d.name.startsWith('values')) {
                    throw new InvalidUserDataException("Cannot define resource values here, because we need to ensure R.java remains unchanged. Remove `$d` and use the values{} block instead.")
                }
                d.eachFile { f ->
                    if (!isIgnored(f.name)) {
                        final relPath = resPath.relativize(f.toPath())
                        final oldPath = oldResPath.resolve(relPath)
                        if (!Files.exists(oldPath)) {
                            throw new InvalidUserDataException("Cannot create a new resource `$f`, because we need to ensure R.java remains unchanged.")
                        }
                    }
                }
            }
        }
    }

    private static boolean isIgnored(final String filename) {
        filename =~ IGNORED_ASSET_PATTERN
    }
}
