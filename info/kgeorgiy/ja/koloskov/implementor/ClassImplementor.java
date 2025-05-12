package info.kgeorgiy.ja.koloskov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The {@code ClassImplementor} class generates an implementation for a given class or interface.
 * <p>
 * This class creates a concrete implementation of the provided class or interface. In both cases (class or interface),
 * all abstract methods, including those from parent classes, are implemented.
 * </p>
 * <p>
 * When the target is a class, in addition to implementing all abstract methods, the generated implementation
 * will also attempt to provide constructors to properly instantiate the class, based on its available constructors.
 * </p>
 */
public class ClassImplementor {

    /**
     * A rule for validating whether a class can be implemented.
     *
     * @param predicate the condition that determines if a class is invalid
     * @param message   the error message thrown if the rule is violated
     */
    private record Rule(Predicate<Class<?>> predicate, String message) {
        /**
         * Checks whether the given class violates the rule.
         * <p>
         * If the class matches the predicate, an {@link ImplerException} is thrown with the specified message.
         * </p>
         *
         * @param clazz the class to check against the rule
         * @throws ImplerException if the class violates the rule
         */
        void check(Class<?> clazz) throws ImplerException {
            if (predicate.test(clazz)) {
                throw new ImplerException(message);
            }
        }
    }

    /**
     * List of rules used to validate whether a class can be implemented.
     *
     * <p>
     * Contains a list of {@code Predicate<Class<?>>} rules along with an associated error message
     * to be thrown as an exception if the rule is violated. These rules are used for validating
     * the {@code targetClass} in the constructor, and exceptions are thrown if any of the rules fail.
     * </p>
     */
    private static final List<Rule> CLASS_VALIDATION_RULES = List.of(
            new Rule(Class::isArray, "Cannot implement array"),
            new Rule(Class::isPrimitive, "Cannot implement primitive type"),
            new Rule(Class::isRecord, "Cannot implement record type"),
            new Rule(clazz -> Modifier.isFinal(clazz.getModifiers()), "Cannot implement final class"),
            new Rule(clazz -> Modifier.isPrivate(clazz.getModifiers()), "Cannot implement private class"),
            new Rule(Class::isSealed, "Cannot implement sealed class"),
            new Rule(clazz -> clazz == Enum.class, "Cannot implement enum class"),
            new Rule(clazz -> clazz.getPackageName().startsWith("java."), "Cannot implement classes from the `java` package")
    );


    /**
     * The target class to be implemented.
     */
    private final Class<?> targetClass;

    /**
     * A set of abstract methods that need to be implemented.
     */
    private final Set<MethodWrapper> methods = new HashSet<>();

    /**
     * A list of available constructors for the implementation.
     */
    private final List<ConstructorWrapper> constructors = new ArrayList<>();

    /**
     * The name of the generated implementation class.
     */
    private final String name;

    /**
     * Constructs a {@code ClassImplementor} for the specified class, generating an implementation
     * with the default name (class name + "Impl").
     *
     * @param clazz the target class to be implemented
     * @throws ImplerException if the class cannot be implemented due to validation or other issues
     */
    public ClassImplementor(final Class<?> clazz) throws ImplerException {
        this(clazz, clazz.getSimpleName() + "Impl");
    }


    /**
     * Constructs a {@code ClassImplementor} for the specified class, generating an implementation
     * with a custom name for the implementation class.
     *
     * <p>
     * The constructor performs the validation of the target class and collects abstract methods
     * and constructors (if the class is not an interface). If the class cannot be implemented due
     * to the rules, an {@code ImplerException} is thrown.
     * </p>
     *
     * @param clazz the target class to be implemented
     * @param name  the name of the generated implementation class
     * @throws ImplerException if the class cannot be implemented, such as when no constructors
     *                         are available or the class fails validation
     */
    public ClassImplementor(final Class<?> clazz, final String name) throws ImplerException {
        this.targetClass = clazz;
        this.name = name;

        validateClass(clazz);

        collectAbstractMethods(clazz);
        validateMethods();

        if (!targetClass.isInterface()) {
            collectAvailableConstructors(clazz);
            if (constructors.isEmpty()) {
                throw new ImplerException("No available constructors found");
            }
        }
    }

