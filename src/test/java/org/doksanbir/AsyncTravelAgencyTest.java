package org.doksanbir;

import org.junit.jupiter.api.*;
import java.util.Arrays;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class AsyncTravelAgencyTest {

    @Test
    void testCalculateQuotation() {
        String destination = "Destination_1";
        int days = 5;
        int people = 2;
        double quotation = AsyncTravelAgency.calculateQuotation(destination, days, people);
        assertTrue(quotation > 0, "Quotation should be positive");
    }

    @Test
    void testGetWeatherForecast() {
        String destination = "Destination_1";
        String weather = AsyncTravelAgency.getWeatherForecast(destination);
        assertTrue(Arrays.asList(AsyncTravelAgency.WEATHER_CONDITIONS).contains(weather), "Weather forecast should be valid");
    }

    @Test
    void testProcessDestination()  {
        int destinationIndex = 0;
        CompletableFuture<Void> task = AsyncTravelAgency.processDestination(destinationIndex);
        assertDoesNotThrow(() -> task.get(), "Destination processing should complete without exception");
    }

    @Test
    void testPerformanceMetricsLogging() {
        AsyncTravelAgency.PerformanceMetrics metrics = AsyncTravelAgency.startPerformanceTracking();
        assertNotNull(metrics, "Performance metrics should not be null");
    }

    @Test
    void testMainMethodExecution() {
        assertDoesNotThrow(() -> AsyncTravelAgency.main(new String[]{}), "Main method should execute without throwing exceptions");
    }
}
