package org.doksanbir;


import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.*;

class AdvancedConcurrentTravelAgencyTest {

    private AdvancedConcurrentTravelAgency agency;
    private Method getDestinationRateMultiplierMethod;
    private Method fibMethod;

    private Method fetchWeatherForecastMethod;

    @BeforeEach
    void setUp() throws Exception {
        agency = new AdvancedConcurrentTravelAgency();

        // Access private methods for testing
        getDestinationRateMultiplierMethod = AdvancedConcurrentTravelAgency.class.getDeclaredMethod("getDestinationRateMultiplier", String.class);
        getDestinationRateMultiplierMethod.setAccessible(true);

        fibMethod = AdvancedConcurrentTravelAgency.class.getDeclaredMethod("fib", int.class);
        fibMethod.setAccessible(true);

        fetchWeatherForecastMethod = AdvancedConcurrentTravelAgency.class.getDeclaredMethod("fetchWeatherForecast", String.class);
        fetchWeatherForecastMethod.setAccessible(true);
    }

    @Test
    void testGetQuotation() throws Exception {
        String destination = "Destination_1";
        int days = 5;
        int people = 2;

        // Calculate expected quotation using reflection
        double rateMultiplier = (Double) getDestinationRateMultiplierMethod.invoke(agency, destination);
        double expectedQuotation = 100.0 * (1 + rateMultiplier) * days * people;

        // Actual quotation
        double actualQuotation = agency.getQuotation(destination, days, people);

        assertEquals(expectedQuotation, actualQuotation, "Quotation calculation is incorrect");
    }

    @Test
    void testGetQuotationAsync() throws ExecutionException, InterruptedException {
        String destination = "Destination_1";
        int days = 3;
        int people = 2;

        Future<Double> futureQuotation = agency.getQuotationAsync(destination, days, people);
        assertNotNull(futureQuotation, "Future object should not be null");

        double quotation = futureQuotation.get();
        assertTrue(quotation > 0, "Async quotation should be greater than 0");
    }

    @Test
    void testGetWeatherForecast() throws InterruptedException {
        String destination = "Destination_1";
        String weather = agency.getWeatherForecast(destination);

        assertNotNull(weather, "Weather forecast should not be null");
        assertTrue(Arrays.asList(AdvancedConcurrentTravelAgency.WEATHER_CONDITIONS).contains(weather), "Weather forecast should be one of the predefined conditions");
    }

    @Test
    void testFetchWeatherForecast() throws Exception {
        String destination = "Destination_1";

        String weather = (String) fetchWeatherForecastMethod.invoke(agency, destination);
        assertTrue(Arrays.asList(AdvancedConcurrentTravelAgency.WEATHER_CONDITIONS).contains(weather), "Weather forecast should be one of the predefined conditions");
    }

    @Test
    void testDisplayTravelPage() throws InterruptedException {
        // This is an integration test; it may need to verify the logs or the overall behavior of displayTravelPage.
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            try {
                agency.displayTravelPage();
            } catch (InterruptedException e) {
                fail("Should not interrupt");
            } finally {
                latch.countDown();
            }
        }).start();

        assertTrue(latch.await(30, TimeUnit.SECONDS), "displayTravelPage did not complete in time");
    }

    @Test
    void testFibonacciCalculation() throws Exception {
        // Test for a known Fibonacci number
        int n = 10; // Known Fibonacci index
        double expectedFibNumber = 55; // Known Fibonacci number for index 10
        double actualFibNumber = (Double) fibMethod.invoke(agency, n);
        assertEquals(expectedFibNumber, actualFibNumber, "Fibonacci calculation is incorrect");
    }

    @Test
    void testDestinationRateMultiplier() throws Exception {
        String destination = "Destination_1";
        double actualMultiplier = (Double) getDestinationRateMultiplierMethod.invoke(agency, destination);
        assertTrue(actualMultiplier >= 0, "Destination rate multiplier should be non-negative");
    }


    @Test
    void testSemaphoreUsageInWeatherForecast() throws InterruptedException {
        String destination = "Destination_1";
        ExecutorService executor = Executors.newFixedThreadPool(20);
        for (int i = 0; i < 20; i++) {
            executor.submit(() -> {
                try {
                    agency.getWeatherForecast(destination);
                } catch (InterruptedException e) {
                    fail("Weather forecast retrieval should not be interrupted");
                }
            });
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(1, TimeUnit.MINUTES), "All weather forecast tasks should complete");
    }

}

