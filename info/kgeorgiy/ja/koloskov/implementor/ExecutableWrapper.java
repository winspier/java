package info.kgeorgiy.ja.koloskov.implementor;

import java.lang.reflect.Executable;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * An abstract wrapper for {@link Executable} elements such as methods and constructors.
 *
 * <p>
 * This class provides functionality for generating code representations of executable elements,
 * including their modifiers, return types (for methods), parameter lists, and exception declarations.
 * </p>
 *
 * <p>
 * It serves as a base for specific wrappers like {@code MethodWrapper} and {@code ConstructorWrapper}
 * </p>
 *
 * @param <T> the type of {@link Executable} (either {@link java.lang.reflect.Method} or {@link java.lang.reflect.Constructor})
 */
public abstract class ExecutableWrapper<T extends Executable> {

    /**
     * Default return values for primitive types.
     *
     * <p>
     * This list is used when generating method bodies with default return statements.
     * </p>
     *
     * <p>
     * The rules are applied in order, meaning that {@code boolean} returns {@code false},
     * {@code void} returns an empty statement, and any other primitive type returns {@code 0}.
     * </p>
     */
    protected static final List<DefaultValue> DEFAULT_RETURN_VALUES = List.of(
            new DefaultValue(clazz -> clazz.equals(boolean.class), "false"),
            new DefaultValue(clazz -> clazz.equals(void.class), ""),
            new DefaultValue(Class::isPrimitive, "0")
    );

    /**
     * Modifier mask to exclude unwanted modifiers
     */
    protected static final int MODIFIERS_MASK = ~Modifier.NATIVE & ~Modifier.TRANSIENT & ~Modifier.ABSTRACT;

    /** The executable element wrapped by this instance. */
    protected final T executable;

    /**
     * Constructs a wrapper for the given executable element.
     *
     * @param executable the method or constructor to wrap
     */
    protected ExecutableWrapper(final T executable) {
        this.executable = executable;
    }


    /**
     * Returns the wrapped executable element.
     *
     * @return the wrapped executable
     */
    public T get() {
        return executable;
    }

    /**
     * Generates a string representation of the executable declaration, including modifiers, return type,
     * name, parameters, and exception declarations.
     *
     * @return a formatted string representing the executable declaration
     */
    public String generate() {
        return String.format(
                "\t%s %s %s %s %s {\n\t\t%s;\n\t}",
                getModifiers(),
                getReturnType(),
                getName(),
                getParameters(false),
                getExceptions(),
                getBody()
        );
    }

    /**
     * Retrieves the modifiers of the executable, applying the modifier mask to remove unnecessary modifiers.
     *
     * @return a string representation of the executable's modifiers
     */
    protected String getModifiers() {
        return Modifier.toString(executable.getModifiers() & MODIFIERS_MASK);
    }

    /**
     * Retrieves the return type of the executable.
     * <p>
     * Implementing subclasses must provide a valid return type.
     * </p>
     *
     * @return a string representation of the return type
     */
    protected abstract String getReturnType();


    /**
     * Retrieves the name of the executable.
     * <p>
     * Implementing subclasses must provide the correct name.
     * </p>
     *
     * @return the name of the method or constructor
     */
    protected abstract String getName();

    /**
     * Generates the method body, providing a default return value if necessary.
     *
     * @return a string representing the method body
     */
    protected abstract String getBody();

    /**
     * Generates a formatted parameter list for the executable.
     *
     * @param withoutNames if {@code true}, parameter names are omitted
     * @return a formatted string representing the parameters
     */
    protected String getParameters(final boolean withoutNames) {
        return Arrays.stream(executable.getParameters())
                .map(param -> getParameter(param, withoutNames))
                .collect(Collectors.joining(", ", "(", ")"));
    }

    /**
     * Retrieves the exception declarations of the executable.
     *
     * @return a formatted string listing the thrown exceptions, or an empty string if none are declared
     */
    protected String getExceptions() {
        var exceptions = executable.getExceptionTypes();
        return exceptions.length == 0 ? "" : " throws " + Arrays.stream(exceptions)
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(", "));
    }

    /**
     * Generates a formatted parameter representation.
     *
     * @param parameter     the parameter to format
     * @param withoutTypes  if {@code true}, parameter types are omitted
     * @return a formatted string representing the parameter
     */
    private String getParameter(final Parameter parameter, final boolean withoutTypes) {
        return (withoutTypes ? "" : parameter.getType().getCanonicalName() + " ") + parameter.getName();
    }

    /**
     * Determines whether this {@code ExecutableWrapper} is equal to another object.
     * <p>
     * Two wrappers are considered equal if they wrap executables with the same name and parameter types.
     * </p>
     *
     * @param obj the object to compare
     * @return {@code true} if the wrappers represent the same executable, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ExecutableWrapper<?> that)) return false;

        return executable.getName().equals(that.executable.getName()) &&
                List.of(executable.getParameterTypes()).equals(List.of(that.executable.getParameterTypes()));
    }


    /**
     * Computes the hash code for this {@code ExecutableWrapper}.
     *
     * @return the computed hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(executable.getName(), Arrays.hashCode(executable.getParameterTypes()));
    }

    /**
     * Represents a default return value for a specific type.
     *
     * @param predicate the condition determining whether this default value applies
     * @param value     the default value as a string
     */
    protected record DefaultValue(Predicate<Class<?>> predicate, String value) {}
}
