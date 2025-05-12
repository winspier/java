package info.kgeorgiy.ja.koloskov.iterative;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

/**
 * A thread pool-based implementation of {@link ParallelMapper} interface.
 * Allows applying a function to a list of arguments in parallel using a fixed number of worker threads.
 */
public class ParallelMapperImpl implements ParallelMapper {

    private final List<Worker> workers;
    private final Queue<Runnable> taskQueue;
    private final Object taskQueueLock = new Object();
    private final Object resultsLock = new Object();
    private boolean isClosed = false;

    /**
     * Creates a new {@code ParallelMapperImpl} with the specified number of worker threads.
     *
     * @param threads the number of threads to use
     * @throws IllegalArgumentException if {@code threads} is not positive
     */
    public ParallelMapperImpl(int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Number of threads must be positive");
        }

        taskQueue = new ArrayDeque<>();
        workers = new ArrayList<>(threads);

        for (int i = 0; i < threads; i++) {
            Worker worker = new Worker();
            worker.start();
            workers.add(worker);
        }
    }

    private void checkClosed() {
        if (isClosed) {
            throw new IllegalStateException("Mapper is closed");
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        checkClosed();
        Objects.requireNonNull(f);
        Objects.requireNonNull(args);

        TaskGroup<T, R> group = new TaskGroup<>(args.size());
        return group.execute(f, args);
    }

    @Override
    public void close() {
        synchronized (taskQueueLock) {
            if (isClosed) {
                throw new IllegalStateException("Mapper is already closed");
            }
            isClosed = true;
            taskQueueLock.notifyAll();
        }

        workers.forEach(Worker::join);
    }

    private class Worker {
        private final Thread thread;

        public Worker() {
            thread = new Thread(() -> {
                try {
                    while (true) {
                        Runnable task;
                        synchronized (taskQueueLock) {
                            while (taskQueue.isEmpty() && !isClosed) {
                                taskQueueLock.wait();
                            }
                            if (isClosed) {
                                return;
                            }
                            task = taskQueue.poll();
                        }
                        Objects.requireNonNull(task).run();
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        public void start() {
            thread.start();
        }

        public void join() {
            while (true) {
                try {
                    thread.join();
                    break;
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private class TaskGroup<T, R> {
        private final List<R> results;
        private final List<RuntimeException> exceptions = new ArrayList<>(); // :NOTE: можно вместо листа использовать одну ошибки
        private final Counter completed;

        public TaskGroup(int size) {
            results = new ArrayList<>(Collections.nCopies(size, null));
            completed = new Counter();
        }

        public List<R> execute(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
            for (int i = 0; i < args.size(); i++) {
                var task = getTask(f, args, i);
                synchronized (taskQueueLock) {
                    checkClosed();
                    taskQueue.add(task);
                    taskQueueLock.notify();
                }
            }

            synchronized (resultsLock) {
                while (completed.get() < args.size()) {
                    checkClosed();
                    resultsLock.wait();
                }
            }

            if (!exceptions.isEmpty()) {
                RuntimeException first = exceptions.getFirst();
                for (int i = 1; i < exceptions.size(); i++) {
                    first.addSuppressed(exceptions.get(i));
                }
                throw first;
            }

            return results;
        }

        private Runnable getTask(
                Function<? super T, ? extends R> f,
                List<? extends T> args,
                int i
        ) {
            return () -> {
                try {
                    if (isMapperClosed()) return;
                    R result = f.apply(args.get(i));
                    synchronized (resultsLock) {
                        if (isMapperClosed()) return;
                        results.set(i, result);
                        taskComplete();
                    }
                } catch (RuntimeException e) {
                    synchronized (resultsLock) {
                        if (isMapperClosed()) return;
                        exceptions.add(e);
                        taskComplete();
                    }
                }
            };
        }

        private void taskComplete() {
            completed.increment();
            resultsLock.notifyAll(); // :NOTE: кажется можно использовать просто notify()
        }

        private boolean isMapperClosed() {
            if (isClosed) {
                synchronized (resultsLock) {
                    exceptions.add(new IllegalStateException("Mapper was closed"));
                    taskComplete();
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Thread-safe counter used for tracking the number of completed tasks.
     */
    private static class Counter {
        private int value = 0;

        /**
         * Increments the counter by one.
         */
        public synchronized void increment() {
            value++;
        }

        /**
         * Returns the current value of the counter.
         *
         * @return the number of completed tasks
         */
        public synchronized int get() {
            return value;
        }
    }
}
