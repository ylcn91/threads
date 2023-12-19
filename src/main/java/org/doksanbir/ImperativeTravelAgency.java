package org.doksanbir;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.Random;

/**
 * This class represents a travel agency that provides information and quotations for various travel destinations.
 * It displays a travel page with destinations, weather forecasts, and sample quotations for a random number of days and people.
 * It also monitors performance metrics like execution time, memory usage, and garbage collection.
 */
@Slf4j
public class ImperativeTravelAgency {

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
    private static final String[] DESTINATIONS = new String[DESTINATION_COUNT];
    static final String[] WEATHER_CONDITIONS = {
            "Sunny", "Cloudy", "Rainy", "Stormy", "Snowy", "Windy", "Foggy", "Icy"
    };

    static {
        for (int i = 0; i < DESTINATION_COUNT; i++) {
            DESTINATIONS[i] = "Destination_" + (i + 1);
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
     * Simulates a network call with a 200ms delay.
     * This method is used by getWeatherForecast for demonstration purposes.
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
     * Calculates a multiplier based on the destination for adjusting the base rate.
     * This method uses the Fibonacci sequence (mod 30) for demonstration purposes.
     *
     * @param destination The name of the travel destination.
     * @return A multiplier for the base rate based on the destination.
     */
    double getDestinationRateMultiplier(String destination) {
        return fib(Math.abs(destination.hashCode()) % 30);
    }

    /**
     * Calculates the nth Fibonacci number (used for the destination rate multiplier).
     * This method is used by getDestinationRateMultiplier for demonstration purposes.
     *
     * @param n The index of the Fibonacci number to calculate.
     * @return The nth Fibonacci number.
     */
    private double fib(int n) {
        double[] fibCache = new double[n + 2];
        fibCache[0] = 0;
        fibCache[1] = 1;
        for (int i = 2; i <= n; i++) {
            fibCache[i] = fibCache[i - 1] + fibCache[i - 2];
        }
        return fibCache[n];
    }

    /**
     * Generates and displays a travel page with information about various destinations, including weather forecasts and sample quotations.
     * This method iterates through all destinations, randomly generates travel details (days and people), and displays them along with weather forecasts and quotations.
     */
    public void displayTravelPage() {
        long startTime = System.currentTimeMillis();
        Random random = new Random();

        for (String destination : DESTINATIONS) {
            int days = random.nextInt(10) + 1;
            int people = random.nextInt(5) + 1;
            log.info("Destination: {}", destination);
            log.info("Weather Forecast: {}", getWeatherForecast(destination));
            log.info("Sample Quotation for {} people, {} days: ${}", people, days, getQuotation(destination, days, people));
            log.info("---------------------------------------------");
        }

        long endTime = System.currentTimeMillis();
        log.info("Total execution time for displaying travel page: {} ms", (endTime - startTime));
    }

    /**
     * This is the entry point for the application.
     * It creates an instance of the `ImperativeTravelAgency` class and calls its `displayTravelPage` method to generate and display the travel page.
     * It also measures and logs performance metrics like execution time, memory usage, and garbage collection during the travel page generation.
     *
     * @param args Command-line arguments (not used in this application).
     */
    public static void main(String[] args) {
        logPerformanceMetrics(() -> {
            ImperativeTravelAgency agency = new ImperativeTravelAgency();
            agency.displayTravelPage();
        });
    }

    /**
     * Measures and logs performance metrics for the execution of the provided operation.
     * This method captures metrics like execution time, memory usage, garbage collection activity, thread count, and JVM uptime.
     * It can be used to analyze the resource usage and efficiency of any operation within the application.
     *
     * @param operation The operation to run and measure performance for.
     */
    private static void logPerformanceMetrics(Runnable operation) {
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        long totalGcDuration = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
        long startTime = System.nanoTime();
        Runtime runtime = Runtime.getRuntime();
        long usedMemoryBefore = runtime.totalMemory() - runtime.freeMemory();

        operation.run();

        long usedMemoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long totalGcDurationAfter = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
        int threadCountAfter = ManagementFactory.getThreadMXBean().getThreadCount();
        long duration = (System.nanoTime() - startTime) / 1_000_000;
        MemoryUsage heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();

        log.info("Execution time: {} ms", duration);
        log.info("Used memory before: {} bytes", usedMemoryBefore);
        log.info("Used memory after: {} bytes", usedMemoryAfter);
        log.info("Memory used by operation: {} bytes", (usedMemoryAfter - usedMemoryBefore));
        log.info("Total garbage collection time: {} ms", (totalGcDurationAfter - totalGcDuration));
        log.info("Active thread count after operation: {}", threadCountAfter);
        log.info("JVM uptime: {} ms", ManagementFactory.getRuntimeMXBean().getUptime());
        log.info("Heap memory usage: {}", heapMemoryUsage);
        log.info("Non-heap memory usage: {}", nonHeapMemoryUsage);
    }
}
