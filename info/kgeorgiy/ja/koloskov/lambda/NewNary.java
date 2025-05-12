package info.kgeorgiy.ja.koloskov.lambda;

import info.kgeorgiy.java.advanced.lambda.Trees.Leaf;
import info.kgeorgiy.java.advanced.lambda.Trees.Nary;
import java.util.List;

public class NewNary<T> extends BaseTreeSpliterator<T, Nary<T>> {

    protected NewNary(final Nary<T> root) {
        super(root);
    }

    @Override
    protected long getSize(final Nary<T> node) {
        if (node instanceof Nary.Node<T>(final List<Nary<T>> children) && children.isEmpty()) {
            return 0;
        }
        if (node instanceof Nary.Node<T>(
                final List<Nary<T>> children
        ) && children.size() == 1 && isLeaf(children.getFirst())) {
            return 1;
        }
        return isLeaf(node) ? 1 : Long.MAX_VALUE;
    }

    @Override
    protected List<Nary<T>> trySplitBranch(final Nary<T> node) {
        return node instanceof Nary.Node<T>(final List<Nary<T>> children) ?
                List.of(
                        new Nary.Node<>(children.subList(0, children.size() / 2)),
                        new Nary.Node<>(children.subList(children.size() / 2, children.size()))
                ) : null;
    }

    @Override
    protected BaseTreeSpliterator<T, Nary<T>> construct(final Nary<T> node) {
        return new NewNary<>(node);
    }

    @Override
    protected boolean isBranch(final Nary<T> node) {
        return node instanceof Nary.Node<T>;
    }

    @Override
    protected Nary<T> getChild(final Nary<T> node, final int index) {
        if (node instanceof Nary.Node<T>(final List<Nary<T>> children)) {
            return children.get(index);
        }
        return null;
    }

    @Override
    protected T getLeafValue(final Nary<T> node, final int index) {
        return node instanceof Leaf<T>(final T value) ? value : null;
    }

    @Override
    protected int getMaxChildIndex(final Nary<T> node) {
        return node instanceof Nary.Node<T>(final List<Nary<T>> children) ? children.size() - 1 : 0;
    }
}
