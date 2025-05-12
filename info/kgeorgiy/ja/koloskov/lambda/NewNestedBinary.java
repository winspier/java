package info.kgeorgiy.ja.koloskov.lambda;

import info.kgeorgiy.java.advanced.lambda.Trees.Binary;
import info.kgeorgiy.java.advanced.lambda.Trees.Leaf;
import java.util.List;

public class NewNestedBinary<T> extends BaseTreeSpliterator<T, Binary<List<T>>> {

    protected NewNestedBinary(final Binary<List<T>> root) {
        super(root);
    }

    @Override
    public int characteristics() {
        return super.characteristics() & (~IMMUTABLE);
    }

    @Override
    protected long getSize(final Binary<List<T>> node) {
        return node instanceof Leaf<List<T>>(final List<T> value) ? value.size() : Long.MAX_VALUE;
    }

    @Override
    protected List<Binary<List<T>>> trySplitLeaf(final Binary<List<T>> node) {
        return node instanceof Leaf<List<T>>(List<T> value)
                ? splitList(value, Leaf::new)
                : null;
    }

    @Override
    protected List<Binary<List<T>>> trySplitBranch(final Binary<List<T>> node) {
        return node instanceof Binary.Branch<List<T>>(
                final Binary<List<T>> left, final Binary<List<T>> right
        ) ? List.of(left, right) : null;
    }

    @Override
    protected BaseTreeSpliterator<T, Binary<List<T>>> construct(final Binary<List<T>> node) {
        return new NewNestedBinary<>(node);
    }

    @Override
    protected boolean isBranch(final Binary<List<T>> node) {
        return node instanceof Binary.Branch<List<T>>;
    }

    @Override
    protected Binary<List<T>> getChild(final Binary<List<T>> node, final int index) {
        if (node instanceof Binary.Branch<List<T>>(
                final Binary<List<T>> left, final Binary<List<T>> right
        )) {
            return index == 0 ? left : right;
        }
        return null;
    }

    @Override
    protected T getLeafValue(final Binary<List<T>> node, final int index) {
        return node instanceof Leaf<List<T>>(final List<T> value) ? value.get(index) : null;
    }

    @Override
    protected int getMaxLeafIndex(Binary<List<T>> node) {
        return node instanceof Leaf<List<T>>(final List<T> value)
                ? value.size() - 1
                : super.getMaxLeafIndex(node);
    }

    @Override
    protected int getMaxChildIndex(final Binary<List<T>> node) {
        return isBranch(node) ? 1 : 0;
    }
}
