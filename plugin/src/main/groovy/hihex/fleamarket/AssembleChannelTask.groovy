package hihex.fleamarket

import com.android.builder.signing.SignedJarBuilder
import com.android.ide.common.signing.KeystoreHelper
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import hihex.fleamarket.model.Channel
import hihex.fleamarket.model.ResValue
import hihex.fleamarket.utils.Files
import hihex.fleamarket.utils.Xml
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.*
import org.w3c.dom.Element
import org.w3c.dom.Node


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

        writeManifest(tempDir)
        writeResValues(tempDir)
        copyAssets(tempDir)

        def apk = aapt(tempDir)
        if (channel.signingConfig) {
            apk = sign(tempDir, apk)
        }
        zipAlign(apk)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void updateSdk() {
        // Ref: http://stackoverflow.com/a/28161842/ to get the SDK directory.
        sdk = project.android.sdkDirectory
        apiLevel = project.android.compileSdkVersion
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
        Files.copy(oldApk, unalignedApk)
        final List commandLine = [
                "$sdk/bin/aapt", 'p', '-u',
                '-F', unalignedApk,
                '-M', "$tempDir/AndroidManifest.xml",
                '-A', "$tempDir/assets",
                '-I', "$sdk/platforms/$apiLevel/android.jar",
                '-S', "$tempDir/res",
        ]
        channel.resources.each { folder ->
            commandLine += ['-S', folder]
        }
        commandLine += ['-S', oldResources]

        final waitResult = commandLine.execute().waitFor()
        assert waitResult == 0

        unalignedApk
    }

    private File sign(final File tempDir, final File unsignedApk) {
        final certInfo = channel.signingConfig.with {
            KeystoreHelper.getCertificateInfo(storeType, storeFile, storePassword, keyPassword, keyAlias)
        }

        final signedApk = new File(tempDir, "signed.apk")
        signedApk.withOutputStream { output ->
            final builder = new SignedJarBuilder(output, certInfo.key, certInfo.certificate, '1.3.0', 'Android Gradle 1.3.0')
            unsignedApk.withInputStream { builder.writeZip(it) }
            builder.clone()
        }

        signedApk
    }

    private void zipAlign(final File signedApk) {
        ["$sdk/bin/zipalign", '-f', '4', signedApk, outputFile].execute().waitFor()
    }
}
