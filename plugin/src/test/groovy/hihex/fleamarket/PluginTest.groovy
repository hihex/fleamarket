package hihex.fleamarket

import hihex.fleamarket.utils.FileOps
import nebula.test.IntegrationSpec
import org.gradle.api.logging.LogLevel

import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

class PluginTest extends IntegrationSpec {
    @Override
    protected LogLevel getLogLevel() {
        LogLevel.INFO
    }

    def setup() {
        buildFile << '''
            apply plugin: 'com.android.application'
            apply plugin: 'hihex.fleamarket'

            android {
                compileSdkVersion 22
                buildToolsVersion '22.0.1'

                defaultConfig {
                    applicationId 'com.example.testapk'
                    minSdkVersion 15
                    targetSdkVersion 22
                    versionName '1.0'
                    versionCode 1
                }

                lintOptions {
                    checkReleaseBuilds false
                }

                signingConfigs {
                    release {
                        storeFile file('keystore')
                        storePassword 'aaaaaa'
                        keyAlias 'mykey'
                        keyPassword 'aaaaaa'
                    }
                }
        '''

        try {
            FileOps.copyRecursivelyToDirectory(new File('../local.properties'), projectDir)
        } catch (final NoSuchFileException ignored) {
        }

        FileOps.copyRecursivelyToDirectory(new File('../testapk/keystore'), projectDir)
        FileOps.copyRecursivelyToDirectory(new File('../testapk/src'), projectDir)
    }

    def 'applying FleaMarket plugin does not affect APK assembly'() {
        given:
        buildFile << '}'

        when:
        runTasksSuccessfully('assembleDebug')

        then:
        fileExists('build/outputs/apk/applying-FleaMarket-plugin-does-not-affect-APK-assembly-debug.apk')
    }

    def 'defining channels extension does not affect normal APK assembly'() {
        given:
        buildFile << '''
            }

            channels {
                defaultConfig {}
                create 'A', 'B', 'C', 'D'
            }
        '''

        when:
        runTasksSuccessfully('assembleDebug')

        then:
        fileExists('build/outputs/apk/defining-channels-extension-does-not-affect-normal-APK-assembly-debug.apk')
    }

    def 'list channels'() {
        given:
        buildFile << '''
            }

            channels {
                defaultConfig {}
                create 'AChannel', 'BChannel', 'CChannel'
                create('EChannel') {}
                create('FChannel') {}
            }
        '''

        when:
        final result = runTasksSuccessfully('listChannels')

        then:
        final lines = result.standardOutput.readLines()
        lines.containsAll(['AChannel', 'BChannel', 'CChannel', 'EChannel', 'FChannel'])
    }

    def 'generate two channels'() {
        given:
        buildFile << '''
            }

            channels {
                defaultConfig {
                    filename { "${it.name}-v0.1.apk" }
                }
                create 'AChannel', 'BChannel'
            }
        '''

        when:
        runTasksSuccessfully('assembleAChannel')

        then:
        fileExists('build/outputs/flea-market/AChannel-v0.1.apk')

        when:
        runTasksSuccessfully('assembleBChannel')

        then:
        fileExists('build/outputs/flea-market/BChannel-v0.1.apk')
    }

