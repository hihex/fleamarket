package hihex.fleamarket
import com.android.build.gradle.BaseExtension
import com.android.builder.core.BuilderConstants
import com.android.builder.signing.SignedJarBuilder
import com.android.ide.common.signing.KeystoreHelper
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import hihex.fleamarket.model.Channel
import hihex.fleamarket.model.ResValue
import hihex.fleamarket.utils.Files
import hihex.fleamarket.utils.LoggerAppendable
import hihex.fleamarket.utils.Xml
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.*
import org.w3c.dom.Element
import org.w3c.dom.Node

import java.util.jar.JarEntry
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@CompileStatic
class AssembleChannelTask extends DefaultTask {
    Channel channel

    File sdk

    String apiLevel

    @Optional
    @InputFiles
    FileTree newResources

    @InputFiles
    Collection<File> newAssets

    @InputDirectory
    File oldResources

    @InputFile
    File oldManifest

    @InputFile
    File oldApk

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
        oldApk = newChannel.getInputApk(project)

        dependsOn(newChannel.packageTaskName)
    }

    @TaskAction
    void assemble() {
        // Make sure the intermediate folders are ready.
        outputFile.parentFile.mkdirs()
        final tempDir = temporaryDir

        FileUtils.cleanDirectory(tempDir)

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
            sdk = sdkDirectory
            apiLevel = compileSdkVersion
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
                    if (element.hasAttributeNS(null, 'name')) {
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
            Files.linkOrCopy(source, target)
        }
    }

    private File aapt(final File tempDir) {
        final unalignedApk = new File(tempDir, 'unaligned.apk')
        FileUtils.copyFile(oldApk, unalignedApk)

        execute([
                "$sdk/bin/aapt", 'p', '-u',
                '-M', "$tempDir/AndroidManifest.xml",
                '-F', unalignedApk,
                '-I', "$sdk/platforms/$apiLevel/android.jar",
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
                final jarEntry = (entry.method == JarEntry.STORED) ? new JarEntry(entry) : new JarEntry(entry.name)
                builder.invokeMethod('writeEntry', [zf.getInputStream(entry), jarEntry])
            }
            builder.close()
        }

        signedApk
    }

    private void zipAlign(final File signedApk) {
        // For some unknown reason we may need to call 'zipalign' multiple times to correctly align file. Perhaps
        // related to the double-entry bug mentioned above.

        execute(["$sdk/bin/zipalign", '-f', '4', signedApk, outputFile])
        /*
        while () {
            signedApk.delete()
            outputFile.renameTo(signedApk)
        }
        */
    }

    private void execute(final List args) {
        logger.info(args.join(' '))

        args.execute().with {
            final stdout = new LoggerAppendable(logger, LogLevel.INFO)
            final stderr = new LoggerAppendable(logger, LogLevel.ERROR)
            waitForProcessOutput(stdout, stderr)
            assert exitValue() == 0
        }
    }
}
