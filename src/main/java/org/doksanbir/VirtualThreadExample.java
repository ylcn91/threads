package org.doksanbir;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class demonstrates various aspects of Virtual Threads in Java,
 * including creation, execution, and performance characteristics.
 */
@Slf4j
public class VirtualThreadExample {

    private static final int THREAD_COUNT = 10_000;
    private static final Duration TASK_DURATION = Duration.ofMillis(10);

    public static void main(String[] args) throws InterruptedException {
        demonstrateBasicVirtualThreads();
        demonstrateVirtualThreadsWithExecutorService();
        comparePlatformAndVirtualThreads();
    }

    /**
     * Demonstrates basic creation and execution of Virtual Threads.
     */
    private static void demonstrateBasicVirtualThreads() throws InterruptedException {
        log.info("Demonstrating basic Virtual Threads:");

        Set<String> poolNames = ConcurrentHashMap.newKeySet();
        Set<String> threadNames = ConcurrentHashMap.newKeySet();

        var threads = IntStream.range(0, THREAD_COUNT)
                .mapToObj(i -> Thread.ofVirtual().name("VirtualThread-" + i).start(() -> {
                    String poolName = readPoolName();
                    poolNames.add(poolName);
                    String workerName = readWorkerName();
                    threadNames.add(workerName);
                    simulateWork();
                }))
                .toList();

        Instant begin = Instant.now();

        for (var thread : threads) {
            thread.join();
        }

        Instant end = Instant.now();
        Duration duration = Duration.between(begin, end);

        log.info("Executed {} Virtual Threads", THREAD_COUNT);
        log.info("Duration: {}", duration);
        log.info("Unique pool names: {}", poolNames.size());
        log.info("Unique thread names: {}", threadNames.size());
    }

    /**
     * Demonstrates using Virtual Threads with ExecutorService.
     */
    private static void demonstrateVirtualThreadsWithExecutorService() throws InterruptedException {
        log.info("Demonstrating Virtual Threads with ExecutorService:");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Instant begin = Instant.now();

            List<Future<String>> futures = IntStream.range(0, THREAD_COUNT)
                    .mapToObj(i -> executor.submit(() -> {
                        simulateWork();
                        return "Task " + i + " completed by " + Thread.currentThread().getName();
                    }))
                    .toList();

            for (Future<String> future : futures) {
                future.get(); // Wait for each task to complete
            }

            Instant end = Instant.now();
            Duration duration = Duration.between(begin, end);

            log.info("Executed {} tasks using Virtual Thread Executor", THREAD_COUNT);
            log.info("Duration: {}", duration);
        } catch (ExecutionException e) {
            log.error("Error during execution: {}", e.getMessage());
        }
    }

    /**
     * Compares the performance of Platform Threads and Virtual Threads.
     */
    private static void comparePlatformAndVirtualThreads() throws InterruptedException {
        log.info("Comparing Platform Threads and Virtual Threads:");

        // Platform Threads
        Instant platformBegin = Instant.now();
        try (var executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            executeTasksWithExecutor(executor);
        }
        Instant platformEnd = Instant.now();
        Duration platformDuration = Duration.between(platformBegin, platformEnd);

        // Virtual Threads
        Instant virtualBegin = Instant.now();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executeTasksWithExecutor(executor);
        }
        Instant virtualEnd = Instant.now();
        Duration virtualDuration = Duration.between(virtualBegin, virtualEnd);

        log.info("Platform Threads Duration: {}", platformDuration);
        log.info("Virtual Threads Duration: {}", virtualDuration);
        log.info("Virtual Threads were {} times faster", platformDuration.toMillis() / (double) virtualDuration.toMillis());
    }

    private static void executeTasksWithExecutor(ExecutorService executor) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    simulateWork();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
    }

    private static String readPoolName() {
        return "pool-" + Thread.currentThread().getName();
    }

    private static String readWorkerName() {
        return "worker-" + Thread.currentThread().getName();
    }

    private static void simulateWork() {
        try {
            Thread.sleep(TASK_DURATION);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Work simulation interrupted", e);
        }
    }
}