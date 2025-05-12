package info.kgeorgiy.ja.koloskov.lambda;

import info.kgeorgiy.java.advanced.lambda.Trees.Leaf;
import info.kgeorgiy.java.advanced.lambda.Trees.SizedBinary;
import java.util.List;

public class NewSized<T> extends BaseTreeSpliterator<T, SizedBinary<T>> {

    protected NewSized(final SizedBinary<T> root) {
        super(root);
    }

    @Override
    public int characteristics() {
        return super.characteristics() | SIZED | SUBSIZED;
    }

    @Override
    protected long getSize(final SizedBinary<T> node) {
        return isLeaf(node)
                ? 1
                : (node instanceof final SizedBinary.Branch<T> t ? t.size() : Long.MAX_VALUE);
    }

    @Override
    protected List<SizedBinary<T>> trySplitBranch(final SizedBinary<T> node) {
        return node instanceof SizedBinary.Branch<T>(
                final SizedBinary<T> left, final SizedBinary<T> right, final int size
        ) ? List.of(left, right) : null;
    }

    @Override
    protected BaseTreeSpliterator<T, SizedBinary<T>> construct(final SizedBinary<T> node) {
        return new NewSized<>(node);
    }

    @Override
    protected boolean isBranch(final SizedBinary<T> node) {
        return node instanceof SizedBinary.Branch<T>;
    }

    @Override
    protected SizedBinary<T> getChild(final SizedBinary<T> node, final int index) {
        if (node instanceof SizedBinary.Branch<T>(
                final SizedBinary<T> left, final SizedBinary<T> right, final int size
        )) {
            return index == 0 ? left : right;
        }
        return null;
    }

    @Override
    protected T getLeafValue(final SizedBinary<T> node, int index) {
        return node instanceof Leaf<T>(final T value) ? value : null;
    }

    @Override
    protected int getMaxChildIndex(final SizedBinary<T> node) {
        return isBranch(node) ? 1 : 0;
    }
}