    def 'modify AndroidManifest.xml'() {
        given:
        buildFile << '''
            }

            channels {
                defaultConfig {}
                create('my-channel') {
                    filename 'MyChannel.apk'
                    manifest { c ->
                        analyticsChannel = c.name
                        deleteTagsWithName 'activity', 'com.example.testapk.FirstActivity'
                        addUsesPermission 'com.example.TEST_PERMISSION'
                    }
                }
            }
        '''

        when:
        runTasksSuccessfully('assembleMy-channel')

        then:
        fileExists('build/outputs/flea-market/MyChannel.apk')

        when:
        final aaptResult = ['aapt', 'l', '-a', file('build/outputs/flea-market/MyChannel.apk')].execute().text

        then:
        aaptResult =~ /A: android:name\([^)]+\)="MY_CHANNEL" \([^)]+\)\s*A: android:value\([^)]+\)="my-channel"/
        !(aaptResult =~ /E: activity \([^)]+\)\s*A: android:name\([^)]+\)="com\.example\.testapk\.FirstActivity"/)
        aaptResult =~ /E: activity \([^)]+\)\s*A: android:name\([^)]+\)="com\.example\.testapk\.SecondActivity"/
        aaptResult =~ /E: uses-permission \([^)]+\)\s*A: android:name\([^)]+\)="com\.example\.TEST_PERMISSION"/
    }

    def 'insert an asset'() {
        given:
        buildFile << '''
            }

            channels {
                defaultConfig {}
                create('my-channel') {
                    filename 'MyChannel.apk'
                    asset '1.txt', file('src/main/AndroidManifest.xml')
                }
            }
        '''

        when:
        runTasksSuccessfully('assembleMy-channel')

        then:
        fileExists('build/outputs/flea-market/MyChannel.apk')

        when:
        final zipFile = new ZipFile(file('build/outputs/flea-market/MyChannel.apk'))
        final entry = zipFile.getEntry('assets/1.txt')

        then:
        entry != null
        zipFile.getInputStream(entry).text == file('src/main/AndroidManifest.xml').text
    }

    def 'image and strings replacement'() {
        given:
        buildFile << '''
            }

            repositories {
                jcenter()
            }

            dependencies {
                compile 'co.infinum:NotificationLib:1.1.2@aar'
            }

            channels {
                defaultConfig {}
                create('altChannel') {
                    filename 'AltChannel.apk'
                    resources file('src/alt-channel/res')
                    values {
                        replaceStrings ~/[aeiou]/, '_'
                    }
                }
            }
        '''

        when:
        runTasksSuccessfully('assembleAltChannel')

        then:
        fileExists('build/outputs/flea-market/AltChannel.apk')

        when:
        final outputApk = file('build/outputs/flea-market/AltChannel.apk')
        final replacedRes = new ZipFile(outputApk).with {
            final entry = getEntry('res/drawable-hdpi-v4/ic_notification_android_labs_logo.png')
            getInputStream(entry).bytes
        }

        then:
        replacedRes == file('src/alt-channel/res/drawable-hdpi-v4/ic_notification_android_labs_logo.png').bytes

        when:
        final aaptResult = ['aapt', 'd', 'strings', outputApk].execute().text

        then:
        aaptResult =~ /ABC B_ld _nd It_l_c w_th _nd_rsc_r_/
        aaptResult =~ /T_st/
        aaptResult =~ /F__t/
    }

    def 'signingConfig'() {
        given:
        buildFile << '''
            }

            channels {
                defaultConfig {}
                create('123abc') {
                    signingConfig android.signingConfigs.release
                    manifest { c ->
                        analyticsChannel = c.name
                    }
                }
            }
        '''

        when:
        runTasksSuccessfully('assemble123abc')

        then:
        fileExists('build/outputs/flea-market/123abc.apk')

        when:
        final aaptResult = ['aapt', 'l', '-a', file('build/outputs/flea-market/123abc.apk')].execute().text

        then:
        aaptResult =~ /A: android:name\([^)]+\)="MY_CHANNEL" \([^)]+\)\s*A: android:value\([^)]+\)="123abc"/
    }

    def 'interaction with productFlavors'() {
        given:
        buildFile << '''
                productFlavors {
                    first
                    second
                }
            }

            channels {
                defaultConfig {
                    flavors 'first'
                }
                create 'alt'
            }
        '''

        when:
        runTasksSuccessfully('assembleAlt')

        then:
        fileExists('build/outputs/flea-market/alt.apk')
    }

    def 'interaction with provided signingConfig'() {
        given:
        buildFile << '''
                buildTypes {
                    release {
                        signingConfig signingConfigs.release
                    }
                }
            }

            channels {
                defaultConfig {}
                create 'alt'
            }
        '''

        when:
        runTasksSuccessfully('assembleAlt')

        then:
        fileExists('build/outputs/flea-market/alt.apk')
    }

    def 'should not be able to add a random resource'() {
        given:
        final newXmlFile = file('src/alt/res/xml/aaaa.xml')
        newXmlFile << '<?xml version="1.0" encoding="UTF-8"?><View/>'

        buildFile << '''
            }

            channels {
                defaultConfig {}
                create('alt') {
                    resources file('src/alt/res')
                }
            }
        '''

        expect:
        runTasksWithFailure('assembleAlt')
    }

    def 'benchmark multiple channels'() {
        given:
        buildFile << '''
            }

            channels {
                defaultConfig {
                    manifest { c ->
                        analyticsChannel = c.name
                    }
                }
                for (int i = 0; i < 100; ++ i) {
                    create "channel$i"
                }
            }
        '''

        // Populate the resources with a bunch of files to simulate real usage.
        final copySource = file("src/main/res/drawable/ic_launcher.png")
        for (int i = 0; i < 100; ++ i) {
            final copyTarget = file("src/main/res/drawable/resource_${i}.png")
            Files.copy(copySource.toPath(), copyTarget.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        expect:
        runTasksSuccessfully('assembleRelease')

        when:
        final String[] tasks = (0 ..< 100).collect { "assembleChannel$it" }
        final timingBegin = System.nanoTime()
        runTasksSuccessfully(*tasks)
        final timingEnd = System.nanoTime()

        then:
        println "Timing: ${(timingEnd - timingBegin) / (100e9)} s/APK"
        // We promise 2.0 s/APK for very simple APKs.
        (timingEnd - timingBegin) <= 100 * 2.0e9
    }

    def 'ignored files in resources will not cause exception'() {
        given:
        createFile 'src/alt/res/xml/.DS_Store'

        buildFile << '''
            }

            channels {
                defaultConfig {}
                create('alt') {
                    resources file('src/alt/res')
                }
            }
        '''

        expect:
        runTasksSuccessfully('assembleAlt')
    }

    def 'old 2-arg closure syntax'() {
        given:
        buildFile << '''
            }

            channels {
                defaultConfig {}
                create('alt') {
                    manifest { m, c ->
                        m.analyticsChannel = c.name
                    }
                }
            }
        '''

        expect:
        runTasksSuccessfully('assembleAlt')

        when:
        final aaptResult = ['aapt', 'l', '-a', file('build/outputs/flea-market/alt.apk')].execute().text

        then:
        aaptResult =~ /A: android:name\([^)]+\)="MY_CHANNEL" \([^)]+\)\s*A: android:value\([^)]+\)="alt"/
    }

    def 'renameTags'() {
        given:
        buildFile << '''
            }

            channels {
                defaultConfig {}
                create('alt') {
                    manifest {
                        renameTags ~/^com\\.example\\.(.+)/, 'test.$1'
                    }
                }
            }
        '''

        expect:
        runTasksSuccessfully('assembleAlt')

        when:
        final aaptResult = ['aapt', 'l', '-a', file('build/outputs/flea-market/alt.apk')].execute().text

        then:
        !(aaptResult =~ /A: android:name\([^)]+\)="com\.example\./)
        aaptResult =~ /A: android:name\([^)]+\)="test\./
    }
}
