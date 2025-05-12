package info.kgeorgiy.ja.koloskov.student;

import info.kgeorgiy.java.advanced.student.AdvancedQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements AdvancedQuery {

    private static final Comparator<Student> NAME_COMPARATOR = Comparator.comparing(Student::firstName)
            .thenComparing(Student::lastName)
            .thenComparing(Student::id);

    private static final Function<Student, String> STUDENT_FULL_NAME =
            s -> s.firstName() + " " + s.lastName();

    @Override
    public List<Group> getGroupsByName(final Collection<Student> collection) {
        return getGroupsBy(collection, NAME_COMPARATOR);
    }

    private static List<Group> getGroupsBy(
            final Collection<Student> collection,
            final Comparator<? super Student> comparator
    ) {
        return collection.stream()
                .collect(Collectors.collectingAndThen(Collectors.groupingBy(
                        Student::groupName,
                        TreeMap::new,
                        Collectors.toCollection(() -> new TreeSet<>(comparator))
                ), TreeMap::entrySet))
                .stream()
                .map(e -> new Group(e.getKey(), e.getValue().stream().toList()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Group> getGroupsById(final Collection<Student> collection) {
        return getGroupsBy(collection, Comparator.comparing(Student::id));
    }

    @Override
    public GroupName getLargestGroup(final Collection<Student> collection) {
        return minKeyBy(
                collection.stream(),
                Student::groupName,
                Function.identity(),
                getComparatorBySizeAndKey(Comparator.comparing(GroupName::name)).reversed(),
                null
        );
    }

    private static <K, V> K minKeyBy(
            final Stream<Student> stream,
            final Function<? super Student, K> keyExtractor,
            final Function<? super Student, V> valueExtractor,
            final Comparator<? super Entry<K, Set<V>>> comparator,
            final K defaultValue
    ) {
        return stream.collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(
                        keyExtractor,
                        Collectors.mapping(valueExtractor, Collectors.toSet())
                        ), Map::entrySet
                ))
                .stream()
                .min(comparator)
                .map(Entry::getKey)
                .orElse(defaultValue);
    }

    private static <K, V> Comparator<Entry<K, Set<V>>> getComparatorBySizeAndKey(final Comparator<? super K> keyComparator) {
        return Comparator.<Entry<K, Set<V>>>comparingInt(p -> p.getValue().size())
                .thenComparing(Entry::getKey, keyComparator);
    }

    @Override
    public GroupName getLargestGroupFirstName(final Collection<Student> collection) {
        return minKeyBy(
                collection.stream(),
                Student::groupName,
                Student::firstName,
                getComparatorBySizeAndKey(Comparator.comparing(GroupName::name)
                        .reversed()).reversed(),
                null
        );
    }

    @Override
    public String getMostPopularName(final Collection<Group> collection) {
        return minKeyBy(
                getStudentStream(collection),
                Student::firstName,
                Student::groupName,
                getComparatorBySizeAndKey(Comparator.<String>naturalOrder().reversed()).reversed(),
                ""
        );
    }

    private static Stream<Student> getStudentStream(final Collection<Group> collection) {
        return collection.stream().flatMap(s -> s.students().stream());
    }

    @Override
    public String getLeastPopularName(final Collection<Group> collection) {
        return minKeyBy(
                getStudentStream(collection),
                Student::firstName,
                Student::groupName,
                getComparatorBySizeAndKey(Comparator.naturalOrder()),
                ""
        );
    }

    @Override
    public List<String> getFirstNames(final Collection<Group> collection, final int[] indices) {
        return flatMapByIndices(collection, indices, Student::firstName);
    }

    private <R> List<R> flatMapByIndices(
            final Collection<Group> collection,
            final int[] indices,
            final Function<Student, R> mapper
    ) {
        return collection.stream().flatMap(g -> getByIndices(g, indices)).map(mapper).toList();
    }

    @Override
    public List<String> getFirstNames(final List<Student> list) {
        return extractFromStudents(list, Student::firstName, Collectors.toList());
    }

    @Override
    public List<String> getLastNames(final List<Student> list) {
        return extractFromStudents(list, Student::lastName, Collectors.toList());
    }

    @Override
    public List<GroupName> getGroupNames(final List<Student> list) {
        return extractFromStudents(list, Student::groupName, Collectors.toList());
    }

    @Override
    public List<String> getFullNames(final List<Student> list) {
        return extractFromStudents(list, STUDENT_FULL_NAME, Collectors.toList());
    }

    @Override
    public Set<String> getDistinctFirstNames(final List<Student> list) {
        return extractFromStudents(list, Student::firstName, Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(final List<Student> list) {
        return list.stream().max(Comparator.naturalOrder()).map(Student::firstName).orElse("");
    }

    @Override
    public List<Student> sortStudentsById(final Collection<Student> collection) {
        return sortStudentsBy(collection, Comparator.comparing(Student::id));
    }

    private static List<Student> sortStudentsBy(
            final Collection<Student> collection,
            final Comparator<? super Student> comparator
    ) {
        return collection.stream().sorted(comparator).toList();
    }

    @Override
    public List<Student> sortStudentsByName(final Collection<Student> collection) {
        return sortStudentsBy(collection, NAME_COMPARATOR);
    }

    @Override
    public List<Student> findStudentsByFirstName(
            final Collection<Student> collection,
            final String firstName
    ) {
        return findStudentsBy(collection, Student::firstName, firstName);
    }

    private static <T> List<Student> findStudentsBy(
            final Collection<Student> collection,
            final Function<? super Student, T> valueExtractor,
            final T target
    ) {
        return findStudentsBy(collection, valueExtractor, target, Collectors.toList());
    }

    private static <T, R> R findStudentsBy(
            final Collection<Student> collection,
            final Function<? super Student, T> valueExtractor,
            final T target,
            final Collector<? super Student, ?, R> collector
    ) {
        return collection.stream()
                .filter(s -> Objects.equals(valueExtractor.apply(s), target))
                .sorted(NAME_COMPARATOR)
                .collect(collector);
    }

    @Override
    public List<Student> findStudentsByLastName(
            final Collection<Student> collection,
            final String lastName
    ) {
        return findStudentsBy(collection, Student::lastName, lastName);
    }

    @Override
    public List<Student> findStudentsByGroup(
            final Collection<Student> collection,
            final GroupName groupName
    ) {
        return findStudentsBy(collection, Student::groupName, groupName);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(
            final Collection<Student> collection,
            final GroupName groupName
    ) {
        return findStudentsBy(
                collection, Student::groupName, groupName,
                Collectors.toMap(
                        Student::lastName,
                        Student::firstName,
                        BinaryOperator.minBy(String::compareTo)
                )
        );
    }

    private static Stream<Student> getByIndices(
            final Group group,
            final int[] indices
    ) {
        return Arrays.stream(indices)
                .filter(i -> i >= 0 && i < group.students().size())
                .mapToObj(i -> group.students().get(i));
    }

    private <T, R> R extractFromStudents(
            final Collection<Student> collection,
            final Function<? super Student, T> valueExtractor,
            final Collector<? super T, ?, R> collector
    ) {
        return collection.stream().map(valueExtractor).collect(collector);
    }

    @Override
    public List<String> getLastNames(final Collection<Group> collection, final int[] indices) {
        return flatMapByIndices(collection, indices, Student::lastName);
    }

    @Override
    public List<GroupName> getGroupNames(final Collection<Group> collection, final int[] indices) {
        return flatMapByIndices(collection, indices, Student::groupName);
    }

    @Override
    public List<String> getFullNames(final Collection<Group> collection, final int[] indices) {
        return flatMapByIndices(collection, indices, STUDENT_FULL_NAME);
    }
}