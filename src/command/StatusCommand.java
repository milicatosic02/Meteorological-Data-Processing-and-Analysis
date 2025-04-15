package command;

import java.util.Map;



import java.util.Map;

public class StatusCommand implements Runnable {
    private final String jobName;
    private final Map<String, JobStatus> jobStatuses;

    public StatusCommand(String jobName, Map<String, JobStatus> jobStatuses) {
        this.jobName = jobName;
        this.jobStatuses = jobStatuses;
    }

    @Override
    public void run() {
        try {
            JobStatus status = jobStatuses.get(jobName);
            if (status != null) {
                System.out.println("Status zadatka '" + jobName + "': " + status.toString().toLowerCase());
            } else {
                System.out.println("Zadatak sa imenom '" + jobName + "' nije pronađen.");
            }
        } catch (Exception e) {
            System.out.println("Greška prilikom prikaza statusa zadatka '" + jobName + "'.");
        }
    }
}
