package org.doksanbir;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.IntStream;

/**
 * Demonstrates various aspects of Virtual Threads in Java, including their
 * creation, execution, and performance characteristics when compared with
 * traditional Platform Threads.
 *
 * <p>This example covers:</p>
 * <ul>
 *   <li>Basic creation and execution of Virtual Threads</li>
 *   <li>Using Virtual Threads with ExecutorService</li>
 *   <li>Performance comparison between Platform Threads and Virtual Threads</li>
 * </ul>
 */
@Slf4j
public class VirtualThreadExample {

    // Constants for configuring the number of threads and task duration
    private static final int THREAD_COUNT = 10_000;
    private static final Duration TASK_DURATION = Duration.ofMillis(10);

    /**
     * The entry point of the Java application.
     *
     * @param args Command line arguments
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public static void main(String[] args) throws InterruptedException {
        // Log the start of the example
        log.info("Starting VirtualThreadExample...");

        // Demonstrate the basic usage of Virtual Threads
        demonstrateBasicVirtualThreads();

        // Demonstrate the usage of Virtual Threads with an ExecutorService
        demonstrateVirtualThreadsWithExecutorService();

        // Compare the performance of Platform Threads and Virtual Threads
        comparePlatformAndVirtualThreads();

        // Log the completion of the example
        log.info("VirtualThreadExample completed.");
    }

    /**
     * Demonstrates basic creation and execution of Virtual Threads.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    private static void demonstrateBasicVirtualThreads() throws InterruptedException {
        log.info("Demonstrating basic Virtual Threads...");

        // Concurrent sets to track unique pool and thread names for the Virtual Threads
        Set<String> poolNames = ConcurrentHashMap.newKeySet();
        Set<String> threadNames = ConcurrentHashMap.newKeySet();

        // Create and start Virtual Threads
        List<Thread> threads = createVirtualThreads(poolNames, threadNames);

        // Measure and log the execution time of joining all Virtual Threads
        measureExecutionTime(() -> joinThreads(threads), "Executed {} Virtual Threads", THREAD_COUNT);

        // Log the number of unique pool and thread names encountered
        log.info("Unique pool names: {}", poolNames.size());
        log.info("Unique thread names: {}", threadNames.size());
    }

    /**
     * Demonstrates using Virtual Threads with an ExecutorService.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    private static void demonstrateVirtualThreadsWithExecutorService() throws InterruptedException {
        log.info("Demonstrating Virtual Threads with ExecutorService...");

        // Use a VirtualThreadPerTaskExecutor to manage Virtual Threads
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Measure and log the execution time of tasks using the Virtual Thread Executor
            measureExecutionTime(() -> executeTasksWithExecutor(executor), "Executed {} tasks using Virtual Thread Executor", THREAD_COUNT);
        }
    }

    /**
     * Compares the performance of Platform Threads and Virtual Threads using an ExecutorService.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    private static void comparePlatformAndVirtualThreads() throws InterruptedException {
        log.info("Comparing Platform Threads and Virtual Threads...");

        // Measure execution time using Platform Threads with a fixed thread pool
        Duration platformDuration = measureExecutionTime(() -> {
            try (ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
                executeTasksWithExecutor(executor);
            }
        });

        // Measure execution time using Virtual Threads with a VirtualThreadPerTaskExecutor
        Duration virtualDuration = measureExecutionTime(() -> {
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                executeTasksWithExecutor(executor);
            }
        });

        // Log the comparison results
        log.info("Platform Threads Duration: {}", platformDuration);
        log.info("Virtual Threads Duration: {}", virtualDuration);
        log.info("Virtual Threads were {} times faster", (double) platformDuration.toMillis() / virtualDuration.toMillis());
    }

    /**
     * Creates and starts Virtual Threads that simulate work by sleeping for a fixed duration.
     *
     * @param poolNames   Set to track unique pool names used by the Virtual Threads
     * @param threadNames Set to track unique thread names used by the Virtual Threads
     * @return A list of created Virtual Threads
     */
    static List<Thread> createVirtualThreads(Set<String> poolNames, Set<String> threadNames) {
        // Log the creation of Virtual Threads
        log.info("Creating and starting {} Virtual Threads...", THREAD_COUNT);

        // Create and start Virtual Threads that simulate work
        return IntStream.range(0, THREAD_COUNT)
                .mapToObj(i -> Thread.ofVirtual().name("VirtualThread-" + i).start(() -> {
                    // Track the current thread's pool and thread names
                    poolNames.add(Thread.currentThread().getName());
                    threadNames.add(Thread.currentThread().getName());
                    // Simulate work by sleeping
                    simulateWork();
                }))
                .toList();
    }

    /**
     * Joins all the provided threads, ensuring they complete before returning.
     *
     * @param threads The list of threads to join
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    protected static void joinThreads(List<Thread> threads) throws InterruptedException {
        // Join each thread in the list
        for (Thread thread : threads) {
            thread.join();
        }
    }

    /**
     * Executes a fixed number of tasks using the provided ExecutorService.
     *
     * @param executor The ExecutorService to use for executing tasks
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    private static void executeTasksWithExecutor(ExecutorService executor) throws InterruptedException {
        // Use a CountDownLatch to wait for all tasks to complete
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // Submit tasks to the executor
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    // Simulate work by sleeping
                    simulateWork();
                } finally {
                    // Count down the latch after task completion
                    latch.countDown();
                }
            });
        }

        // Await completion of all tasks
        latch.await();
    }

    /**
     * Simulates work by causing the thread to sleep for a predefined duration.
     */
    private static void simulateWork() {
        try {
            // Simulate work by sleeping for the defined duration
            Thread.sleep(TASK_DURATION.toMillis());
        } catch (InterruptedException e) {
            // Restore interrupted status and log the interruption
            Thread.currentThread().interrupt();
            log.warn("Work simulation interrupted", e);
        }
    }

    /**
     * Measures the execution time of the provided task and returns the duration.
     *
     * @param runnable The task to execute
     * @return The duration it took to execute the task
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    private static Duration measureExecutionTime(ThrowingRunnable runnable) throws InterruptedException {
        // Record the start time
        Instant start = Instant.now();
        try {
            // Execute the provided task
            runnable.run();
        } catch (ExecutionException e) {
            // Log any execution errors
            log.error("Execution error: {}", e.getMessage(), e);
        }
        // Calculate and return the duration
        return Duration.between(start, Instant.now());
    }

    /**
     * Measures the execution time of the provided task and logs the result.
     *
     * @param runnable The task to execute
     * @param message  The message template to log
     * @param args     Additional arguments to log
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    static void measureExecutionTime(ThrowingRunnable runnable, String message, Object... args) throws InterruptedException {
        // Measure the execution time
        Duration duration = measureExecutionTime(runnable);

        // Log the message with the duration
        log.info("{} in {}", message.formatted(args), duration);
    }

    /**
     * A functional interface representing a task that can throw InterruptedException or ExecutionException.
     */
    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws InterruptedException, ExecutionException;
    }
}
