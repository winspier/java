package info.kgeorgiy.ja.koloskov.implementor;

import info.kgeorgiy.java.advanced.implementor.*;

import info.kgeorgiy.java.advanced.implementor.tools.JarImpler;
import java.io.UncheckedIOException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * A class that generates implementations of Java interfaces and abstract classes.
 * <p>
 * This class implements {@link JarImpler} and {@link Impler}. Class can generate a Java source
 * file with an implementation of a given abstract class or interface. Additionally, it supports compiling
 * the generated code and packaging it into a JAR file.
 * </p>
 */
public class Implementor implements JarImpler {


    /**
     * Default constructor for {@code Implementor}.
     */
    public Implementor() {
    }

    /**
     * Main entry point of the application.
     * <p>
     * Depending on the command-line arguments, this method generates either a Java implementation file
     * or a JAR file with the compiled implementation. If the {@code -jar} flag is provided, a JAR file
     * will be created; otherwise, a regular implementation file will be generated.
     * </p>
     * <p>
     * <b>Note:</b> The order of the arguments matters. The {@code -jar} flag, if used, must be the first argument.
     * Generated class' name will be the same as the class name of the type token with the {@code Impl} suffix added.
     * </p>
     *
     * @param args command-line arguments:
     *             <ul>
     *                 <li>{@code -jar}: Optional flag to create a JAR file.</li>
     *                 <li>{@code <class>}: Type token of the class or interface type to implement.</li>
     *                 <li>{@code <path>}: The root directory for the generated file or JAR.</li>
     *             </ul>
     */
    public static void main(final String[] args) {
        if (!validateArgs(args)) {
            return;
        }

        final JarImpler implementor = new Implementor();
        try {
            if (args.length == 3) {
                implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            } else {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            }
        } catch (final ImplerException e) {
            System.err.println("Implementation failed: " + e.getMessage());
        } catch (final InvalidPathException e) {
            System.err.println("Invalid path: " + e.getMessage());
        } catch (final ClassNotFoundException e) {
            System.err.println("Invalid class name: " + e.getMessage());
        }
    }

    /**
     * Validates the command-line arguments for the program.
     * <p>
     * If args contains {@code null} or have less or more arguments, it will be written to stderr and return {@code boolean} flag
     * </p>
     *
     * @param args the arguments to validate
     * @return {@code true} if the arguments are valid, otherwise {@code false}
     */
    private static boolean validateArgs(final String[] args) {
        if (args == null || args.length == 0 || Stream.of(args).anyMatch(Objects::isNull)) {
            System.err.println("Unexpected null or empty arguments passed");
            return false;
        }

        if (args.length < 2 || args.length > 3 || (args.length == 3 && !args[0].equals("-jar"))) {
            System.err.println("Usage: [optional: -jar] <type token> <path>");
            return false;
        }
        return true;
    }

    /**
     * Adds a compiled class file to the specified JAR archive.
     *
     * <p>
     * This helper function is used to add a single class file to an existing JAR archive.
     * The class file is added relative to the {@code rootDirectory} and its path in the JAR
     * will reflect the directory structure relative to this root.
     * </p>
     *
     * @param classFile       the path to the compiled class file to add to the JAR
     * @param rootDirectory   the root directory containing the compiled class files
     * @param jarOutputStream the output stream of the JAR file to which the class file will be added
     * @throws UncheckedIOException if an {@code IOException} occurs during the file copy or while interacting
     *         with the {@code jarOutputStream}, the exception is wrapped into an unchecked exception.
     */
    private static void addClassToJar(
            final Path classFile,
            final Path rootDirectory,
            final JarOutputStream jarOutputStream
    ) {
        try {
            final String entryName = rootDirectory.relativize(classFile)
                    .toString()
                    .replace(File.separatorChar, '/');
            jarOutputStream.putNextEntry(new ZipEntry(entryName));
            Files.copy(classFile, jarOutputStream);
            jarOutputStream.closeEntry();
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot add class to jar: " + classFile, e);
        }
    }

