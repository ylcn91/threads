package org.doksanbir;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

/**
 * This class represents a **parallel travel agency** that generates and displays information about various travel destinations using parallel processing.
 * It leverages a ForkJoinPool to process destinations concurrently, improving performance for large numbers of destinations.
 * Each destination thread handles tasks like calculating quotations, fetching weather forecasts, and logging details.
 * This approach offers significant speedup compared to sequential processing, especially when dealing with many destinations.
 * The agency employs several key features:
 * * **Parallel stream processing:** Utilizes IntStream.parallel() to iterate over destinations and execute tasks concurrently.
 * * **Functional decomposition:** Breaks down complex tasks into smaller, pure functions like `calculateQuotation` and `getWeatherForecast`.
 * * **Immutable data structures:** Employs String arrays for destinations and weather conditions to ensure thread safety.
 * * **Performance monitoring:** Tracks execution time, memory usage, and garbage collection activity through the PerformanceMetrics class.
 * <p>
 * This declarative and parallel approach enhances efficiency and scalability for processing a large number of travel destinations.
 */
@Slf4j
public class ParallelTravelAgency {

    /**
     * The total number of available travel destinations.
     */
    private static final int DESTINATION_COUNT = 50;

    /**
     * The base price per day for travel.
     */
    static final double BASE_RATE = 100.0;

    /**
     * An array containing names of all travel destinations.
     */
    private static final String[] DESTINATIONS;

    /**
     * An array of possible weather conditions for destinations.
     */
    static final String[] WEATHER_CONDITIONS = {"Sunny", "Cloudy", "Rainy", "Stormy", "Snowy", "Windy", "Foggy", "Icy"};

    /**
     * A static Random object used for generating random numbers throughout the code.
     */
    private static final Random random = new Random();

    static {
        DESTINATIONS = IntStream.range(0, DESTINATION_COUNT)
                .mapToObj(i -> "Destination_" + (i + 1))
                .toArray(String[]::new);
    }

    /**
     * The entry point for the application. Starts performance tracking, processes destinations in parallel, and logs performance metrics.
     *
     * @param args Command-line arguments (not used in this application).
     */
    public static void main(String[] args) {
        PerformanceMetrics metrics = startPerformanceTracking();
        processDestinationsInParallel();
        logPerformanceMetrics(metrics);
    }

    /**
     * Uses a ForkJoinPool to submit a parallel task that iterates over and processes each destination individually.
     */
    static void processDestinationsInParallel() {
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        try {
            forkJoinPool.submit(() ->
                    IntStream.range(0, DESTINATION_COUNT).parallel().forEach(ParallelTravelAgency::processDestination)).join();
        } finally {
            forkJoinPool.shutdown();
        }
    }

    /**
     * Processes a single destination by calculating its quotation, fetching weather, and logging details.
     *
     * @param index The index of the destination to process.
     */
    private static void processDestination(int index) {
        String destination = DESTINATIONS[index];
        int days = random.nextInt(10) + 1;
        int people = random.nextInt(5) + 1;
        double quotation = calculateQuotation(destination, days, people);
        String weather = getWeatherForecast(destination);
        logTravelDetails(destination, weather, days, people, quotation);
    }

    /**
     * Calculates a sample quotation for a trip based on destination, days, and people.
     *
     * @param destination The name of the travel destination.
     * @param days        The number of days for the trip.
     * @param people      The number of people traveling.
     * @return The calculated quotation for the trip.
     */
    static double calculateQuotation(String destination, int days, int people) {
        double rateMultiplier = getDestinationRateMultiplier(destination);
        return BASE_RATE * rateMultiplier * days * people;
    }

    /**
     * Calculates a rate multiplier for a given destination based on a pseudo-random process.
     *
     * @param destination The name of the destination city.
     * @return A double representing the rate multiplier for the destination.
     * <p>
     * Note: This method uses a combination of `hashCode` and the Fibonacci sequence to generate a seemingly random
     * multiplier based on the destination name. This is not a secure or robust method for generating random numbers
     * and should be replaced with a proper cryptographic random number generator for production use.
     */
    static double getDestinationRateMultiplier(String destination) {
        return fib(Math.abs(destination.hashCode()) % 30);
    }

