package org.doksanbir;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Random;
import java.util.concurrent.*;
import java.util.stream.IntStream;

/**
 * This class simulates a multi-threaded travel agency that retrieves weather forecasts
 * and calculates travel quotes for various destinations concurrently.
 * It displays a travel page with destinations, weather forecasts, and sample quotations
 * for a random number of days and people.
 */
@Slf4j
public class ThreadedTravelAgency {

    private static final int DESTINATION_COUNT = 50;
    private static final double BASE_RATE = 100.0;
    private static final String[] DESTINATIONS = IntStream.range(0, DESTINATION_COUNT)
            .mapToObj(i -> "Destination_" + (i + 1))
            .toArray(String[]::new);
    private static final String[] WEATHER_CONDITIONS = {
            "Sunny", "Cloudy", "Rainy", "Stormy", "Snowy", "Windy", "Foggy", "Icy"
    };

    private final ExecutorService executorService;
    private final Random random;

    public ThreadedTravelAgency() {
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.random = new Random();
    }

    /**
     * Submits a task to asynchronously calculate and return a travel quotation for the specified destination, days, and people.
     *
     * @param destination The name of the travel destination.
     * @param days        The desired number of days for the trip.
     * @param people      The number of people traveling.
     * @return A CompletableFuture object containing the calculated quote upon completion.
     */
    public CompletableFuture<Double> getQuotationAsync(String destination, int days, int people) {
        return CompletableFuture.supplyAsync(() -> getQuotation(destination, days, people), executorService);
    }

    /**
     * Submits a task to asynchronously retrieve the weather forecast for the specified destination.
     *
     * @param destination The name of the travel destination.
     * @return A CompletableFuture object containing the retrieved weather forecast upon completion.
     */
    public CompletableFuture<String> getWeatherForecastAsync(String destination) {
        return CompletableFuture.supplyAsync(() -> getWeatherForecast(destination), executorService);
    }

    /**
     * Calculates the travel quotation for the specified destination, days, and people.
     *
     * @param destination The name of the travel destination.
     * @param days        The desired number of days for the trip.
     * @param people      The number of people traveling.
     * @return The calculated travel quote as a double.
     */
    private double getQuotation(String destination, int days, int people) {
        double rate = BASE_RATE * (1 + getDestinationRateMultiplier(destination));
        return rate * days * people;
    }

    /**
     * Simulates a network call to fetch the weather forecast for the specified destination.
     * This method introduces a delay of 200 milliseconds for demonstration purposes.
     *
     * @param destination The name of the travel destination.
     * @return A randomly chosen weather condition from the available options.
     */
    public String getWeatherForecast(String destination) {
        simulateNetworkCall();
        return WEATHER_CONDITIONS[new Random(destination.hashCode()).nextInt(WEATHER_CONDITIONS.length)];
    }

