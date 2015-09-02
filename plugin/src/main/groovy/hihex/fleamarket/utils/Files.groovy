package hihex.fleamarket.utils

import java.nio.file.Files as NioFiles

class Files {
    private Files() {}

    static void linkOrCopy(final File source, final File target) {
        final sourcePath = source.absoluteFile.toPath()
        final targetPath = target.toPath()
        try {
            NioFiles.createSymbolicLink(targetPath, sourcePath)
        } catch (UnsupportedOperationException ignored) {
            NioFiles.copy(sourcePath, targetPath)
        }
    }

    static void copy(final File source, final File target) {
        NioFiles.copy(source.toPath(), target.toPath())
    }
}
