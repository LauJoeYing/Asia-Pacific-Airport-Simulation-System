package asia.pacific.airport.simulation.system;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ATC implements Logging {
    private final static String ATC = "ATC";
    private final Lock runwayLock;
    private final GateHandler gateHandler;
    private final PriorityBlockingQueue<AirplaneActivity> airplaneActivityQueue;

    public ATC() {
        runwayLock = new ReentrantLock(true);
        gateHandler = new GateHandler();
        airplaneActivityQueue = new PriorityBlockingQueue<>();
    }

    private void enqueueActivity(Airplane airplane, String activity) {
        airplaneActivityQueue.offer(new AirplaneActivity(airplane, activity));

        logCurrentActivityQueues();

        String enqueueActivityLoggingMessage = String.format(
                "%s (%s) %s activity queue.",
                airplane.getName(),
                activity.equals("LAND") ?
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
        String activity;

        try {
            nextAirplaneActivity = airplaneActivityQueue.take();
            airplaneToRun = nextAirplaneActivity.getAirplane();
            activity = nextAirplaneActivity.getActivity();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        logCurrentActivityQueues();

        String dequeueLandingLoggingMessage = String.format(
                "%s is ready to be %s.",
                airplaneToRun.getName(),
                activity.equals("LAND") ?
                        "landed" :
                        "taken off"
        );

        log(dequeueLandingLoggingMessage);

        runwayLock.lock();
        if (activity.equals("LAND")) {
            sendLandingApproval(airplaneToRun);
        } else {
            sendTakeOffApproval(airplaneToRun);
        }
    }

    private boolean pendingActivityPresent() {
        return airplaneActivityQueue.size() > 0;
    }

    private boolean nextActivityIsLanding() {
        return airplaneActivityQueue.size() > 0 && airplaneActivityQueue.peek().getActivity().equals("LAND");
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

        enqueueActivity(airplane, "LAND");
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
            enqueueActivity(airplane, "LAND");
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
            enqueueActivity(airplane, "TAKE OFF");
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
            enqueueActivity(airplane, "TAKE OFF");
        }
    }

    public void handlePostActivity() {
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
