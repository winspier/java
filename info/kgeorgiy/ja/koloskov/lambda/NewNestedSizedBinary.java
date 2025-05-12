package info.kgeorgiy.ja.koloskov.lambda;

import info.kgeorgiy.java.advanced.lambda.Trees.Leaf;
import info.kgeorgiy.java.advanced.lambda.Trees.SizedBinary;
import java.util.List;

public class NewNestedSizedBinary<T> extends BaseTreeSpliterator<T, SizedBinary<List<T>>> {

    protected NewNestedSizedBinary(final SizedBinary<List<T>> root) {
        super(root);
    }

    @Override
    public int characteristics() {
        return super.characteristics() & (~IMMUTABLE);
    }

    @Override
    protected long getSize(final SizedBinary<List<T>> node) { 
        return node instanceof Leaf<List<T>>(final List<T> value) ? value.size() : Long.MAX_VALUE;
    }

    @Override
    protected List<SizedBinary<List<T>>> trySplitLeaf(final SizedBinary<List<T>> node) {
        return node instanceof Leaf<List<T>>(final List<T> value)
                ? splitList(value, Leaf::new)
                : null;
    }

    @Override
    protected List<SizedBinary<List<T>>> trySplitBranch(final SizedBinary<List<T>> node) {
        return node instanceof SizedBinary.Branch<List<T>>(
                final SizedBinary<List<T>> left, final SizedBinary<List<T>> right, final int size
        ) ? List.of(left, right) : null;
    }

    @Override
    protected BaseTreeSpliterator<T, SizedBinary<List<T>>> construct(final SizedBinary<List<T>> node) {
        return new NewNestedSizedBinary<>(node);
    }

    @Override
    protected boolean isBranch(final SizedBinary<List<T>> node) {
        return node instanceof SizedBinary.Branch<List<T>>;
    }

    @Override
    protected SizedBinary<List<T>> getChild(final SizedBinary<List<T>> node, final int index) {
        if (node instanceof SizedBinary.Branch<List<T>>(
                final SizedBinary<List<T>> left, final SizedBinary<List<T>> right, final int size
        )) {
            return index == 0 ? left : right;
        }
        return null;
    }

    @Override
    protected T getLeafValue(final SizedBinary<List<T>> node, final int index) {
        return node instanceof Leaf<List<T>>(final List<T> value) ? value.get(index) : null;
    }

    @Override
    protected int getMaxLeafIndex(final SizedBinary<List<T>> node) {
        return node instanceof Leaf<List<T>>(final List<T> value)
                ? value.size() - 1
                : super.getMaxLeafIndex(node);
    }

    @Override
    protected int getMaxChildIndex(final SizedBinary<List<T>> node) {
        return isBranch(node) ? 1 : 0;
    }
}
