package hihex.fleamarket
import hihex.fleamarket.utils.Zip
import spock.lang.Specification

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ZipTest extends Specification {
    private static final long MIN_TIME = (365*10+3)*86400_000L

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
        final modifiedTimes = new ZipFile(file).entries().collect { it.time }

        then:
        assert modifiedTimes.min() > MIN_TIME

        when: 'now clear the timestamps'
        Zip.clearTimestamps(file)
        final newModifiedTimes = new ZipFile(file).entries().collect { it.time }

        then:
        assert modifiedTimes.size() == newModifiedTimes.size()
        assert newModifiedTimes.min() <= MIN_TIME
    }
}
