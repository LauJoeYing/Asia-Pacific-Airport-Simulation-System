package asia.pacific.airport.simulation.system;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static asia.pacific.airport.simulation.system.AsiaPacificAirportSimulationSystem.TOTAL_PLANES;

public class ATC implements Logging {
    private final static String ATC = "ATC";
    private final Lock runwayLock;
    private final GateHandler gateHandler;
    private final PriorityBlockingQueue<AirplaneActivity> airplaneActivityQueue;
    private AtomicInteger totalAirplaneCycleCount;
    private AtomicInteger totalPassengerCycleCount;
    private AtomicLongArray waitingTimeList;

    public ATC() {
        runwayLock = new ReentrantLock(true);
        gateHandler = new GateHandler();
        airplaneActivityQueue = new PriorityBlockingQueue<>();
        totalAirplaneCycleCount = new AtomicInteger(0);
        totalPassengerCycleCount = new AtomicInteger(0);
        waitingTimeList = new AtomicLongArray(0);
    }

    private void enqueueActivity(Airplane airplane, AirplaneAction action) {
        airplaneActivityQueue.offer(new AirplaneActivity(airplane, action));

        logCurrentActivityQueues();

        String enqueueActivityLoggingMessage = String.format(
                "%s (%s) %s activity queue.",
                airplane.getName(),
                action.equals(AirplaneAction.LAND) ?
                        "landing" :
                        "take off",
                airplane.isEmergency() && airplaneActivityQueue.size() > 1 ?
                        "cuts queue and has been offered first place in the" :
                        "has been added to the"
        );
        log(enqueueActivityLoggingMessage);
    }

