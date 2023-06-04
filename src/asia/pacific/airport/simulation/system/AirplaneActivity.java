package asia.pacific.airport.simulation.system;

import static java.lang.System.currentTimeMillis;

public class AirplaneActivity implements Comparable<AirplaneActivity> {
    private final AirplaneAction action;
    private final Long actionRequestTime;
    private boolean isEmergency;
    private final Object actionApprovalLock;
    private boolean isActionApprovalGranted;
    private final Object actionCompletionLock;
    private boolean isActionCompleted;

    public AirplaneActivity(AirplaneAction action) {
        this.action = action;
        isEmergency = false;
        isActionApprovalGranted = false;
        actionApprovalLock = new Object();
        isActionCompleted = false;
        actionCompletionLock = new Object();
        this.actionRequestTime = currentTimeMillis();
    }

    public AirplaneActivity(AirplaneAction action, boolean isEmergency) {
        this.action = action;
        this.isEmergency = isEmergency;
        isActionApprovalGranted = false;
        actionApprovalLock = new Object();
        isActionCompleted = false;
        actionCompletionLock = new Object();
        this.actionRequestTime = currentTimeMillis();
    }

    public AirplaneAction getAction() {
        return action;
    }

    public boolean isEmergency() {
        return isEmergency;
    }

    public void setEmergency(boolean isEmergency) {
        this.isEmergency = isEmergency;
    }

    public boolean isActionApprovalGranted() {
        synchronized (actionApprovalLock) {
            return isActionApprovalGranted;
        }
    }

    public void setActionApprovalGranted(boolean isActionRequestApproved) {
        synchronized (actionApprovalLock) {
            this.isActionApprovalGranted = isActionRequestApproved;
            actionApprovalLock.notifyAll();
        }
    }

    public void waitForActionRequestApproval() {
        synchronized (actionApprovalLock) {
            while (!isActionApprovalGranted) {
                try {
                    actionApprovalLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Object getActionApprovalLock() {
        return actionApprovalLock;
    }

    public boolean isActionCompleted() {
        synchronized (actionCompletionLock) {
            return isActionCompleted;
        }
    }

    public void setActionCompleted(boolean isActionCompleted) {
        synchronized (actionCompletionLock) {
            this.isActionCompleted = isActionCompleted;
            actionCompletionLock.notifyAll();
        }
    }

    public void waitForActionCompletion() {
        synchronized (actionCompletionLock) {
            while (!isActionCompleted) {
                try {
                    actionCompletionLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Object getActionCompletionLock() {
        return actionCompletionLock;
    }

    public Long getActionRequestTime() {
        return actionRequestTime;
    }

    public String getName() {
        return action == AirplaneAction.LANDING ? "Landing" : "Take Off";
    }

    @Override
    public int compareTo(AirplaneActivity other) {
        if (isEmergency) {
            if (action.equals(AirplaneAction.TAKE_OFF)) {
                return -2;
            } else {
                return -1;
            }
        }

        return Long.compare(this.actionRequestTime, other.actionRequestTime);
    }
}