    /**
     * Generates a default manifest for the JAR file.
     * <p>
     * This method creates a new {@link Manifest} object and sets the {@code MANIFEST_VERSION}
     * attribute to {@code "1.0"}. The generated manifest can be used when creating a JAR file.
     * </p>
     *
     * @return a default {@link Manifest}
     */
    private static Manifest getManifest() {
        final Manifest manifest = new Manifest();
        final Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        return manifest;
    }

    /**
     * Compiles the generated implementation source file into a class file.
     * <p>
     * This method uses the system's Java compiler to compile the generated source file
     * that implements the specified class or interface. It adds the directory containing
     * the generated source file to the classpath.
     * </p>
     *
     * @param clazz the class or interface being implemented
     * @param path  the directory containing the generated source file
     * @throws ImplerException if the compilation fails, or if the Java compiler is not available
     */
    private static void compile(final Class<?> clazz, final Path path) throws ImplerException {
        final String classpath = System.getProperty("java.class.path") + File.pathSeparator + path;

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Java compiler is not available");
        }
        final String[] args = new String[]{
                "-cp", classpath, "-encoding", "UTF-8",
                getImplFilePath(clazz, path).toString()};
        final int exitCode = compiler.run(null, null, null, args);
        if (exitCode != 0) {
            throw new ImplerException("Compilation failed");
        }
    }

    /**
     * Creates the necessary directories for the given path if they do not already exist.
     * <p>
     * This method ensures that all parent directories of the specified path are created. If the
     * directories already exist, the method does nothing. If any error occurs during directory
     * creation, an exception is thrown.
     * </p>
     *
     * @param path the path for which directories should be created
     * @throws ImplerException if an error occurs while creating directories
     */
    private static void createDirectories(final Path path) throws ImplerException {
        if (path.getParent() == null) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
        } catch (final IOException e) {
            throw new ImplerException("Сan not create directories", e);
        }
    }

    /**
     * Creates a temporary directory inside the specified base path.
     * <p>
     * The temporary directory will be created in the parent directory of the given {@code path}
     * and will have a prefix of {@code "temp"}.
     * </p>
     *
     * @param path the path where the temporary directory should be created
     * @return the path to the created temporary directory
     * @throws ImplerException if the directory cannot be created
     */
    private static Path createTempDirectory(final Path path) throws ImplerException {
        try {
            return Files.createTempDirectory(path.toAbsolutePath().getParent(), "temp");
        } catch (final IOException e) {
            throw new ImplerException("Failed to create temporary directory", e);
        }
    }

    /**
     * Generates the file path for the implementation of the given class.
     * <p>
     * The implementation java file will be placed in the corresponding package directory within the
     * specified root directory, with the class name suffixed by {@code "Impl"}.
     * </p>
     *
     * @param clazz the class to implement
     * @param path  the root directory where the implementation file should be placed
     * @return the path to the generated implementation file
     */
    private static Path getImplFilePath(final Class<?> clazz, final Path path) {
        return path.resolve(getPackagePath(clazz)).resolve(clazz.getSimpleName() + "Impl.java");
    }

    /**
     * Returns the package path for the given class.
     * <p>
     * The package name of the class is converted into a corresponding directory path, where
     * each package segment is separated by the file system's separator.
     * </p>
     *
     * @param clazz the class whose package path should be determined
     * @return the package path as a {@link Path}, corresponding to the directory structure
     */
    private static Path getPackagePath(final Class<?> clazz) {
        return Path.of(clazz.getPackageName().replace('.', File.separatorChar));
    }

    /**
     * Recursively deletes files and directories at the specified path.
     * <p>
     * This method walks through the given path, including all subdirectories and files,
     * and deletes them in reverse order to avoid issues when deleting directories.
     * Any errors encountered during the deletion are logged to {@code System.err}.
     * </p>
     *
     * @param path the path to delete, including subdirectories and files
     */
    private static void clearPath(final Path path) {
        if (!Files.exists(path)) {
            return;
        }

        try (final Stream<Path> paths = Files.walk(path).sorted(Comparator.reverseOrder())) {
            for (final Path p : paths.toList()) {
                try {
                    Files.deleteIfExists(p);
                } catch (final IOException e) {
                    System.err.printf("Failed to delete: %s (%s)%n", p, e.getMessage());
                }
            }
        } catch (final IOException e) {
            System.err.printf("Failed to clean directory: %s (%s)%n", path, e.getMessage());
        }
    }

    /**
     * Generates a Java source file implementing the specified class or interface.
     * <p>
     * This method generates the implementation for the given class or interface and writes it to the specified
     * directory. The implementation will be placed in the corresponding file with the appropriate suffix, such as
     * {@code ClassNameImpl.java}.
     * </p>
     *
     * <p>
     * <b>NOTE:</b> throws as cause of {@code ImplerException} IOException if an I/O error occurs during file creation or writing
     * </p>
     *
     * @param clazz type token of the class or interface to implement
     * @param path  the root directory where the implementation file should be placed
     * @throws ImplerException if an error occurs while generating the implementation or writing to the file
     */
    @Override
    public void implement(final Class<?> clazz, Path path) throws ImplerException {
        final ClassImplementor classImplementor = new ClassImplementor(clazz);
        path = getImplFilePath(clazz, path);
        createDirectories(path);
        try (final BufferedWriter bufferedWriter = Files.newBufferedWriter(path)) {
            classImplementor.writeImplementation(bufferedWriter);
        } catch (final IOException e) {
            throw new ImplerException("Error while writing implementation to file", e);
        }
    }

    /**
     * Generates a JAR file containing a compiled implementation of the given class or interface.
     * <p>
     * This method generates the Java source code for the specified class or interface, compiles it, and
     * packages the compiled class into a JAR file at the specified location.
     * </p>
     * <p>
     * <b>NOTE:</b> throws as cause of {@code ImplerException} IOException if an I/O error occurs during file creation or copying
     * </p>
     * @param clazz   type token of the class or interface to implement
     * @param jarPath the path where the JAR file should be created
     * @throws ImplerException if an error occurs during implementation generation, compilation, or packaging
     */
    @Override
    public void implementJar(final Class<?> clazz, final Path jarPath) throws ImplerException {
        createDirectories(jarPath);
        final Path tempDir = createTempDirectory(jarPath);

        try {
            implement(clazz, tempDir);
            compile(clazz, tempDir);
            packageIntoJar(clazz, jarPath, tempDir);
        } finally {
            clearPath(tempDir);
        }
    }

    /**
     * Creates a JAR file containing the compiled implementation of a class.
     * <p>
     * This method packages the compiled class files of the specified class into a JAR file at the provided output path.
     * It adds all class files from the temporary directory to the JAR file, ensuring the correct package structure.
     * </p>
     * <p>
     * <b>NOTE:</b> throws as cause of {@code ImplerException} IOException if an I/O error occurs during file operations
     * </p>
     * @param clazz         the implemented class
     * @param jarPath       the output path for the JAR file
     * @param tempDirectory the directory containing compiled class files
     * @throws ImplerException if an error occurs while writing the JAR file or adding class files to it
     */
    private void packageIntoJar(final Class<?> clazz, final Path jarPath, final Path tempDirectory)
            throws ImplerException {
        try (final JarOutputStream jarOutputStream = new JarOutputStream(
                Files.newOutputStream(jarPath), getManifest())) {
            final Path classRoot = tempDirectory.resolve(getPackagePath(clazz));

            try (final Stream<Path> classFiles = Files.walk(classRoot)) {
                classFiles
                        .filter(p -> p.toString().endsWith(".class"))
                        .forEach(classFile -> addClassToJar(
                                classFile,
                                tempDirectory,
                                jarOutputStream
                        ));
            }
        } catch (final IOException e) {
            throw new ImplerException("Cannot write to jar", e);
        }
    }

}