package asia.pacific.airport.simulation.system;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AsiaPacificAirportSimulationSystem {
    final static int TOTAL_PLANES = 6;

    public static void main(String[] args) {
        ATC atc = new ATC();

        ExecutorService executorService = Executors.newFixedThreadPool(TOTAL_PLANES);
        for (int i = 0; i < TOTAL_PLANES; i++) {
            boolean isEmergency = i == TOTAL_PLANES - 1;
            Airplane airplane = new Airplane(atc, isEmergency);
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
                System.out.println("All tasks completed. ServiceExecutor shut down.");
            } else {
                System.out.println("Timeout occurred while waiting for tasks to complete.");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
