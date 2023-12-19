package org.doksanbir;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.GarbageCollectorMXBean;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class AdvancedConcurrentTravelAgency {

    /**
     * The number of travel destinations available.
     */
    private static final int DESTINATION_COUNT = 50;

    /**
     * The base price per day for travel.
     */
    private static final double BASE_RATE = 100.0;

    /**
     * A map containing the weather forecast for each destination.
     */
    private static final ConcurrentMap<String, String> destinationWeather = new ConcurrentHashMap<>();

    /**
     * A list containing names of all travel destinations.
     */
    private static final CopyOnWriteArrayList<String> destinations = new CopyOnWriteArrayList<>();

    /**
     * A semaphore to limit the number of concurrent weather forecast requests.
     */
    private static final Semaphore weatherSemaphore = new Semaphore(10);

    /**
     * A thread pool to process quotation requests.
     */
    static final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * A counter to keep track of the number of quotations processed.
     */
    private final AtomicInteger quotationCounter = new AtomicInteger();

    /**
     * An array containing names of all travel destinations.
     */
    static final String[] WEATHER_CONDITIONS = {
            "Sunny", "Cloudy", "Rainy", "Stormy", "Snowy", "Windy", "Foggy", "Icy"
    };

    /**
     * Initializes the list of destinations.
     */
    static {
        for (int i = 0; i < DESTINATION_COUNT; i++) {
            destinations.add("Destination_" + (i + 1));
        }
    }

    /**
     * Calculates a sample quotation for a trip to the specified destination for the given number of days and people.
     *
     * @param destination The name of the travel destination.
     * @param days        The number of days for the trip.
     * @param people      The number of people traveling.
     * @return The calculated quotation for the trip.
     */
    public double getQuotation(String destination, int days, int people) {
        log.info("Calculating quotation for destination: {}", destination);
        double rate = BASE_RATE * (1 + getDestinationRateMultiplier(destination));
        return rate * days * people;
    }

    private double getDestinationRateMultiplier(String destination) {
        log.info("Calculating destination rate multiplier for destination: {}", destination);
        return fib(Math.abs(destination.hashCode()) % 30);
    }

    private double fib(int n) {
        if (n <= 1) return n;
        return fib(n - 1) + fib(n - 2);
    }

    public Future<Double> getQuotationAsync(String destination, int days, int people) {
        log.info("Asynchronously fetching quotation for destination: {}", destination);
        return executorService.submit(() -> {
            double quotation = getQuotation(destination, days, people);
            quotationCounter.incrementAndGet();
            log.info("Quotation calculated for destination: {}", destination);
            return quotation;
        });
    }

    public String getWeatherForecast(String destination) throws InterruptedException {
        log.info("Fetching weather forecast for destination: {}", destination);
        String weather = destinationWeather.get(destination);
        if (weather == null) {
            weatherSemaphore.acquire();
            try {
                weather = destinationWeather.computeIfAbsent(destination, this::fetchWeatherForecast);
            } finally {
                weatherSemaphore.release();
            }
        }
        log.info("Weather forecast for destination: {}", destination);
        return weather;
    }

    private String fetchWeatherForecast(String destination) {
        log.info("Actually fetching weather forecast for destination: {}", destination);
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

    public void displayTravelPage() throws InterruptedException {
        PerformanceMetrics metrics = startPerformanceTracking();

        CountDownLatch latch = new CountDownLatch(DESTINATION_COUNT);
        destinations.forEach(destination ->
                processDestination(destination, latch)
        );

        latch.await();
        executorService.shutdown();
        logPerformanceMetrics(metrics);
    }

    private void processDestination(String destination, CountDownLatch latch) {
        int days = new Random().nextInt(10) + 1;
        int people = new Random().nextInt(5) + 1;
        executorService.submit(() -> {
            try {
                String weather = getWeatherForecast(destination);
                double quotation = getQuotationAsync(destination, days, people).get();
                log.info("Destination: {}, Weather: {}, Quotation for {} days, {} people: ${}",
                        destination, weather, days, people, quotation);
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error processing destination: {}", destination, e);
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });
    }

    private PerformanceMetrics startPerformanceTracking() {
        long startTime = System.currentTimeMillis();
        long startMemory = getUsedMemory();
        long startGcDuration = getTotalGCDuration();
        int initialThreadCount = ManagementFactory.getThreadMXBean().getThreadCount();
        return new PerformanceMetrics(startTime, startMemory, startGcDuration, initialThreadCount);
    }

    private void logPerformanceMetrics(PerformanceMetrics metrics) {
        long endTime = System.currentTimeMillis();
        long endMemory = getUsedMemory();
        long endGcDuration = getTotalGCDuration();
        int finalThreadCount = ManagementFactory.getThreadMXBean().getThreadCount();

        log.info("Execution time: {} ms", (endTime - metrics.startTime));
        log.info("Used memory before: {} bytes", metrics.startMemory);
        log.info("Used memory after: {} bytes", endMemory);
        log.info("Memory used by operation: {} bytes", (endMemory - metrics.startMemory));
        log.info("Total garbage collection time: {} ms", (endGcDuration - metrics.startGcDuration));
        log.info("Initial thread count: {}", metrics.initialThreadCount);
        log.info("Final thread count: {}", finalThreadCount);
        log.info("Total quotations processed: {}", quotationCounter.get());
    }

    private static long getUsedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    private static long getTotalGCDuration() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionTime)
                .sum();
    }

    private static class PerformanceMetrics {
        long startTime;
        long startMemory;
        long startGcDuration;
        int initialThreadCount;

        PerformanceMetrics(long startTime, long startMemory, long startGcDuration, int initialThreadCount) {
            this.startTime = startTime;
            this.startMemory = startMemory;
            this.startGcDuration = startGcDuration;
            this.initialThreadCount = initialThreadCount;
        }
    }

    public static void main(String[] args) {
        log.info("Starting travel agency application");
        AdvancedConcurrentTravelAgency agency = new AdvancedConcurrentTravelAgency();
        try {
            agency.displayTravelPage();
        } catch (InterruptedException e) {
            log.error("Error occurred", e);
            Thread.currentThread().interrupt();
        }
        log.info("Exiting travel agency application");
    }
}
