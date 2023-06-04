package asia.pacific.airport.simulation.system;

import static asia.pacific.airport.simulation.system.RefuelTruck.REFUEL_TRUCK_NAME;

public class FuelDepot implements Logging {
    private static final String FUEL_DEPOT_NAME = "Fuel Depot";
    public FuelDepot() {}

    public void refill(RefuelTruck refuelTruck) {
        String refillingLoggingMessage = String.format(
                "Refilling %s.",
                REFUEL_TRUCK_NAME
        );
        log(refillingLoggingMessage);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        String refillCompletionLoggingMessage = String.format(
                "Finished refilling %s.",
                REFUEL_TRUCK_NAME
        );
        log(refillCompletionLoggingMessage);

        refuelTruck.setFuelSufficient(true);
    }

    @Override
    public void log(String loggingMessage) {
        System.out.printf(
                "%s %s: %s%n",
                AirportTime.getCurrentTimestamp(),
                FUEL_DEPOT_NAME,
                loggingMessage
        );
    }
}