    /**
     * Calculates the nth Fibonacci number.
     *
     * @param n The index of the Fibonacci number to calculate.
     * @return The nth Fibonacci number.
     */
    static double fib(int n) {
        if (n <= 1) return n;
        return fib(n - 1) + fib(n - 2);
    }

    /**
     * Simulates retrieving the weather forecast for a given destination.
     *
     * @param destination The name of the destination city.
     * @return A string representing the simulated weather forecast for the destination.
     * <p>
     * Note: This method simulates making a network call but actually relies on a random number generator based on the
     * destination's hashCode. This is not a realistic representation of an actual weather service and should be replaced
     * with proper network calls and parsing logic for production use.
     */
    static String getWeatherForecast(String destination) {
        simulateNetworkCall();
        Random destinationSpecificRandom = new Random(destination.hashCode());
        return WEATHER_CONDITIONS[destinationSpecificRandom.nextInt(WEATHER_CONDITIONS.length)];
    }

    /**
     * Simulates a network call by introducing a 200 ms delay.
     * This method should be replaced with actual network calls and error handling logic for production use.
     */
    static void simulateNetworkCall() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread was interrupted during simulateNetworkCall", e);
        }
    }

    /**
     * Logs information about a travel quote to the logger.
     *
     * @param destination The name of the destination city.
     * @param weather     The weather forecast for the destination.
     * @param days        The number of days of the trip.
     * @param people      The number of people traveling.
     * @param quotation   The calculated travel quotation.
     */
    private static void logTravelDetails(String destination, String weather, int days, int people, double quotation) {
        log.info("Destination: {}, Weather: {}, Quotation for {} days, {} people: ${}", destination, weather, days, people, quotation);
    }

    /**
     * Starts tracking performance metrics for an operation.
     *
     * @return A PerformanceMetrics object containing the starting time, memory usage, and GC duration.
     */
    private static PerformanceMetrics startPerformanceTracking() {
        long startTime = System.currentTimeMillis();
        long startMemory = getUsedMemory();
        long startGcDuration = getTotalGCDuration();
        return new PerformanceMetrics(startTime, startMemory, startGcDuration);
    }

    /**
     * Logs performance metrics for an operation after it has finished.
     *
     * @param metrics The PerformanceMetrics object containing the starting and ending values.
     */
    private static void logPerformanceMetrics(PerformanceMetrics metrics) {
        long endTime = System.currentTimeMillis();
        long endMemory = getUsedMemory();
        long endGcDuration = getTotalGCDuration();

        log.info("Execution time: {} ms", (endTime - metrics.startTime));
        log.info("Used memory before: {} bytes", metrics.startMemory);
        log.info("Used memory after: {} bytes", endMemory);
        log.info("Memory used by operation: {} bytes", (endMemory - metrics.startMemory));
        log.info("Total garbage collection time: {} ms", (endGcDuration - metrics.startGcDuration));
    }

    /**
     * Retrieves the current amount of used memory by the JVM.
     *
     * @return The amount of memory in bytes currently used by the JVM.
     */
    private static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * Calculates the total time spent by the garbage collector since the JVM started.
     *
     * @return The total duration of garbage collection in milliseconds.
     */
    private static long getTotalGCDuration() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionTime)
                .sum();
    }

    /**
     * Class used to track performance metrics for an operation.
     * <p>
     * This class stores the starting time, memory usage, and garbage collection duration at the beginning of an operation
     * and can be used to calculate the total execution time, memory usage, and GC time after the operation is complete.
     */
    private static class PerformanceMetrics {

        /**
         * The starting time of the operation in milliseconds since the Unix epoch.
         */
        long startTime;


        /**
         * The amount of memory in bytes used by the JVM at the start of the operation.
         */
        long startMemory;

        /**
         * The total duration of garbage collection in milliseconds at the start of the operation.
         */
        long startGcDuration;

        PerformanceMetrics(long startTime, long startMemory, long startGcDuration) {
            this.startTime = startTime;
            this.startMemory = startMemory;
            this.startGcDuration = startGcDuration;
        }
    }
}
