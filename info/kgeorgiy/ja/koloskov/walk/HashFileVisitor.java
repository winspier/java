package info.kgeorgiy.ja.koloskov.walk;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HashFileVisitor extends SimpleFileVisitor<Path> {

    private static final Logger logger = Logger.getLogger(HashFileVisitor.class.getName());
    private final FileHasher hasher;
    private final BiConsumer<Path, String> action;

    public HashFileVisitor(final FileHasher hasher, final BiConsumer<Path, String> action) {
        this.hasher = hasher;
        this.action = action;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
        try {
            action.accept(file, hasher.getHash(file));
        } catch (final IOException e) {
            logger.log(Level.WARNING, "Failed to hash file: " + file + ": " + e.getMessage());
            action.accept(file, hasher.getNullHash());
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException exc) {
        logger.log(Level.WARNING, "Failed to access file: " + file + ": " + exc.getMessage());
        action.accept(file, hasher.getNullHash());
        return FileVisitResult.CONTINUE;
    }
}