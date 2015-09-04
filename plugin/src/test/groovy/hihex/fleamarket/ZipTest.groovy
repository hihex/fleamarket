package hihex.fleamarket

import hihex.fleamarket.utils.Zip
import spock.lang.Specification

import java.nio.file.attribute.FileTime
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ZipTest extends Specification {
    private static final MIN_TIME = FileTime.fromMillis(315532800000L + 86400L)

    def 'can clear timestamp'() {
        given:
        final file = File.createTempFile("tmp", ".zip")
        file.withOutputStream {
            final zos = new ZipOutputStream(it)
            for (int i = 0; i < 10; ++ i) {
                final entry = new ZipEntry("b/" * i + "a.txt")
                zos.putNextEntry(entry)
                zos << ("Hello!" * i)
                zos.closeEntry()
            }
            zos.finish()
        }

        when: 'ensure the zip file has the timestamps filled in'
        final modifiedTimes = new ZipFile(file).entries().collect { it.lastModifiedTime }

        then:
        assert modifiedTimes.min() > MIN_TIME

        when: 'now clear the timestamps'
        Zip.clearTimestamps(file)
        final newModifiedTimes = new ZipFile(file).entries().collect { it.lastModifiedTime }

        then:
        assert modifiedTimes.size() == newModifiedTimes.size()
        assert newModifiedTimes.min() <= MIN_TIME
    }
}
