package info.kgeorgiy.ja.koloskov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Walker {

    private static final Logger logger = Logger.getLogger(Walker.class.getName());
    private final int maxDepth;

    public Walker() {
        this(Integer.MAX_VALUE);
    }

    public Walker(final int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public void walk(final String[] args) {
        if (!validateArgs(args)) {
            return;
        }

        final Path inputPath;
        final Path outputPath;

        if ((inputPath = toPath(args[0])) == null || (outputPath = toPath(args[1])) == null
                || !createDirectories(outputPath)) {
            return;
        }

        final String hashAlgorithm = args.length == 3 ? args[2].toUpperCase() : "SHA-256";
        final int hashLength = hashAlgorithm.equals("SHA-256") ? 8 : -1;

        run(inputPath, outputPath, hashAlgorithm, hashLength);
    }

    private static boolean validateArgs(final String[] args) {
        if (args == null || args.length == 0 || Stream.of(args).anyMatch(Objects::isNull)) {
            logger.log(Level.SEVERE, "Unexpected null or empty arguments passed");
            return false;
        }

        if (args.length < 2 || args.length > 3) {
            logger.log(Level.SEVERE, "Usage: <input file> <output file> [optional: <algorithm>]");
            return false;
        }
        return true;
    }

    private static Path toPath(final String from) {
        try {
            return Paths.get(from);
        } catch (final InvalidPathException e) {
            logger.log(Level.SEVERE, "Invalid path of file: {0}", from);
            return null;
        }
    }

    private static boolean createDirectories(final Path path) {
        final Path parentDir = path.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            try {
                Files.createDirectories(parentDir);
            } catch (final IOException e) {
                logger.log(Level.SEVERE, "Failed to create output directory: {0}", parentDir);
                return false;
            } catch (final SecurityException e) {
                logger.log(
                        Level.SEVERE,
                        "Access denied when creating output directory: {0}",
                        parentDir
                );
                return false;
            }
        }
        return true;
    }

    private void run(
            final Path inputPath,
            final Path outputPath,
            final String hashAlgorithm,
            final int hashLength
    ) {
        try (final BufferedReader reader = Files.newBufferedReader(
                inputPath,
                StandardCharsets.UTF_8
        );
                final BufferedWriter writer = Files.newBufferedWriter(
                        outputPath,
                        StandardCharsets.UTF_8
                )) {
            final FileHasher hasher = new MyFileHasher(hashAlgorithm, hashLength);
            final var visitor = new HashFileVisitor(
                    hasher,
                    (file, hash) -> writeRecord(
                            writer,
                            file.toString(),
                            hash
                    )
            );
            processWithLines(
                    reader,
                    line -> processFile(
                            line,
                            visitor,
                            () -> writeRecord(
                                    writer,
                                    line,
                                    hasher.getNullHash()
                            )
                    )
            );
        } catch (final IOException e) {
            logger.log(Level.SEVERE, "Error processing files: {0}", e.getMessage());
        } catch (final NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "No such hashing algorithm: {0}", e.getMessage());
        }
    }

    private static void writeRecord(
            final BufferedWriter writer,
            final String fileName,
            final String hash
    ) {
        try {
            writer.write(hash + " " + fileName);
            writer.newLine();
        } catch (final IOException e) {
            logger.log(Level.WARNING, "Error while writing output: {0}", e.getMessage());
        }
    }

    private static void processWithLines(
            final BufferedReader reader,
            final Consumer<String> consumer
    ) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                consumer.accept(line);
            }
        }
    }

    private void processFile(
            final String fileName,
            final FileVisitor<? super Path> visitor,
            final Runnable errorAction
    ) {
        try {
            final Path path = Paths.get(fileName);
            if (Files.exists(path)) {
                Files.walkFileTree(path, EnumSet.noneOf(FileVisitOption.class), maxDepth, visitor);
                return;
            } else {
                logger.log(Level.WARNING, "Not found: {0}:", fileName);
            }
        } catch (final IOException e) {
            logger.log(
                    Level.WARNING,
                    "IOException while processing file: " + fileName + ": " + e.getMessage()
            );
        } catch (final InvalidPathException e) {
            logger.log(Level.WARNING, "Invalid path: " + fileName + ": " + e.getMessage());
        } catch (final SecurityException e) {
            logger.log(
                    Level.WARNING,
                    "Access denied when walking " + fileName + ": " + e.getMessage()
            );
        }
        errorAction.run();
    }
}
