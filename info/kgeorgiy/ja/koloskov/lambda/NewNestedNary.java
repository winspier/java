package info.kgeorgiy.ja.koloskov.lambda;

import info.kgeorgiy.java.advanced.lambda.Trees.Leaf;
import info.kgeorgiy.java.advanced.lambda.Trees.Nary;
import java.util.List;

public class NewNestedNary<T> extends BaseTreeSpliterator<T, Nary<List<T>>> {

    protected NewNestedNary(final Nary<List<T>> root) {
        super(root);
    }

    @Override
    public int characteristics() {
        return super.characteristics() & (~IMMUTABLE);
    }

    @Override
    protected long getSize(final Nary<List<T>> node) {
        return node instanceof Leaf<List<T>>(List<T> value) ? value.size() : Long.MAX_VALUE;
    }

    @Override
    protected List<Nary<List<T>>> trySplitLeaf(final Nary<List<T>> node) {
        return node instanceof Leaf<List<T>>(List<T> value)
                ? splitList(value, Leaf::new)
                : null;
    }

    @Override
    protected List<Nary<List<T>>> trySplitBranch(final Nary<List<T>> node) {
        return node instanceof Nary.Node<List<T>>(final List<Nary<List<T>>> children) ?
                List.of(
                        new Nary.Node<>(children.subList(0, children.size() / 2)),
                        new Nary.Node<>(children.subList(children.size() / 2, children.size()))
                ) : null;
    }

    @Override
    protected BaseTreeSpliterator<T, Nary<List<T>>> construct(final Nary<List<T>> node) {
        return new NewNestedNary<>(node);
    }

    @Override
    protected boolean isBranch(final Nary<List<T>> node) {
        return node instanceof Nary.Node<List<T>>;
    }

    @Override
    protected Nary<List<T>> getChild(final Nary<List<T>> node, final int index) {
        if (node instanceof Nary.Node<List<T>>(final List<Nary<List<T>>> children)) {
            return children.get(index);
        }
        return null;
    }

    @Override
    protected T getLeafValue(final Nary<List<T>> node, int index) {
        return node instanceof Leaf<List<T>>(final List<T> value) ? value.get(index) : null;
    }

    @Override
    protected int getMaxLeafIndex(Nary<List<T>> node) {
        return node instanceof Leaf<List<T>>(final List<T> value)
                ? value.size() - 1
                : super.getMaxLeafIndex(node);
    }

    @Override
    protected int getMaxChildIndex(final Nary<List<T>> node) {
        return node instanceof Nary.Node<List<T>>(final List<Nary<List<T>>> children) ?
                children.size() - 1 : 0;
    }
}
