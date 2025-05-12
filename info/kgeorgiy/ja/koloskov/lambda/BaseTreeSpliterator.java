package info.kgeorgiy.ja.koloskov.lambda;

import info.kgeorgiy.java.advanced.lambda.Trees.Leaf;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class BaseTreeSpliterator<T, Tree> implements Spliterator<T> {
    private List<AbstractMap.SimpleEntry<Tree, Integer>> list;

    protected BaseTreeSpliterator(final Tree root) {
        this.list = new ArrayList<>(List.of(new AbstractMap.SimpleEntry<>(root, 0)));
    }

    protected static <T, N> List<N> splitList(
            final List<T> list,
            final Function<List<T>, N> nodeMaker
    ) {
        if (list.size() <= 1) {
            return null;
        }
        return List.of(
                nodeMaker.apply(list.subList(0, list.size() / 2)),
                nodeMaker.apply(list.subList(list.size() / 2, list.size()))
        );
    }

    @Override
    public boolean tryAdvance(final Consumer<? super T> action) {
        if (list.isEmpty()) {
            return false;
        }
        final var e = list.getLast();
        var root = e.getKey();
        int cnt = e.getValue();

        while (isBranch(root)) {
            root = getChild(root, cnt);
            cnt = 0;
            list.add(new AbstractMap.SimpleEntry<>(root, cnt));
        }

        if (isLeaf(root)) {
            final int i = list.getLast().getValue();
            action.accept(getLeafValue(root, i));
            if (i + 1 <= getMaxLeafIndex(root)) {
                list.removeLast();
                list.add(new SimpleEntry<>(root, i + 1));
            } else {
                do {
                    list.removeLast();
                    if (list.isEmpty()) {
                        break;
                    }
                    final var c = new AbstractMap.SimpleEntry<>(
                            list.getLast().getKey(),
                            list.getLast().getValue() + 1
                    );
                    list.removeLast();
                    list.addLast(c);
                } while (!list.isEmpty()
                        && list.getLast().getValue() > getMaxChildIndex(list.getLast().getKey()));
            }
            return true;
        }
        return false;
    }

    @Override
    public Spliterator<T> trySplit() {
        if (list.size() != 1) {
            return null;
        }
        final var e = list.getLast();
        var root = e.getKey();
        final int cnt = e.getValue();

        if (cnt != 0) {
            return null;
        }

        if (isLeaf(root)) {
            final var res = trySplitLeaf(root);
            if (res == null) {
                return null;
            }
            return applySplit(res.getFirst(), res.getLast());
        }

        while (isBranch(root) && getMaxChildIndex(root) == 0) {
            root = getChild(root, 0);
        }

        if (isBranch(root)) {
            final var res = trySplitBranch(root);
            if (res == null) {
                return null;
            }
            return applySplit(res.getFirst(), res.getLast());
        }
        return null;
    }

    @Override
    public long estimateSize() {
        if (list.isEmpty()) {
            return 0;
        }
        return getSize(list.getLast().getKey());
    }

    @Override
    public int characteristics() {
        return IMMUTABLE | ORDERED;
    }

    protected abstract long getSize(Tree node);

    protected List<Tree> trySplitLeaf(final Tree node) {
        return null;
    }

    private BaseTreeSpliterator<T, Tree> applySplit(final Tree first, final Tree second) {
        list = new ArrayList<>(List.of(new SimpleEntry<>(second, 0)));
        return construct(first);
    }

    protected abstract List<Tree> trySplitBranch(Tree node);

    protected abstract BaseTreeSpliterator<T, Tree> construct(Tree node);

    protected abstract boolean isBranch(Tree node);

    protected abstract Tree getChild(Tree node, int index);

    protected boolean isLeaf(final Tree node) {
        return node instanceof Leaf;
    }

    protected abstract T getLeafValue(Tree node, int index);

    protected int getMaxLeafIndex(final Tree node) {
        return 0;
    }

    protected abstract int getMaxChildIndex(Tree node);

}
