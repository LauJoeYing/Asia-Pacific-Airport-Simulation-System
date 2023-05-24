package asia.pacific.airport.simulation.system;

import static java.util.Objects.isNull;

public class Gate implements Logging {
    private final int id;
    private Airplane airplane;

    public Gate(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public boolean isOccupied() {
        return !isNull(airplane);
    }

    public Airplane getAirplane() {
        return airplane;
    }

    public void setAirplane(Airplane airplane) {
        if (airplane == null) {
            String gateUnoccupationLoggingMessage = String.format(
                    "Unoccupied by %s.",
                    this.airplane.getName()
            );
            log(gateUnoccupationLoggingMessage);
        } else {
            String gateOccupationLoggingMessage = String.format(
                    "Gate is not occupied, can be assigned to %s for docking.",
                    airplane.getName()
            );
            log(gateOccupationLoggingMessage);
        };

        this.airplane = airplane;
    }

    public String getName() {
        return String.format("Gate %d", id);
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
}
