package asia.pacific.airport.simulation.system;

public class Airplane implements Runnable, Logging {
    private static int planeCount = 0;
    private final int id;
    private final ATC atc;
    private boolean isEmergency;

    public Airplane(ATC atc, boolean isEmergency) {
        planeCount++;
        id = planeCount;
        this.atc = atc;
        this.isEmergency = isEmergency;
    }

    public String getName() {
        return String.format("Airplane %d", this.id);
    }

    private void requestToLand() {
        String requestToLandLoggingMessage = String.format(
                "Request for %slanding.",
                getEmergencyStatus()
        );
        log(requestToLandLoggingMessage);

        atc.handleLandingRequest(this);
    }

    public void handleLandingApproval() {
        log("Landing approval received.");
        land();
    }

    private void land() {
        log("Landing on runway.");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log("Landed successfully.");

        atc.handlePostTrafficActivity();
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
        log("Request for take off.");
        atc.handleTakeOffRequest(this);
    }

    public void handleTakeOffApproval() {
        log("Take off approval received.");
        takeOff();
    }

    private void takeOff() {
        log("Taking off.");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log("Took off successfully.");

        atc.handlePostTrafficActivity();
    }

    public boolean isEmergency() {
        return isEmergency;
    }

    public void setEmergency(boolean isEmergency) {
        this.isEmergency = isEmergency;
    }

    public String getEmergencyStatus() {
        return isEmergency ?
                "emergency " :
                "";
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
        dock();
        requestToTakeOff();
    }
}
