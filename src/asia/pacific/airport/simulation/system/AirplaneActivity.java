package asia.pacific.airport.simulation.system;

import static java.lang.System.currentTimeMillis;

public class AirplaneActivity implements Comparable<AirplaneActivity> {
    private final Airplane airplane;
    private final String activity;
    private final Long requestTime;

    public AirplaneActivity(Airplane airplane, String activity) {
        this.airplane = airplane;
        this.activity = activity;
        this.requestTime = currentTimeMillis();
    }

    public Airplane getAirplane() {
        return airplane;
    }

    public String getActivity() {
        return activity;
    }

    @Override
    public String toString() {
        return String.format(
                "%s: %s",
                airplane.getName(),
                activity.equals("LAND") ?
                        "landing" :
                        "take off"
        );
    }

    @Override
    public int compareTo(AirplaneActivity other) {
        if (airplane.isEmergency()) {
            if (activity.equals("TAKE OFF")) {
                return -2;
            } else {
                return -1;
            }
        }

        return Long.compare(this.requestTime, other.requestTime);
    }
}
