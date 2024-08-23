package org.doksanbir;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AdvancedConcurrentTravelAgency simulates a travel agency system that handles multiple travel destinations concurrently.
 * It provides functionality to get weather forecasts and travel quotations for various destinations.
 * Problem Statement:
 * Design and implement a system that can efficiently handle multiple travel destinations,
 * providing weather forecasts and price quotations concurrently. The system should be able to:
 * 1. Manage a list of travel destinations
 * 2. Provide weather forecasts for destinations
 * 3. Calculate price quotations based on destination, duration, and number of travelers
 * 4. Handle these operations concurrently for multiple destinations
 * 5. Provide performance metrics for the operations
 * This class uses various concurrent programming techniques to achieve these goals efficiently.
 */
@Slf4j
public class AdvancedConcurrentTravelAgency {

    /**
     * The number of travel destinations available.
     * This determines the size of the destination list.
     */
    private static final int DESTINATION_COUNT = 50;

    /**
     * The base price per day for travel.
     * This is used as a starting point for quotation calculations.
     */
    private static final double BASE_RATE = 100.0;

    /**
     * The number of permits for the weather forecast semaphore.
     * This limits the number of concurrent weather forecast operations.
     */
    private static final int WEATHER_SEMAPHORE_PERMITS = 10;

    /**
     * The delay in milliseconds used to simulate network calls.
     * This adds realism to the weather forecast fetching operation.
     */
    private static final int NETWORK_CALL_DELAY = 200;

    /**
     * A map containing the weather forecast for each destination.
     * This cache helps avoid redundant weather forecast fetches.
     */
    private static final ConcurrentMap<String, String> destinationWeather = new ConcurrentHashMap<>();

    /**
     * A list containing names of all travel destinations.
     * This list is thread-safe and allows for concurrent access.
     */
    private static final CopyOnWriteArrayList<String> destinations = new CopyOnWriteArrayList<>();

    /**
     * A semaphore to limit the number of concurrent weather forecast requests.
     * This prevents overloading of the simulated weather service.
     */
    private static final Semaphore weatherSemaphore = new Semaphore(WEATHER_SEMAPHORE_PERMITS);

    /**
     * A thread pool to process quotation and weather forecast requests.
     * This allows for efficient handling of multiple concurrent operations.
     */
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * A counter to keep track of the number of quotations processed.
     * This is used for performance metrics.
     */
    private final AtomicInteger quotationCounter = new AtomicInteger();

    /**
     * A Random object used for generating random values.
     */
    private static final Random random = new Random();

    /**
     * An array containing possible weather conditions.
     * This is used to generate random weather forecasts.
     */
    private static final String[] WEATHER_CONDITIONS = {
            "Sunny", "Cloudy", "Rainy", "Stormy", "Snowy", "Windy", "Foggy", "Icy"
    };

    /**
     * Initializes the list of destinations.
     * This static block populates the destinations list when the class is loaded.
     */
    static {
        for (int i = 0; i < DESTINATION_COUNT; i++) {
            destinations.add("Destination_" + (i + 1));
        }
    }

    /**
     * Calculates a quotation for a trip to the specified destination.
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

    /**
     * Calculates a rate multiplier for a given destination.
     * This method uses a Fibonacci sequence to add variability to the rates.
     *
     * @param destination The name of the travel destination.
     * @return A rate multiplier based on the destination's hash code.
     */
    private double getDestinationRateMultiplier(String destination) {
        log.info("Calculating destination rate multiplier for destination: {}", destination);
        return fibonacci(Math.abs(destination.hashCode()) % 30);
    }

    /**
     * Calculates the nth Fibonacci number iteratively.
     *
     * @param n The index of the Fibonacci number to calculate.
     * @return The nth Fibonacci number.
     */
    private double fibonacci(int n) {
        if (n <= 1) return n;
        double[] fib = new double[n + 1];
        fib[0] = 0;
        fib[1] = 1;
        for (int i = 2; i <= n; i++) {
            fib[i] = fib[i - 1] + fib[i - 2];
        }
        return fib[n];
    }

    /**
     * Asynchronously calculates a quotation for a trip.
     *
     * @param destination The name of the travel destination.
     * @param days        The number of days for the trip.
     * @param people      The number of people traveling.
     * @return A CompletableFuture that will contain the calculated quotation.
     */
    public CompletableFuture<Double> getQuotationAsync(String destination, int days, int people) {
        log.info("Asynchronously fetching quotation for destination: {}", destination);
        return CompletableFuture.supplyAsync(() -> {
            double quotation = getQuotation(destination, days, people);
            quotationCounter.incrementAndGet();
            log.info("Quotation calculated for destination: {}", destination);
            return quotation;
        }, executorService);
    }

