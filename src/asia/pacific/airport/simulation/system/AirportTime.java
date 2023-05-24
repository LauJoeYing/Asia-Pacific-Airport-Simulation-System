package asia.pacific.airport.simulation.system;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class AirportTime {
    public static String getCurrentTimestamp(){
        String timestamp = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("d-MM-y kk:mm:ss"));

        return String.format("[ %s ]", timestamp);
    }
}

