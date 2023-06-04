package asia.pacific.airport.simulation.system;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AsiaPacificAirportSimulationSystem {
    final static int TOTAL_PLANES = 6;

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        ATC atc = new ATC();
        FuelDepot fuelDepot = new FuelDepot();
        RefuelTruck refuelTruck = new RefuelTruck(atc, fuelDepot);

        ExecutorService executorService = Executors.newFixedThreadPool(TOTAL_PLANES + 1);

        executorService.execute(refuelTruck);

        for (int i = 0; i < TOTAL_PLANES; i++) {
            boolean isEmergency = i == TOTAL_PLANES - 1;
            Airplane airplane = new Airplane(atc, refuelTruck);
            executorService.execute(airplane);
            try {
                Thread.sleep(new Random().nextInt(3000));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        executorService.shutdown();

        try {
            boolean terminated = executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            if (terminated) {
                long endTime = System.currentTimeMillis();
                long operatingTime = (endTime - startTime) / 1000 ;
                System.out.println("\nAll tasks completed. Asia Pacific Airport shuts down.");
                System.out.println("Total operating time: " + operatingTime + " seconds");
            } else {
                System.out.println("\nTimeout occurred while waiting for tasks to complete.");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
