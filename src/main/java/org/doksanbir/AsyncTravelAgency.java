package org.doksanbir;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

/**
 * This class simulates a multi-threaded travel agency that retrieves weather forecasts and calculates travel quotes for various destinations concurrently using CompletableFuture.
 * It showcases efficient asynchronous processing and performance tracking for a large number of destinations.
 */
@Slf4j
public class AsyncTravelAgency {

    /**
     * The number of travel destinations available.
     */
    private static final int DESTINATION_COUNT = 50;

    /**
     * The base price per day for travel.
     */
    private static final double BASE_RATE = 100.0;

    /**
     * An array containing names of all travel destinations.
     */
    private static final String[] DESTINATIONS;

    /**
     * An array containing names of all weather conditions.
     */
    static final String[] WEATHER_CONDITIONS = {"Sunny", "Cloudy", "Rainy", "Stormy", "Snowy", "Windy", "Foggy", "Icy"};

    /**
     * A Random instance for generating random numbers.
     */
    private static final Random random = new Random();

    /**
     * Static initializer for initializing the `DESTINATIONS` array.
     */
    static {
        DESTINATIONS = IntStream.range(0, DESTINATION_COUNT)
                .mapToObj(i -> "Destination_" + (i + 1))
                .toArray(String[]::new);
    }

    /**
     * The main method for running the Travel Agency simulation.
     */
    public static void main(String[] args) {
        try {
            runTravelAgency();
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error occurred in running travel agency: ", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Runs the Travel Agency simulation, performing asynchronous tasks for each destination and logging performance metrics.
     *
     * @throws ExecutionException   If any exception occurs during asynchronous task execution.
     * @throws InterruptedException If the main thread is interrupted while waiting for tasks to complete.
     */
    private static void runTravelAgency() throws ExecutionException, InterruptedException {
        PerformanceMetrics metrics = startPerformanceTracking();
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                IntStream.range(0, DESTINATION_COUNT)
                        .mapToObj(AsyncTravelAgency::processDestination)
                        .toArray(CompletableFuture[]::new)
        );
        allTasks.get();
        logPerformanceMetrics(metrics);
    }

    /**
     * Processes information for a single travel destination asynchronously using CompletableFuture chaining.
     * This method calculates the travel quotation and retrieves the weather forecast concurrently, then logs the details with the chosen number of days and people.
     *
     * @param index The index of the destination in the `DESTINATIONS` array.
     * @return A CompletableFuture representing the completion of the asynchronous tasks for this destination.
     */
    static CompletableFuture<Void> processDestination(int index) {
        String destination = DESTINATIONS[index];
        int days = random.nextInt(10) + 1;
        int people = random.nextInt(5) + 1;

        return CompletableFuture.supplyAsync(() -> calculateQuotation(destination, days, people))
                .thenApplyAsync(quotation -> Pair.of(destination, quotation))
                .thenCompose(pair -> CompletableFuture.supplyAsync(() -> getWeatherForecast(pair.first))
                        .thenApply(weather -> logTravelDetails(pair.first, weather, days, people, pair.second)))
                .exceptionally(e -> {
                    log.error("Error processing destination: {}", destination, e);
                    return null;
                });
    }

    /**
     * Calculates the travel quotation for the specified destination, days, and people.
     *
     * @param destination The name of the travel destination.
     * @param days        The desired number of days for the trip.
     * @param people      The number of people traveling.
     * @return The calculated travel quote as a double.
     */
    static double calculateQuotation(String destination, int days, int people) {
        double rateMultiplier = getDestinationRateMultiplier(destination);
        return BASE_RATE * rateMultiplier * days * people;
    }

    /**
     * Calculates a rate multiplier for the specified destination based on a pseudo-random process using the Fibonacci sequence.
     *
     * @param destination The name of the travel destination.
     * @return A double representing the rate multiplier for the destination.
     * <p>
     * Note: This method uses a combination of `hashCode` and the Fibonacci sequence to generate a seemingly random multiplier.
     * This is not a secure or robust method for generating random numbers and should be replaced with a proper cryptographic
     * random number generator for production use.
     */
    private static double getDestinationRateMultiplier(String destination) {
        return fib(Math.abs(destination.hashCode()) % 30);
    }

    /**
     * Calculates the nth Fibonacci number using a recursive algorithm.
     *
     * @param n The index of the Fibonacci number to calculate.
     * @return The calculated Fibonacci number as a double.
     */
    private static double fib(int n) {
        if (n <= 1) return n;
        return fib(n - 1) + fib(n - 2);
    }


    /**
     * Simulates retrieving the weather forecast for the specified destination using a random condition from `WEATHER_CONDITIONS`.
     *
     * @param destination The name of the travel destination.
     * @return A randomly chosen weather condition string from the `WEATHER_CONDITIONS` array.
     */
    static String getWeatherForecast(String destination) {
        simulateNetworkCall();
        Random destinationSpecificRandom = new Random(destination.hashCode());
        return WEATHER_CONDITIONS[destinationSpecificRandom.nextInt(WEATHER_CONDITIONS.length)];
    }


    /**
     * Simulates a network call with a 200ms delay.
     * This method is used by getWeatherForecast for demonstration purposes.
     */
    private static void simulateNetworkCall() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread was interrupted during simulateNetworkCall", e);
        }
    }

    /**
     * Logs information about a specific travel destination, including quote, weather, days, people, and destination name.
     *
     * @param destination The name of the travel destination.
     * @param weather     The retrieved weather forecast for the destination.
     * @param days        The chosen number of days for the trip.
     * @param people      The chosen number of people traveling.
     * @param quotation   The calculated travel quote for the specified number of days and people.
     * @return Void indicating successful logging.
     */
    private static Void logTravelDetails(String destination, String weather, int days, int people, double quotation) {
        log.info("Destination: {}, Weather: {}, Quotation for {} days, {} people: ${}", destination, weather, days, people, quotation);
        return null;
    }

    /**
     * Retrieves the total duration of all garbage collection operations.
     *
     * @return The total duration of all garbage collection operations in milliseconds.
     */
    private static long getTotalGCDuration() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionTime)
                .sum();
    }

    /**
     * Retrieves the total memory used by the JVM.
     *
     * @return The total memory used by the JVM in bytes.
     */
    private static long getUsedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    /**
     * Logs performance metrics for the Travel Agency simulation.
     *
     * @param metrics The performance metrics to log.
     */
    private static void logPerformanceMetrics(PerformanceMetrics metrics) {
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
    }


    /**
     * Starts tracking performance metrics for the Travel Agency simulation.
     *
     * @return A PerformanceMetrics object containing the start time, memory usage, garbage collection duration, and thread count at the start of the simulation.
     */
    static PerformanceMetrics startPerformanceTracking() {
        long startTime = System.currentTimeMillis();
        long startMemory = getUsedMemory();
        long startGcDuration = getTotalGCDuration();
        int initialThreadCount = ManagementFactory.getThreadMXBean().getThreadCount();
        return new PerformanceMetrics(startTime, startMemory, startGcDuration, initialThreadCount);
    }

    /**
     * A record containing performance metrics for the Travel Agency simulation.
     */
    static class PerformanceMetrics {
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

    /**
     * A record containing two values of different types.
     * This is used to return multiple values from a method.
     *
     * @param <T> The type of the first value.
     * @param <U> The type of the second value.
     */
    private record Pair<T, U>(T first, U second) {
        static <T, U> Pair<T, U> of(T first, U second) {
            return new Pair<>(first, second);
        }
    }
}
