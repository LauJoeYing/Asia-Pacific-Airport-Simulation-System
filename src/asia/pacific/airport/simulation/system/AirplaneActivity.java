package asia.pacific.airport.simulation.system;

import static java.lang.System.currentTimeMillis;

public class AirplaneActivity implements Comparable<AirplaneActivity> {
    private final Airplane airplane;
    private final AirplaneAction action;
    private final Long requestTime;

    public AirplaneActivity(Airplane airplane, AirplaneAction action) {
        this.airplane = airplane;
        this.action = action;
        this.requestTime = currentTimeMillis();
    }

    public Airplane getAirplane() {
        return airplane;
    }

    public AirplaneAction getAction() {
        return action;
    }

    @Override
    public String toString() {
        return String.format(
                "%s: %s",
                airplane.getName(),
                action.equals(AirplaneAction.LAND) ?
                        "landing" :
                        "take off"
        );
    }

    @Override
    public int compareTo(AirplaneActivity other) {
        if (airplane.isEmergency()) {
            if (action.equals(AirplaneAction.TAKE_OFF)) {
                return -2;
            } else {
                return -1;
            }
        }

        return Long.compare(this.requestTime, other.requestTime);
    }
}
