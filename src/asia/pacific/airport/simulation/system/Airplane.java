package asia.pacific.airport.simulation.system;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Airplane implements Runnable, Logging, Comparable<Airplane> {
    private static final int MAX_PASSENGER_COUNT = 50;
    private static final int MIN_PASSENGER_COUNT = 15;
    private static int airplaneCount = 0;
    private final int id;
    private final ATC atc;
    private final RefuelTruck refuelTruck;
    private AirplaneActivity currentActivity;
    private boolean isRefueled;
    private final Object refuelingLock;
    private int passengerCount;
    private CountDownLatch boardingLatch;
    private final AtomicInteger passengerCompleted;
    private final Random random;

    public Airplane(ATC atc, RefuelTruck refuelTruck) {
        airplaneCount++;
        id = airplaneCount;
        this.atc = atc;
        this.refuelTruck = refuelTruck;
        isRefueled = false;
        refuelingLock = new Object();
        random = new Random();
        passengerCount = random.nextInt(MAX_PASSENGER_COUNT - MIN_PASSENGER_COUNT + 1) + MIN_PASSENGER_COUNT;
        boardingLatch = new CountDownLatch(passengerCount);
        passengerCompleted = new AtomicInteger(0);
    }

    public String getName() {
        return String.format("Airplane %d", this.id);
    }

    public AirplaneActivity getCurrentActivity() {
        return currentActivity;
    }

    public void setActivityEmergency(boolean isEmergency) {
        currentActivity.setEmergency(isEmergency);
    }

    public void setActivityApprovalGranted(boolean isApprovalGranted) {
        synchronized (currentActivity.getActionApprovalLock()) {
            currentActivity.setActionApprovalGranted(isApprovalGranted);
            currentActivity.getActionApprovalLock().notifyAll();
        }
    }

    public void setActivityCompletion(boolean isCompleted) {
        synchronized (currentActivity.getActionCompletionLock()) {
            currentActivity.setActionCompleted(isCompleted);
            currentActivity.getActionCompletionLock().notifyAll();
        }
    }

    public String getCurrentActivityName() {
        return String.format(
                "%s (%s)",
                getName(),
                currentActivity.getName()
        );
    }

    public boolean isRefueled() {
        synchronized (refuelingLock) {
            return isRefueled;
        }
    }

    public void setRefueled(boolean isRefueled) {
        synchronized (refuelingLock) {
            this.isRefueled = isRefueled;
            refuelingLock.notifyAll();
        }
    }

    private void waitForRefueling() {
        synchronized (refuelingLock) {
            while (!isRefueled) {
                try {
                    refuelingLock.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        log("Refueled.");
    }

    private void requestToLand() {
        currentActivity = new AirplaneActivity(AirplaneAction.LANDING, airplaneCount == AsiaPacificAirportSimulationSystem.TOTAL_PLANES);
        String requestToLandLoggingMessage = String.format(
                "Request for %slanding.",
                currentActivity.isEmergency() ? "emergency " : ""
        );
        log(requestToLandLoggingMessage);

        atc.handleLandingRequest(this);
    }

    private void land() {
        currentActivity.waitForActionRequestApproval();

        atc.addWaitingTime(System.currentTimeMillis() - currentActivity.getActionRequestTime());

        atc.handlePreTrafficActivity(this);
        log("Landing approval received.");
        log("Landing on runway.");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log("Landed successfully.");

        setActivityCompletion(true);
        atc.handlePostTrafficActivity(this);
    }

    private void dock() {
        log("Docking to the gate assigned.");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log("Docked successfully.");
    }

    private void requestToRefuel() {
        log("Request to refuel.");
        refuelTruck.enqueueAirplane(this);
    }

    private void clean() {
        log("Cabin Crew is cleaning the airplane.");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log("Cabin Crew has finished cleaning the airplane.");
    }

    private void refillSupplies() {
        log("Cabin Crew is refilling airplane supplies.");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log("Cabin Crew has finished refilling supplies.");
    }

    private void postDockingActivity() {
        Thread requestToRefuelThread = new Thread(this::requestToRefuel);
        Thread disembarkingThread = new Thread(this::disembarkPassenger);
        Thread cleaningThread = new Thread(this::clean);
        Thread refillingThread = new Thread(this::refillSupplies);
        Thread embarkingThread = new Thread(this::embarkPassenger);

        requestToRefuelThread.start();
        disembarkingThread.start();

        try {
            disembarkingThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        cleaningThread.start();
        refillingThread.start();

        try {
            requestToRefuelThread.join();
            cleaningThread.join();
            refillingThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        embarkingThread.start();

        try {
            embarkingThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void embarkPassenger() {
        passengerCount = random.nextInt(MAX_PASSENGER_COUNT - MIN_PASSENGER_COUNT + 1) + MIN_PASSENGER_COUNT;
        boardingLatch = new CountDownLatch(passengerCount);
        boardPassenger(BoardType.EMBARK);
    }

    private void disembarkPassenger() {
        boardPassenger(BoardType.DISEMBARK);
    }

    private void boardPassenger(BoardType boardType) {
        String allBoardingLoggingMessage = String.format(
                "%d passengers are %s the airplane.",
                passengerCount,
                boardType == BoardType.EMBARK ? "boarding" : "disembarking"
        );
        log(allBoardingLoggingMessage);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        for (int i = 1; i <= passengerCount; i++) {
            int passengerNumber = i;
            executor.schedule(() -> {
                boardingLatch.countDown();
                String boardingLoggingMessage = String.format(
                        "Passenger %d is %s the airplane. [%d/%d]",
                        passengerNumber,
                        boardType == BoardType.EMBARK ? "embarking" : "disembarking",
                        passengerCompleted.incrementAndGet(),
                        passengerCount
                );
                log(boardingLoggingMessage);

                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                atc.passengerIncrement();
            }, i * 300L, TimeUnit.MILLISECONDS);
        }

        try {
            boardingLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        passengerCompleted.set(0);
        executor.shutdown();
        String allBoardingCompletionLoggingMessage = String.format(
                "All %d passengers have %s the airplane.",
                passengerCount,
                boardType == BoardType.EMBARK ? "embarked" : "disembarked"
        );
        log(allBoardingCompletionLoggingMessage);
    }

    private void requestToTakeOff() {
        waitForRefueling();
        currentActivity = new AirplaneActivity(AirplaneAction.TAKE_OFF);
        log("Request for take off.");
        atc.handleTakeOffRequest(this);
    }

    private void takeOff() {
        currentActivity.waitForActionRequestApproval();

        atc.handlePreTrafficActivity(this);
        log("Take off approval received.");

        log("Taking off.");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log("Took off successfully.");

        setActivityCompletion(true);
        atc.handlePostTrafficActivity(this);
        atc.airplaneIncrement();
    }

    @Override
    public void log(String loggingMessage) {
        System.out.printf(
                "%s %s: %s%n",
                AirportTime.getCurrentTimestamp(),
                getName(),
                loggingMessage
        );
    }

    @Override
    public void run() {
        requestToLand();
        land();
        dock();
        postDockingActivity();
        requestToTakeOff();
        takeOff();
    }

    @Override
    public int compareTo(Airplane other) {
        if (atc.getGateHandler().gateIsFull() && atc.nextActivityIsTakeOff()) {
            if (this.currentActivity.getAction().equals(AirplaneAction.TAKE_OFF)) {
                return -1;
            } else if (other.currentActivity.getAction().equals(AirplaneAction.TAKE_OFF)) {
                return 1;
            } else {
                return this.currentActivity.compareTo(other.currentActivity);
            }
        } else {
            return this.currentActivity.compareTo(other.currentActivity);
        }
    }
}