    /**
     * Retrieves the weather forecast for a given destination.
     *
     * @param destination The name of the travel destination.
     * @return The weather forecast for the destination.
     * @throws InterruptedException if the thread is interrupted while waiting for the semaphore.
     */
    public String getWeatherForecast(String destination) throws InterruptedException {
        log.info("Fetching weather forecast for destination: {}", destination);
        return destinationWeather.computeIfAbsent(destination, this::fetchWeatherForecastWithSemaphore);
    }

    /**
     * Fetches the weather forecast for a destination, using a semaphore to limit concurrent requests.
     *
     * @param destination The name of the travel destination.
     * @return The weather forecast for the destination.
     */
    private String fetchWeatherForecastWithSemaphore(String destination) {
        try {
            weatherSemaphore.acquire();
            return fetchWeatherForecast(destination);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread was interrupted while acquiring semaphore", e);
            return "N/A";
        } finally {
            weatherSemaphore.release();
        }
    }

    /**
     * Simulates fetching a weather forecast from an external service.
     *
     * @param destination The name of the travel destination.
     * @return A randomly selected weather condition for the destination.
     */
    private String fetchWeatherForecast(String destination) {
        log.info("Actually fetching weather forecast for destination: {}", destination);
        simulateNetworkCall();
        Random random = new Random(destination.hashCode());
        return WEATHER_CONDITIONS[random.nextInt(WEATHER_CONDITIONS.length)];
    }

    /**
     * Simulates a network call by introducing a delay.
     */
    private void simulateNetworkCall() {
        try {
            Thread.sleep(NETWORK_CALL_DELAY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread was interrupted during simulateNetworkCall", e);
        }
    }

    /**
     * Processes all destinations concurrently, fetching weather forecasts and quotations.
     *
     * @throws InterruptedException if the thread is interrupted while waiting for operations to complete.
     */
    public void displayTravelPage() throws InterruptedException {
        PerformanceMetrics metrics = startPerformanceTracking();

        CompletableFuture<?>[] futures = destinations.stream()
                .map(this::processDestination)
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();
        executorService.shutdown();
        logPerformanceMetrics(metrics);
    }



    /**
     * Processes a single destination, fetching its weather forecast and a quotation.
     *
     * @param destination The name of the travel destination.
     * @return A CompletableFuture representing the completion of the processing.
     */
    private CompletableFuture<Void> processDestination(String destination) {
        int days = random.nextInt(10) + 1;
        int people = random.nextInt(5) + 1;
        return CompletableFuture.runAsync(() -> {
            try {
                String weather = getWeatherForecast(destination);
                double quotation = getQuotationAsync(destination, days, people).get();
                log.info("Destination: {}, Weather: {}, Quotation for {} days, {} people: ${}",
                        destination, weather, days, people, quotation);
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error processing destination: {}", destination, e);
                Thread.currentThread().interrupt();
            }
        }, executorService);
    }

    /**
     * Starts tracking performance metrics for the travel page display operation.
     *
     * @return A PerformanceMetrics object containing initial metrics.
     */
    private PerformanceMetrics startPerformanceTracking() {
        long startTime = System.currentTimeMillis();
        long startMemory = getUsedMemory();
        long startGcDuration = getTotalGCDuration();
        int initialThreadCount = ManagementFactory.getThreadMXBean().getThreadCount();
        return new PerformanceMetrics(startTime, startMemory, startGcDuration, initialThreadCount);
    }

    /**
     * Logs the performance metrics for the travel page display operation.
     *
     * @param metrics The initial performance metrics.
     */
    private void logPerformanceMetrics(PerformanceMetrics metrics) {
        long endTime = System.currentTimeMillis();
        long endMemory = getUsedMemory();
        long endGcDuration = getTotalGCDuration();
        int finalThreadCount = ManagementFactory.getThreadMXBean().getThreadCount();

        log.info("Execution time: {} ms", (endTime - metrics.startTime()));
        log.info("Used memory before: {} bytes", metrics.startMemory());
        log.info("Used memory after: {} bytes", endMemory);
        log.info("Memory used by operation: {} bytes", (endMemory - metrics.startMemory()));
        log.info("Total garbage collection time: {} ms", (endGcDuration - metrics.startGcDuration()));
        log.info("Initial thread count: {}", metrics.initialThreadCount());
        log.info("Final thread count: {}", finalThreadCount);
        log.info("Total quotations processed: {}", quotationCounter.get());
    }

    /**
     * Calculates the amount of memory currently being used by the JVM.
     *
     * @return The amount of used memory in bytes.
     */
    private static long getUsedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    /**
     * Calculates the total duration of garbage collection operations.
     *
     * @return The total garbage collection time in milliseconds.
     */
    private static long getTotalGCDuration() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionTime)
                .sum();
    }

    /**
         * Represents performance metrics for the travel page display operation.
         */
        private record PerformanceMetrics(long startTime, long startMemory, long startGcDuration, int initialThreadCount) {
    }

    /**
     * Main method to run the AdvancedConcurrentTravelAgency simulation.
     *
     * @param args Command line arguments (not used).
     */
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