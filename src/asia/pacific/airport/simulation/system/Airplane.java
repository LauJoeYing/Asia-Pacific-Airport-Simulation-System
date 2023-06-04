package asia.pacific.airport.simulation.system;

public class Airplane implements Runnable, Logging, Comparable<Airplane> {
    private static int airplaneCount = 0;
    private final int id;
    private final ATC atc;
    private AirplaneActivity currentActivity;

    public Airplane(ATC atc) {
        airplaneCount++;
        id = airplaneCount;
        this.atc = atc;
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

    private void requestToTakeOff() {
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
        requestToTakeOff();
        takeOff();
    }

    @Override
    public int compareTo(Airplane other) {
        return this.currentActivity.compareTo(other.currentActivity);
    }
}
