package hihex.fleamarket.utils

import groovy.transform.CompileStatic

@CompileStatic
class Zip {
    private static final int EARLIEST_DATE_TIME = 0x00002100
    // 1980 Jan 1st, 00:00:00

    private Zip() {}

    /**
     * Remove the timestamp info of all entries in the ZIP file.
     *
     * @param zipFile Path to the ZIP file.
     */
    static void clearTimestamps(final File zipFile) {
        final stream = new RandomAccessFile(zipFile, "rw")
        stream.withCloseable {
            stream.with {
                final eocdOffset = length() - 22
                seek(eocdOffset)
                assert readInt() == 0x504b0506

                seek(eocdOffset + 16)
                def centralDirectoryOffset = Integer.reverseBytes(readInt())

                while (true) {
                    seek(centralDirectoryOffset)
                    final signature = readInt()
                    if (signature != 0x504b0102) {
                        break
                    }

                    seek(centralDirectoryOffset + 12)
                    writeInt(EARLIEST_DATE_TIME)

                    seek(centralDirectoryOffset + 28)
                    final n = Short.reverseBytes(readShort()) & 0xffff
                    final m = Short.reverseBytes(readShort()) & 0xffff
                    final k = Short.reverseBytes(readShort()) & 0xffff

                    seek(centralDirectoryOffset + 42)
                    final localFileHeader = Integer.reverseBytes(readInt())

                    seek(localFileHeader)
                    assert readInt() == 0x504b0304

                    seek(localFileHeader + 10)
                    writeInt(EARLIEST_DATE_TIME)

                    centralDirectoryOffset += 46 + m + n + k
                }
            }
        }
    }
}
