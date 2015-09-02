package hihex.fleamarket

import hihex.fleamarket.utils.Files
import nebula.test.IntegrationSpec
import org.gradle.api.logging.LogLevel

import java.util.zip.ZipFile

class PluginTest extends IntegrationSpec {
    //private static final String APK_LOCATION = '../testapk/build/outputs/apk/testapk-release.apk'

    void linkOrCopy(String source, String target) {
        Files.linkOrCopy(new File(source), new File(projectDir, target))
    }

    @Override
    protected LogLevel getLogLevel() {
        LogLevel.QUIET
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

                //REPLACEMENT
            }
        '''

        linkOrCopy('../local.properties', 'local.properties')
        linkOrCopy('../testapk/src', 'src')
    }

    def 'applying FleaMarket plugin does not affect APK assembly'() {
        when:
        runTasksSuccessfully('assembleDebug')

        then:
        fileExists('build/outputs/apk/applying-FleaMarket-plugin-does-not-affect-APK-assembly-debug.apk')
    }

    def 'defining channels extension does not affect normal APK assembly'() {
        given:
        buildFile << '''
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
        final now = System.nanoTime()
        runTasksSuccessfully('assembleBChannel')
        final timeElasped = System.nanoTime() - now

        then:
        fileExists('build/outputs/flea-market/BChannel-v0.1.apk')
        timeElasped < 1.5e9

        // Since we performed no modifications, the APKs from the two channels shall be equivalent.
        file('build/outputs/flea-market/AChannel-v0.1.apk').bytes == file('build/outputs/flea-market/BChannel-v0.1.apk').bytes
    }

    def 'modify AndroidManifest.xml'() {
        given:
        buildFile << '''
            channels {
                defaultConfig {}
                create('my-channel') {
                    filename 'MyChannel.apk'
                    manifest { m, c ->
                        m.analyticsChannel = c.name
                        m.deleteTagsWithName 'activity', 'com.example.testapk.FirstActivity'
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
    }

    def 'insert an asset'() {
        given:
        buildFile << '''
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
                    values { rv, c ->
                        rv.replaceStrings ~/[aeiou]/, 'ǝ'
                    }
                }
            }
        '''

        when:
        runTasksSuccessfully('assembleAltChannel')

        then:
        fileExists('build/outputs/flea-market/AltChannel.apk')

        // output of `aapt d strings` is broken with Unicode :(

        // TODO Finish
    }
}
