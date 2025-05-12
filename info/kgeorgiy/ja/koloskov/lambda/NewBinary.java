package info.kgeorgiy.ja.koloskov.lambda;

import info.kgeorgiy.java.advanced.lambda.Trees.Binary;
import info.kgeorgiy.java.advanced.lambda.Trees.Leaf;

import java.util.List;

public class NewBinary<T> extends BaseTreeSpliterator<T, Binary<T>> {

    protected NewBinary(Binary<T> root) {
        super(root);
    }

    @Override
    protected long getSize(Binary<T> node) {
        if (node instanceof Binary.Branch<T>(Binary<T> left, Binary<T> right) &&
                isLeaf(left) && isLeaf(right)) {
            return 2;
        }
        return isLeaf(node) ? 1 : Long.MAX_VALUE;
    }

    @Override
    protected List<Binary<T>> trySplitBranch(Binary<T> node) {
        return node instanceof Binary.Branch<T>(Binary<T> left, Binary<T> right) ?
                List.of(left, right) : null;
    }

    @Override
    protected BaseTreeSpliterator<T, Binary<T>> construct(Binary<T> node) {
        return new NewBinary<>(node);
    }

    @Override
    protected boolean isBranch(Binary<T> node) {
        return node instanceof Binary.Branch<T>;
    }

    @Override
    protected Binary<T> getChild(Binary<T> node, int index) {
        if (node instanceof Binary.Branch<T>(Binary<T> left, Binary<T> right)) {
            return index == 0 ? left : right;
        }
        return null;
    }

    @Override
    protected T getLeafValue(Binary<T> node, int index) {
        return node instanceof Leaf<T>(T value) ? value : null;
    }

    @Override
    protected int getMaxChildIndex(Binary<T> node) {
        return isBranch(node) ? 1 : 0;
    }
}
