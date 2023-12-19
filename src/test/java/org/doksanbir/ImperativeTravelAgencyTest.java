package org.doksanbir;

import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.util.Arrays;

public class ImperativeTravelAgencyTest extends TestCase {

    private ImperativeTravelAgency agency;
    private Method getDestinationRateMultiplierMethod;
    private Method fibMethod;

    public void setUp() throws Exception {
        super.setUp();
        agency = new ImperativeTravelAgency();

        // Reflection to access private methods
        getDestinationRateMultiplierMethod = ImperativeTravelAgency.class.getDeclaredMethod("getDestinationRateMultiplier", String.class);
        getDestinationRateMultiplierMethod.setAccessible(true);

        fibMethod = ImperativeTravelAgency.class.getDeclaredMethod("fib", int.class);
        fibMethod.setAccessible(true);
    }

    public void testGetQuotation() throws Exception {
        String destination = "Destination_1";
        int days = 5;
        int people = 2;

        // Calculate expected quotation using reflection
        double rateMultiplier = (Double) getDestinationRateMultiplierMethod.invoke(agency, destination);
        double expectedQuotation = 100.0 * (1 + rateMultiplier) * days * people;
        double actualQuotation = agency.getQuotation(destination, days, people);

        assertEquals("Quotation calculation is incorrect", expectedQuotation, actualQuotation);
    }

    public void testGetWeatherForecast() {
        String destination = "Destination_1";
        String weather = agency.getWeatherForecast(destination);

        assertTrue("Returned weather condition is not valid",
                Arrays.asList(ImperativeTravelAgency.WEATHER_CONDITIONS).contains(weather));
    }

    public void testDisplayTravelPage() {
        // Testing displayTravelPage is still challenging due to its randomness and logging
        // We'll focus on ensuring it completes execution without exceptions
        try {
            agency.displayTravelPage();
        } catch (Exception e) {
            fail("displayTravelPage should not throw exceptions");
        }
    }

    public void testMain() {
        // Similar to displayTravelPage, test that it runs without exceptions
        try {
            ImperativeTravelAgency.main(new String[]{});
        } catch (Exception e) {
            fail("Main method should not throw exceptions");
        }
    }
}
