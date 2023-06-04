package asia.pacific.airport.simulation.system;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class GateHandler {
    public static final int GATE_CAPACITY = 3;
    private final AtomicReferenceArray<Gate> gates;
    final Semaphore gateSemaphore;

    public GateHandler() {
        gateSemaphore = new Semaphore(GATE_CAPACITY);
        gates = new AtomicReferenceArray<>(new Gate[GATE_CAPACITY]);
        for (int i = 0; i < GATE_CAPACITY; i++) {
            gates.set(i, new Gate(i + 1));
        }
    }

    public AtomicReferenceArray<Gate> getGates() {
        return gates;
    }

    public boolean gateIsFull() {
        return gateSemaphore.availablePermits() == 0;
    }

    public boolean gateIsSufficient() {
        return gateSemaphore.availablePermits() > 0;
    }

    public int acquireGate(Airplane airplane) {
        try {
            gateSemaphore.acquire(1);
            for (int i = 0; i < gates.length(); i++) {
                Gate gate = gates.get(i);
                if (!gate.isOccupied()) {
                    gate.setAirplane(airplane);
                    return gate.getId();
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return -1;
    }

    public void releaseGate(Airplane airplane) {
        gateSemaphore.release(1);
        for (int i = 0; i < gates.length(); i++) {
            Gate gate = gates.get(i);
            if (
                gate != null &&
                gate.getAirplane() != null &&
                gate.getAirplane().getName().equals(airplane.getName())
            )
            {
                gate.setAirplane(null);
                break;
            }
        }
    }
}
