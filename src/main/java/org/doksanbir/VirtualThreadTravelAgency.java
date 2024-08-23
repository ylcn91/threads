package org.doksanbir;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class VirtualThreadTravelAgency {

    private static final int DESTINATION_COUNT = 50;
    private static final double BASE_RATE = 100.0;
    private static final String[] DESTINATIONS = new String[DESTINATION_COUNT];
    private static final String[] WEATHER_CONDITIONS = {
            "Sunny", "Cloudy", "Rainy", "Stormy", "Snowy", "Windy", "Foggy", "Icy"
    };

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicInteger threadCount = new AtomicInteger(0);

    private long startTime;
    private long usedMemoryBefore;
    private long totalGcDurationBefore;

    static {
        for (int i = 0; i < DESTINATION_COUNT; i++) {
            DESTINATIONS[i] = "Destination_" + (i + 1);
        }
    }

    public double getQuotation(String destination, int days, int people) {
        log.info("Calculating quotation for destination: {}", destination);
        double rate = BASE_RATE * (1 + getDestinationRateMultiplier(destination));
        return rate * days * people;
    }

    private double getDestinationRateMultiplier(String destination) {
        log.info("Calculating destination rate multiplier for destination: {}", destination);
        log.info("Simulating CPU intensive task");
        return fib(Math.abs(destination.hashCode()) % 30);
    }

    private double fib(int n) {
        if (n <= 1) return n;
        return fib(n - 1) + fib(n - 2);
    }

    public String getWeatherForecast(String destination) {
        log.info("Fetching weather forecast for destination: {}", destination);
        simulateNetworkCall();
        Random random = new Random(destination.hashCode());
        return WEATHER_CONDITIONS[random.nextInt(WEATHER_CONDITIONS.length)];
    }

    private void simulateNetworkCall() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread was interrupted during simulateNetworkCall", e);
        }
    }

    public void displayTravelPage() {
        log.info("Starting to display travel page");

        int threadCountBefore = ManagementFactory.getThreadMXBean().getThreadCount();
        initializePerformanceMetrics();

        // Submit tasks using virtual threads
        for (String destination : DESTINATIONS) {
            createAndSubmitVirtualThreads(destination);
        }

        awaitExecutorServiceTermination();
        logPerformanceMetrics(threadCountBefore);
    }

    private void initializePerformanceMetrics() {
        startTime = System.currentTimeMillis();
        Runtime runtime = Runtime.getRuntime();
        usedMemoryBefore = runtime.totalMemory() - runtime.freeMemory();
        totalGcDurationBefore = calculateTotalGcDuration();
    }

    private void createAndSubmitVirtualThreads(String destination) {
        for (int i = 0; i < 100; i++) {
            int days = new Random().nextInt(10) + 1;
            int people = new Random().nextInt(5) + 1;
            executorService.submit(() -> {
                threadCount.incrementAndGet();
                long threadName = Thread.currentThread().threadId();
                log.info("Virtual Task for destination: {} started on thread: {}", destination, threadName);
                long taskStartTime = System.nanoTime();
                String weather = getWeatherForecast(destination);
                double quotation = getQuotation(destination, days, people);
                long taskEndTime = System.nanoTime();
                log.info("Virtual Task completed for destination: {} in {} ns", destination, (taskEndTime - taskStartTime));
                log.info("Destination: {}, Weather: {}, Quotation for {} days, {} people: ${}", destination, weather, days, people, quotation);
            });
        }
    }

    private void awaitExecutorServiceTermination() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            log.error("Executor service interrupted", e);
        }
    }

    public void displayTravelPageVirtualThreads() throws InterruptedException {
        log.info("Starting to display travel page with Virtual Threads using Thread.ofVirtual()");

        int threadCountBefore = ManagementFactory.getThreadMXBean().getThreadCount();
        initializePerformanceMetrics();


        ArrayList<Thread> threads = new ArrayList<>();

        // Create and start virtual threads
        for (String destination : DESTINATIONS) {
            createAndStartVirtualThreads(threads, destination);
        }

        waitForAllThreadsToComplete(threads);

        logPerformanceMetrics(threadCountBefore);

    }

    private void createAndStartVirtualThreads(ArrayList<Thread> threads, String destination) {
        for (int i = 0; i < 100; i++) {
            var days = new Random().nextInt(10) + 1;
            var people = new Random().nextInt(5) + 1;
            Thread thread = Thread.ofVirtual()
                    .name("virtual-" + destination, 0)
                    .start(() -> {
                        threadCount.incrementAndGet();
                        long threadId = Thread.currentThread().threadId();
                        log.info("Virtual Task for destination: {} started on thread: {}", destination, threadId);
                        long taskStartTime = System.nanoTime();
                        String weather = getWeatherForecast(destination);
                        double quotation = getQuotation(destination, days, people);
                        long taskEndTime = System.nanoTime();
                        log.info("Virtual Task completed for destination: {} in {} ns", destination, (taskEndTime - taskStartTime));
                        log.info("Destination: {}, Weather: {}, Quotation for {} days, {} people: ${}", destination, weather, days, people, quotation);
                    });
            threads.add(thread);
        }
    }

    private void waitForAllThreadsToComplete(ArrayList<Thread> threads) throws InterruptedException {
        for (Thread thread : threads) {
            thread.join();
        }
    }

    public void displayTravelPagePlatformThreads() throws InterruptedException {
        log.info("Starting to display travel page with Platform Threads using Thread.ofPlatform()");

        int threadCountBefore = ManagementFactory.getThreadMXBean().getThreadCount();
        initializePerformanceMetrics();

        ArrayList<Thread> threads = new ArrayList<>();
        for (String destination : DESTINATIONS) {
            createAndStartPlatformThreads(threads, destination);
        }

        waitForAllThreadsToComplete(threads);
        logPerformanceMetrics(threadCountBefore);
    }

    private void createAndStartPlatformThreads(ArrayList<Thread> threads, String destination) {
        for (int i = 0; i < 2; i++) {
            int days = new Random().nextInt(10) + 1;
            int people = new Random().nextInt(5) + 1;
            Thread thread = Thread.ofPlatform()
                    .name("platform-" + destination + "-" + i, 0)
                    .start(() -> {
                        long threadId = Thread.currentThread().threadId();
                        log.info("Platform Task for destination: {} started on thread: {}", destination, threadId);
                        long taskStartTime = System.nanoTime();
                        String weather = getWeatherForecast(destination);
                        double quotation = getQuotation(destination, days, people);
                        long taskEndTime = System.nanoTime();
                        log.info("Platform Task completed for destination: {} in {} ns", destination, (taskEndTime - taskStartTime));
                        log.info("Destination: {}, Weather: {}, Quotation for {} days, {} people: ${}", destination, weather, days, people, quotation);
                        threadCount.decrementAndGet();
                    });
            threads.add(thread);
            threadCount.incrementAndGet();
        }
    }

    private void logPerformanceMetrics(int threadCountBefore) {
        long endTime = System.currentTimeMillis();
        Runtime runtime = Runtime.getRuntime();
        long usedMemoryAfter = runtime.totalMemory() - runtime.freeMemory();
        int threadCountAfter = ManagementFactory.getThreadMXBean().getThreadCount();
        MemoryUsage heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        long totalGcDurationAfter = calculateTotalGcDuration();
        log.info("Execution time: {} ms", (endTime - startTime));
        log.info("Used memory before: {} bytes", usedMemoryBefore);
        log.info("Used memory after: {} bytes", usedMemoryAfter);
        log.info("Memory used by operation: {} bytes", (usedMemoryAfter - usedMemoryBefore));
        log.info("Active thread count after operation: {}", threadCountAfter);
        log.info("Heap memory usage: {}", heapMemoryUsage);
        log.info("Non-heap memory usage: {}", nonHeapMemoryUsage);
        log.info("Total garbage collection time: {} ms", (totalGcDurationAfter - totalGcDurationBefore));
        log.info("Initial thread count: {}", threadCountBefore);
        log.info("Final thread count: {}", threadCount.get());

    }

    private long calculateTotalGcDuration() {
        long totalGcDuration = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            totalGcDuration += gcBean.getCollectionTime();
        }
        return totalGcDuration;
    }

    public static void main(String[] args) throws InterruptedException {
        VirtualThreadTravelAgency agency = new VirtualThreadTravelAgency();
        agency.displayTravelPageVirtualThreads();
        //agency.displayTravelPage();
        //agency.displayTravelPagePlatformThreads();
    }
}