    private void dequeueActivity() {
        AirplaneActivity nextAirplaneActivity;
        Airplane airplaneToRun;
        AirplaneAction action;

        try {
            nextAirplaneActivity = airplaneActivityQueue.take();
            airplaneToRun = nextAirplaneActivity.getAirplane();
            action = nextAirplaneActivity.getAction();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        logCurrentActivityQueues();

        String dequeueLandingLoggingMessage = String.format(
                "%s is ready to be %s.",
                airplaneToRun.getName(),
                action.equals(AirplaneAction.LAND) ?
                        "landed" :
                        "taken off"
        );

        log(dequeueLandingLoggingMessage);

        runwayLock.lock();
        if (action.equals(AirplaneAction.LAND)) {
            sendLandingApproval(airplaneToRun);
        } else {
            sendTakeOffApproval(airplaneToRun);
        }
    }

    private boolean pendingActivityPresent() {
        return airplaneActivityQueue.size() > 0;
    }

    private boolean nextActivityIsLanding() {
        return airplaneActivityQueue.size() > 0 && airplaneActivityQueue.peek().getAction().equals(AirplaneAction.LAND);
    }

    public void handleLandingRequest(Airplane airplane) {
        String landingRequestLoggingMessage = String.format(
                "%s %slanding request received. Checking for gate availability.",
                airplane.getName(),
                airplane.getEmergencyStatus()
        );
        log(landingRequestLoggingMessage);

        if (gateHandler.gateIsSufficient()) {
            if (pendingActivityPresent()) {
                log("Pending activity is present, please wait in a circle queue.");
            } else {
                sendLandingApproval(airplane);
                return;
            }
        } else {
            log("All gates are occupied at the moment, please wait in a circle queue.");
        }

        enqueueActivity(airplane, AirplaneAction.LAND);
    }

    private void sendLandingApproval(Airplane airplane) {
        if (runwayLock.tryLock()) {
            int gateId = gateHandler.acquireGate(airplane);
            String landingApprovalLoggingMessage = String.format(
                    "%s landing approval granted. Please proceed to gate %d.",
                    airplane.getName(),
                    gateId
            );
            log(landingApprovalLoggingMessage);
            log("Runway is now locked for landing.");
            airplane.handleLandingApproval();
        } else {
            log("Runway is occupied at the moment, please wait in a circle queue.");
            enqueueActivity(airplane, AirplaneAction.LAND);
        }
    }

    public void handleTakeOffRequest(Airplane airplane) {
        String takeOffRequestLoggingMessage = String.format(
                "%s take off request received.",
                airplane.getName()
        );
        log(takeOffRequestLoggingMessage);

        if (!gateHandler.gateIsFull() && nextActivityIsLanding()) {
            airplane.setEmergency(true);
        } else if (pendingActivityPresent()) {
            log("Pending activity is present, please wait at the gate.");
            enqueueActivity(airplane, AirplaneAction.TAKE_OFF);
            return;
        }

        sendTakeOffApproval(airplane);
    }

    private void sendTakeOffApproval(Airplane airplane) {
        if (runwayLock.tryLock()) {
            String takeOffApprovalLoggingMessage = String.format(
                    "%s take off approval granted. Please proceed to runway.",
                    airplane.getName()
            );
            log(takeOffApprovalLoggingMessage);
            gateHandler.releaseGate(airplane);
            log("Runway is now locked for take off.");
            airplane.handleTakeOffApproval();
        } else {
            log("Runway is occupied at the moment, please wait at the gate.");
            enqueueActivity(airplane, AirplaneAction.TAKE_OFF);
        }
    }

    public void handlePostTrafficActivity() {
        runwayLock.unlock();
        log("Runway is now available.");
        if (airplaneActivityQueue.size() > 0) {
            dequeueActivity();
        }
    }

    private void logCurrentActivityQueues() {
        String activityQueueLoggingMessage = String.format(
                "Airplane Activity queue: [%s]",
                airplaneActivityQueue
        );
        log(activityQueueLoggingMessage);
    }

    public void passengerIncrement() {
        totalPassengerCycleCount.getAndIncrement();
    }

    public void airplaneIncrement(){
        totalAirplaneCycleCount.getAndIncrement();
        if (totalAirplaneCycleCount.get() == TOTAL_PLANES){
            sanityCheck();
            statistics();
        }
    }

    public void addWaitingTime(long waitingTime){
        int index = waitingTimeList.length();
        waitingTimeList.set(index, waitingTime);
    }

    private void printWaitingTimeStatistics(){
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        long sum = 0;
        for (int i = 0; i < waitingTimeList.length(); i++) {
            long waitingTime = waitingTimeList.get(i);
            if (waitingTime < min){
                min = waitingTime;
            }
            if (waitingTime > max){
                max = waitingTime;
            }
            sum += waitingTime;
        }

        System.out.printf("Minimum waiting time\t\t: %.3fs\n", (double) min);
        System.out.printf("Maximum waiting time\t\t: %.3fs\n", (double) max);
        System.out.printf("Average waiting time\t\t: %.3fs\n", (double) sum / waitingTimeList.length());
        System.out.printf("Total waiting time\t\t: %.3fs\n", (double) sum);
    }

    private void sanityCheck(){
        System.out.println("---------------------------");
        System.out.println("\tGATE STATUS");
        System.out.println("---------------------------");
        AtomicReferenceArray<Gate> gates = gateHandler.getGates();
        for (int i = 0; i < gates.length(); i++) {
            Gate gate = gates.get(i);
            System.out.printf("%s is %s.", gate.getName(), gate.isOccupied() ? "occupied." : "empty.");
        }
    }

    private void statistics(){
        System.out.println();
        System.out.println("---------------------------");
        System.out.println("\tSTATISTICS");
        System.out.println("---------------------------");

        printWaitingTimeStatistics();

        System.out.printf("Number of planes served\t\t: %d%n", totalAirplaneCycleCount.get());
        System.out.printf("Number of passengers served\t: %d%n", totalPassengerCycleCount.get());
    }



    @Override
    public void log(String loggingMessage) {
        System.out.printf(
                "%s %s: %s%n",
                AirportTime.getCurrentTimestamp(),
                ATC,
                loggingMessage
        );
    }
}