    /**
     * Simulates a network delay of 200 milliseconds.
     */
    private void simulateNetworkCall() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread was interrupted during simulateNetworkCall", e);
        }
    }

    /**
     * Calculates a rate multiplier for the specified destination based on a pseudo-random process using the Fibonacci sequence.
     *
     * @param destination The name of the travel destination.
     * @return A double representing the rate multiplier for the destination.
     *
     * Note: This method uses a combination of `hashCode` and the Fibonacci sequence to generate a seemingly random multiplier.
     * This is not a secure or robust method for generating random numbers and should be replaced with a proper cryptographic
     * random number generator for production use.
     */
    private double getDestinationRateMultiplier(String destination) {
        return fib(Math.abs(destination.hashCode()) % 30) / 100.0;
    }

    /**
     * Calculates the nth Fibonacci number using an iterative approach.
     *
     * @param n The index of the Fibonacci number to calculate.
     * @return The nth Fibonacci number.
     */
    private int fib(int n) {
        if (n <= 1) return n;
        int a = 0, b = 1;
        for (int i = 2; i <= n; i++) {
            int temp = a + b;
            a = b;
            b = temp;
        }
        return b;
    }

    /**
     * Displays the Travel Agency page by fetching and displaying information for each destination concurrently.
     * This method tracks performance metrics like execution time, memory usage, and garbage collection.
     */
    public void displayTravelPage() {
        PerformanceMetrics metrics = startPerformanceTracking();

        CompletableFuture<?>[] futures = new CompletableFuture<?>[DESTINATIONS.length];
        for (int i = 0; i < DESTINATIONS.length; i++) {
            futures[i] = processDestination(DESTINATIONS[i]);
        }
        CompletableFuture.allOf(futures).join();

        logPerformanceMetrics(metrics);
        executorService.shutdown();
    }

    /**
     * Processes information for a single travel destination asynchronously.
     * This method fetches the weather forecast and calculates a travel quote concurrently using separate tasks.
     * It then logs the retrieved information along with the chosen number of days and people for the trip.
     *
     * @param destination The name of the travel destination.
     * @return A CompletableFuture that completes when all processing for the destination is finished.
     */
    private CompletableFuture<Void> processDestination(String destination) {
        int days = random.nextInt(10) + 1;
        int people = random.nextInt(5) + 1;

        return CompletableFuture.allOf(
                getWeatherForecastAsync(destination)
                        .thenCombine(getQuotationAsync(destination, days, people),
                                (weather, quotation) -> {
                                    logDestinationDetails(destination, weather, quotation, days, people);
                                    return null;
                                })
        );
    }

    /**
     * Logs information about a specific travel destination.
     * This method prints the destination name, weather forecast, calculated quotation, number of days, and number of people
     * using the `log.info` method of the `Slf4j` logger.
     *
     * @param destination The name of the travel destination.
     * @param weather     The retrieved weather forecast for the destination.
     * @param quotation   The calculated travel quote for the specified number of days and people.
     * @param days        The chosen number of days for the trip.
     * @param people      The chosen number of people traveling.
     */
    private void logDestinationDetails(String destination, String weather, double quotation, int days, int people) {
        log.info("Destination: {}, Weather: {}, Quotation for {} days, {} people: ${}",
                destination, weather, days, people, String.format("%.2f", quotation));
    }

    /**
     * Starts tracking performance metrics for the `displayTravelPage` method.
     * This method captures the starting time, memory usage, and total garbage collection duration before processing any destinations.
     *
     * @return A `PerformanceMetrics` object containing the initial performance values.
     */
    private PerformanceMetrics startPerformanceTracking() {
        return new PerformanceMetrics(
                System.currentTimeMillis(),
                getUsedMemory(),
                getTotalGCDuration()
        );
    }

    /**
     * Logs performance metrics after finishing processing all travel destinations.
     * This method calculates the total execution time, memory usage, and garbage collection time using the provided `PerformanceMetrics` object
     * and the current time and memory measurements. It then logs these metrics using the `log.info` method of the `Slf4j` logger,
     * including the final thread count.
     *
     * @param metrics The `PerformanceMetrics` object containing the initial performance values.
     */
    private void logPerformanceMetrics(PerformanceMetrics metrics) {
        long endTime = System.currentTimeMillis();
        long endMemory = getUsedMemory();
        long endGcDuration = getTotalGCDuration();
        int finalThreadCount = ManagementFactory.getThreadMXBean().getThreadCount();

        log.info("Execution time: {} ms", (endTime - metrics.startTime));
        log.info("Memory used by operation: {} bytes", (endMemory - metrics.startMemory));
        log.info("Total garbage collection time: {} ms", (endGcDuration - metrics.startGcDuration));
        log.info("Final thread count: {}", finalThreadCount);
    }

    /**
     * Retrieves the current amount of memory used by the JVM.
     * This method calculates the used memory in bytes by subtracting the free memory from the total memory available to the JVM.
     *
     * @return The amount of memory in bytes currently used by the JVM.
     */
    private static long getUsedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    /**
     * Calculates the total time spent by the garbage collector since the JVM started.
     * This method retrieves the collection time of all garbage collector MXBeans available in the JVM and sums them up.
     *
     * @return The total duration of garbage collection in milliseconds.
     */
    private static long getTotalGCDuration() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionTime)
                .sum();
    }

    /**
     * Record used to track performance metrics for an operation.
     * This record stores the starting time, memory usage, and garbage collection duration at the beginning of an operation
     * and can be used to calculate the total execution time, memory usage, and GC time after the operation is complete.
     */
    private record PerformanceMetrics(long startTime, long startMemory, long startGcDuration) {}

    /**
     * This is the entry point for the application.
     * It creates an instance of the `ThreadedTravelAgency` class and calls its `displayTravelPage` method to generate and display the travel page.
     *
     * @param args Command-line arguments (not used in this application).
     */
    public static void main(String[] args) {
        new ThreadedTravelAgency().displayTravelPage();
    }
}