    /**
     * Validates whether the target class can be implemented rules in {@code CLASS_VALIDATION_RULES}.
     *
     * @param clazz the target class to be checked
     * @throws ImplerException if the class violates implementation constraints
     */
    private void validateClass(final Class<?> clazz) throws ImplerException {
        for (var rule : CLASS_VALIDATION_RULES) {
            rule.check(clazz);
        }
    }

    /**
     * Validates whether the collected methods are implementable.
     *
     * @throws ImplerException if a method has a private return type or private parameter
     */
    private void validateMethods() throws ImplerException {
        for (var methodWrapper : methods) {
            Method method = methodWrapper.get();
            if (Modifier.isPrivate(method.getReturnType().getModifiers())) {
                throw new ImplerException("Cannot implement method " + method.getName() + ": private return type");
            }
            if (hasPrivateParameter(method.getParameters())) {
                throw new ImplerException("Cannot implement method " + method.getName() + ": private parameter type");
            }
        }
    }

    /**
     * Collects all abstract methods from the given class and its superclasses.
     *
     * <p>
     * This method scans public and declared methods to collect all abstract methods
     * that require implementation. Final methods are tracked separately and excluded
     * from the final set of methods to be implemented.
     * </p>
     *
     * <p>
     * The {@link MethodWrapper} class is used to correctly compare methods, ensuring that
     * methods with the same signature are considered identical.
     * Two methods are treated as the same if they have
     * the same name, parameter types, and return type compatibility.
     * </p>
     *
     * @param clazz the target class whose abstract methods should be implemented
     */
    private void collectAbstractMethods(Class<?> clazz) {
        Set<MethodWrapper> finalMethods = new HashSet<>();

        methods.addAll(Arrays.stream(clazz.getMethods())
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .map(MethodWrapper::new)
                .collect(Collectors.toSet()));

        while (clazz != null) {
            for (Method method : clazz.getDeclaredMethods()) {
                MethodWrapper wrapper = new MethodWrapper(method);

                if (Modifier.isFinal(method.getModifiers())) {
                    finalMethods.add(wrapper);
                } else if (Modifier.isAbstract(method.getModifiers())) {
                    methods.add(wrapper);
                }
            }
            clazz = clazz.getSuperclass();
        }

        methods.removeAll(finalMethods);
    }

    /**
     * Checks whether a method has private parameters.
     *
     * @param params the method parameters
     * @return {@code true} if the method has at least one private parameter; otherwise, {@code false}
     */
    private boolean hasPrivateParameter(Parameter[] params) {
        return Arrays.stream(params).anyMatch(p -> Modifier.isPrivate(p.getType().getModifiers()));
    }

    /**
     * Collects all available constructors that can be used in the implementation.
     *
     * @param clazz the target class
     */
    private void collectAvailableConstructors(final Class<?> clazz) {
        constructors.addAll(Arrays.stream(clazz.getDeclaredConstructors())
                .filter(c -> !Modifier.isPrivate(c.getModifiers()) && !hasPrivateParameter(c.getParameters()))
                .map(ConstructorWrapper::new)
                .toList());
    }

    /**
     * Generates the class declaration header.
     *
     * @return a string representing the class declaration
     */
    private String generateClassHeader() {
        return String.format(
                "public class %sImpl %s %s",
                targetClass.getSimpleName(),
                targetClass.isInterface() ? "implements" : "extends",
                targetClass.getCanonicalName()
        );
    }

