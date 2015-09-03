package hihex.fleamarket.utils

import org.apache.commons.io.FileUtils

import java.nio.file.Files as NioFiles

class Files {
    private Files() {}

    static void linkOrCopy(final File source, final File target) {
        final sourcePath = source.absoluteFile.toPath()
        final targetPath = target.toPath()
        try {
            NioFiles.createSymbolicLink(targetPath, sourcePath)
        } catch (UnsupportedOperationException ignored) {
            copy(source, target)
        }
    }

    static void copy(final File source, final File target) {
        if (source.isDirectory()) {
            FileUtils.copyDirectory(source, target)
        } else {
            NioFiles.copy(source.toPath(), target.toPath())
        }
    }
}
