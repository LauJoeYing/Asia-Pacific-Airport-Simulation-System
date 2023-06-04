package asia.pacific.airport.simulation.system;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

public class RefuelTruck implements Logging, Runnable {
    public static String REFUEL_TRUCK_NAME = "Refuel Truck";
    private final ATC atc;
    private final FuelDepot fuelDepot;
    private final BlockingQueue<Airplane> pendingRefuelQueue;
    private boolean isFuelSufficient;
    private final Object refillLock;
    private int refuelCount;
    private final Semaphore queueSemaphore;

    public RefuelTruck(ATC atc, FuelDepot fuelDepot) {
        this.atc = atc;
        this.fuelDepot = fuelDepot;
        pendingRefuelQueue = new ArrayBlockingQueue<>(GateHandler.GATE_CAPACITY);
        this.isFuelSufficient = true;
        this.refillLock = new Object();
        this.refuelCount = 0;
        this.queueSemaphore = new Semaphore(0);
    }

    public boolean isFuelSufficient() {
        synchronized (refillLock) {
            return isFuelSufficient;
        }
    }

    public void setFuelSufficient(boolean isFuelSufficient) {
        synchronized (refillLock) {
            this.isFuelSufficient = isFuelSufficient;
        }
    }

    public void waitForRefill() {
        synchronized (refillLock) {
            try {
                while (!isFuelSufficient) {
                    refillLock.wait();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void selfRefill() {
        log("Return to fuel depot for refilling.");
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        fuelDepot.refill(this);
        waitForRefill();
        log("Ready to refuel airplanes.");
    }

    public void enqueueAirplane(Airplane airplane) {
        try {
            pendingRefuelQueue.put(airplane);
            queueSemaphore.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void dequeueAirplane() {
        try {
            queueSemaphore.acquire();
            refuel(pendingRefuelQueue.take());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void refuel(Airplane airplane) {
        String refuelingLoggingMessage = String.format(
                "Refueling %s.",
                airplane.getName()
        );
        log(refuelingLoggingMessage);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        String refuelCompletionLoggingMessage = String.format(
                "Finished refuelling %s.",
                airplane.getName()
        );
        log(refuelCompletionLoggingMessage);
        refuelCount++;

        airplane.setRefueled(true);

        int MAXIMUM_REFUEL_COUNT = 2;
        if (refuelCount == MAXIMUM_REFUEL_COUNT) {
            log("Running low on fuel.");
            selfRefill();
            refuelCount = 0;
        }
    }

    @Override
    public void log(String loggingMessage) {
        System.out.printf(
                "%s %s: %s%n",
                AirportTime.getCurrentTimestamp(),
                REFUEL_TRUCK_NAME,
                loggingMessage
        );
    }

    @Override
    public void run() {
        while (atc.getTotalAirplaneCycleCount() != AsiaPacificAirportSimulationSystem.TOTAL_PLANES) {
            if (!pendingRefuelQueue.isEmpty()) {
                dequeueAirplane();
            }
        }
    }
}
