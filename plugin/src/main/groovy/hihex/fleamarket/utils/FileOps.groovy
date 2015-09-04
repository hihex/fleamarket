package hihex.fleamarket.utils

import groovy.io.FileType

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

class FileOps {
    private FileOps() {}

    // Ref: http://stackoverflow.com/a/10068306/224671 to copy files using NIO.
    private static final class CopyFileVisitor extends SimpleFileVisitor<Path> {
        private final Path targetPath;
        private Path sourcePath = null;

        CopyFileVisitor(final Path targetPath) {
            this.targetPath = targetPath;
        }

        @Override
        FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
            if (!sourcePath) {
                sourcePath = dir
            } else {
                Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)))
            }
            FileVisitResult.CONTINUE
        }

        @Override
        FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
            Files.copy(file, targetPath.resolve(sourcePath.relativize(file)))
            FileVisitResult.CONTINUE
        }
    }

    static void clearDirectory(final File directory) {
        directory.eachFile(FileType.ANY) {
            deleteRecursively(it)
        }
    }

    static boolean deleteRecursively(final File file) {
        if (file.isDirectory()) {
            file.deleteDir()
        } else {
            file.delete()
        }
    }

    static void copyRecursively(final File src, final File target) {
        assert !target.exists()
        if (src.isDirectory()) {
            Files.walkFileTree(src.toPath(), new CopyFileVisitor(target.toPath()))
        } else {
            Files.copy(src.toPath(), target.toPath())
        }
    }

    static void copyRecursivelyToDirectory(final File src, final File targetDir) {
        assert targetDir.isDirectory()
        copyRecursively(src, new File(targetDir, src.name))
    }
}
