package org.doksanbir;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParallelTravelAgencyTest {


    // Another happy path is to test that the 'calculateQuotation' method correctly calculates the quotation for a given destination, number of days, and number of people.
    @Test
    public void test_calculate_quotation_correctly_calculates_quotation() {
        // Given
        String destination = "Destination_1";
        int days = 5;
        int people = 3;
        double expectedQuotation = ParallelTravelAgency.BASE_RATE * ParallelTravelAgency.getDestinationRateMultiplier(destination) * days * people;

        // When
        double actualQuotation = ParallelTravelAgency.calculateQuotation(destination, days, people);

        // Then
        assertEquals(expectedQuotation, actualQuotation, 0.001);
    }

    // A happy path is to test that the 'getDestinationRateMultiplier' method correctly calculates the rate multiplier for a given destination.
    @Test
    public void test_get_destination_rate_multiplier_correctly_calculates_rate_multiplier() {
        // Given
        String destination = "Destination_1";
        double expectedRateMultiplier = ParallelTravelAgency.fib(Math.abs(destination.hashCode()) % 30);

        // When
        double actualRateMultiplier = ParallelTravelAgency.getDestinationRateMultiplier(destination);

        // Then
        assertEquals(expectedRateMultiplier, actualRateMultiplier, 0.001);
    }

    // A happy path is to test that the 'getWeatherForecast' method correctly retrieves the weather forecast for a given destination.
    @Test
    public void test_get_weather_forecast_correctly_retrieves_weather_forecast() {
        // Given
        String destination = "Destination_1";

        // When
        String weatherForecast = ParallelTravelAgency.getWeatherForecast(destination);

        // Then
        assertTrue(Arrays.asList(ParallelTravelAgency.WEATHER_CONDITIONS).contains(weatherForecast));
    }

    // A happy path is to test that the 'simulateNetworkCall' method correctly simulates a network call.
    @Test
    public void test_simulate_network_call_correctly_simulates_network_call() {
        // Given
        long expectedSleepTime = 200;

        // When
        long startTime = System.currentTimeMillis();
        ParallelTravelAgency.simulateNetworkCall();
        long endTime = System.currentTimeMillis();
        long actualSleepTime = endTime - startTime;

        // Then
        assertTrue(actualSleepTime >= expectedSleepTime);
    }

    // An edge case to test is when the number of days is 0, to ensure that the 'calculateQuotation' method returns 0.
    @Test
    public void test_calculate_quotation_with_zero_days_returns_zero() {
        // Given
        String destination = "Destination_1";
        int days = 0;
        int people = 3;

        // When
        double quotation = ParallelTravelAgency.calculateQuotation(destination, days, people);

        // Then
        assertEquals(0, quotation, 0.001);
    }

}