    /**
     * Converts a string to its Unicode representation.
     *
     * @param s the input string
     * @return the Unicode-escaped version of the string
     */
    private static String convertToUnicodeEscapes(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            b.append(c >= 128 ? String.format("\\u%04X", (int) c) : c);
        }
        return b.toString();
    }

    /**
     * Writes a line to the provided {@code BufferedWriter}.
     *
     * @param writer  the writer to use
     * @param content the line to write
     * @throws IOException if an I/O error occurs
     */
    private void writeLine(final BufferedWriter writer, final String content) throws IOException {
        writer.write(convertToUnicodeEscapes(content));
        writer.newLine();
    }

    /**
     * Writes the generated implementation to the provided {@code BufferedWriter}.
     *
     * @param writer the writer to use
     * @throws IOException if an I/O error occurs
     */
    public void writeImplementation(final BufferedWriter writer) throws IOException {
        writeLine(writer, generatePackageLine());
        writeLine(writer, generateClassHeader());
        writeLine(writer, "{");

        for (var constructor : constructors) {
            writeLine(writer, constructor.generate());
        }
        for (var method : methods) {
            writeLine(writer, method.generate());
        }
        writeLine(writer, "}");
    }

    /**
     * Generates the package declaration line.
     *
     * @return a string representing the package declaration
     */
    private String generatePackageLine() {
        return "package " + targetClass.getPackageName() + ";";
    }

    /**
     * A wrapper for a {@link Constructor} that provides functionality for code generation.
     * <p>
     * This class extends {@link ExecutableWrapper} and is used to generate implementations
     * of constructors for the given class.
     * </p>
     */
    private class ConstructorWrapper extends ExecutableWrapper<Constructor<?>> {

        /**
         * Constructs a wrapper for the given constructor.
         *
         * @param constructor the constructor to wrap
         */
        public ConstructorWrapper(final Constructor<?> constructor) {
            super(constructor);
        }

        /**
         * Returns an empty string as constructors do not have return types.
         *
         * @return an empty string
         */
        @Override
        protected String getReturnType() {
            return "";
        }

        /**
         * Returns the name of the generated class implementation.
         *
         * @return the name of the implementing class
         */
        @Override
        protected String getName() {
            return ClassImplementor.this.name;
        }

        /**
         * Generates the constructor body, calling the superclass constructor with parameters.
         *
         * @return a string representing the constructor body
         */
        @Override
        protected String getBody() {
            return "super" + getParameters(true);
        }

    }

    /**
     * A wrapper for a {@link Method} that provides functionality for code generation.
     * <p>
     * This class extends {@link ExecutableWrapper} and is used to generate method implementations
     * by providing a return type, name, and a default method body.
     * </p>
     */
    private static class MethodWrapper extends ExecutableWrapper<Method> {

        /**
         * Constructs a wrapper for the given method.
         *
         * @param method the method to wrap
         */
        public MethodWrapper(final Method method) {
            super(method);
        }

        /**
         * Returns the return type of the method.
         *
         * @return a string representation of the method's return type
         */
        @Override
        protected String getReturnType() {
            return executable.getReturnType().getCanonicalName();
        }

        /**
         * Returns the name of the method.
         *
         * @return the method name
         */
        @Override
        protected String getName() {
            return executable.getName();
        }


        /**
         * Generates the method body with a default return statement.
         * <p>
         * If the method has a return type, an appropriate default value is returned.
         * If the return type is {@code void}, an empty statement is used.
         * </p>
         *
         * @return a string representing the method body
         */
        @Override
        protected String getBody() {
            return "return " + DEFAULT_RETURN_VALUES.stream()
                    .filter(defaultValue -> defaultValue.predicate().test(executable.getReturnType()))
                    .map(DefaultValue::value)
                    .findFirst()
                    .orElse("null");
        }

        /**
         * Determines whether this {@code MethodWrapper} is equal to another object.
         * <p>
         * Two method wrappers are considered equal if they wrap methods with the same name,
         * parameter types, and return type compatibility.
         * </p>
         *
         * @param obj the object to compare
         * @return {@code true} if the method wrappers represent equivalent methods, {@code false} otherwise
         */
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MethodWrapper that)) return false;

            return super.equals(that) &&
                    (that.executable.getReturnType().isAssignableFrom(executable.getReturnType()) ||
                            executable.getReturnType().isAssignableFrom(that.executable.getReturnType()));
        }
    }
}
