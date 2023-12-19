package org.doksanbir;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This class simulates a multi-threaded travel agency that retrieves weather forecasts and calculates travel quotes for various destinations.
 * It displays a travel page with destinations, weather forecasts, and sample quotations for a random number of days and people.
 */
@Slf4j
public class ThreadedTravelAgency {

    private static final int DESTINATION_COUNT = 50;
    private static final double BASE_RATE = 100.0;
    private static final String[] DESTINATIONS = new String[DESTINATION_COUNT];
    private static final String[] WEATHER_CONDITIONS = {
            "Sunny", "Cloudy", "Rainy", "Stormy", "Snowy", "Windy", "Foggy", "Icy"
    };
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final Random random = new Random();

    static {
        for (int i = 0; i < DESTINATION_COUNT; i++) {
            DESTINATIONS[i] = "Destination_" + (i + 1);
        }
    }

    /**
     * Submits a task to asynchronously calculate and return a travel quotation for the specified destination, days, and people.
     *
     * @param destination The name of the travel destination.
     * @param days        The desired number of days for the trip.
     * @param people      The number of people traveling.
     * @return A Future object containing the calculated quote upon completion.
     */
    public Future<Double> getQuotationAsync(String destination, int days, int people) {
        return executorService.submit(() -> getQuotation(destination, days, people));
    }

    /**
     * Submits a task to asynchronously retrieve the weather forecast for the specified destination.
     *
     * @param destination The name of the travel destination.
     * @return A Future object containing the retrieved weather forecast upon completion.
     */
    public Future<String> getWeatherForecastAsync(String destination) {
        return executorService.submit(() -> getWeatherForecast(destination));
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
        Random random = new Random(destination.hashCode());
        return WEATHER_CONDITIONS[random.nextInt(WEATHER_CONDITIONS.length)];
    }

    /**
     * Simulates a network delay of 200 milliseconds.
     */
    private void simulateNetworkCall() {
        try {
            // Simulate network delay of 200 milliseconds
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
     * <p>
     * Note: This method uses a combination of `hashCode` and the Fibonacci sequence to generate a seemingly random multiplier.
     * This is not a secure or robust method for generating random numbers and should be replaced with a proper cryptographic
     * random number generator for production use.
     */
    private double getDestinationRateMultiplier(String destination) {
        // Ensure non-negative input for the Fibonacci calculation
        return fib(Math.abs(destination.hashCode()) % 30);
    }

    /**
     * Inefficient recursive Fibonacci method simulating a CPU-intensive task.
     *
     * @param n The index of the Fibonacci number to calculate.
     * @return The nth Fibonacci number.
     */
    private double fib(int n) {
        if (n <= 1) return n;
        return fib(n - 1) + fib(n - 2);
    }

    /**
     * Displays the Travel Agency page by fetching and displaying information for each destination concurrently.
     * <p>
     * This method tracks performance metrics like execution time, memory usage, and garbage collection.
     */
    public void displayTravelPage() {
        PerformanceMetrics metrics = startPerformanceTracking();

        for (var destination : DESTINATIONS) {
            processDestination(destination);
        }

        logPerformanceMetrics(metrics);
        executorService.shutdown();
    }

    /**
     * Processes information for a single travel destination asynchronously.
     * This method fetches the weather forecast and calculates a travel quote concurrently using separate tasks.
     * It then logs the retrieved information along with the chosen number of days and people for the trip.
     *
     * @param destination The name of the travel destination.
     */
    private void processDestination(String destination) {
        int days = random.nextInt(10) + 1;  // Random number of days between 1 and 10
        int people = random.nextInt(5) + 1;  // Random number of people between 1 and 5
        try {
            Future<String> weatherFuture = getWeatherForecastAsync(destination);
            Future<Double> quotationFuture = getQuotationAsync(destination, days, people);

            logDestinationDetails(destination, weatherFuture.get(), quotationFuture.get(), days, people);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error in processing destination: {}", destination, e);
            Thread.currentThread().interrupt();
        }
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
        log.info("Destination: {}, Weather: {}, Quotation for {} days, {} people: ${}", destination, weather, days, people, quotation);
    }

    /**
     * Starts tracking performance metrics for the `displayTravelPage` method.
     * This method captures the starting time, memory usage, and total garbage collection duration before processing any destinations.
     * It returns a `PerformanceMetrics` object that can be used later to calculate the total execution time, memory usage, and GC time.
     *
     * @return A `PerformanceMetrics` object containing the initial performance values.
     */
    private PerformanceMetrics startPerformanceTracking() {
        long startTime = System.currentTimeMillis();
        long startMemory = getUsedMemory();
        long startGcDuration = getTotalGCDuration();
        return new PerformanceMetrics(startTime, startMemory, startGcDuration);
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
        log.info("Used memory before: {} bytes", metrics.startMemory);
        log.info("Used memory after: {} bytes", endMemory);
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
     * The result is returned in milliseconds.
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
     * This class stores the starting time, memory usage, and garbage collection duration at the beginning of an operation
     * and can be used to calculate the total execution time, memory usage, and GC time after the operation is complete.
     */
    private static class PerformanceMetrics {
        long startTime;
        long startMemory;
        long startGcDuration;

        PerformanceMetrics(long startTime, long startMemory, long startGcDuration) {
            this.startTime = startTime;
            this.startMemory = startMemory;
            this.startGcDuration = startGcDuration;
        }
    }

    /**
     * This is the entry point for the application.
     * It creates an instance of the `ThreadedTravelAgency` class and calls its `displayTravelPage` method to generate and display the travel page.
     * It also measures and logs performance metrics like execution time, memory usage, and garbage collection during the travel page generation.
     *
     * @param args Command-line arguments (not used in this application).
     */
    public static void main(String[] args) {
        ThreadedTravelAgency agency = new ThreadedTravelAgency();
        agency.displayTravelPage();
    }
}
