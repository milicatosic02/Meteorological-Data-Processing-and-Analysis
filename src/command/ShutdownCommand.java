package command;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

public class ShutdownCommand implements Runnable {
    private final ExecutorService executorService;
    private final BlockingQueue<String> commandQueue;
    private final boolean saveJobs;
    private final Map<String, JobStatus> jobStatuses;
    private final Map<String, String> jobCommands; // mapa jobName -> originalna komanda

    public ShutdownCommand(ExecutorService executorService,
                           BlockingQueue<String> commandQueue,
                           boolean saveJobs,
                           Map<String, JobStatus> jobStatuses,
                           Map<String, String> jobCommands) {
        this.executorService = executorService;
        this.commandQueue = commandQueue;
        this.saveJobs = saveJobs;
        this.jobStatuses = jobStatuses;
        this.jobCommands = jobCommands;
    }

    @Override
    public void run() {
        try {
            if (saveJobs) {
                saveUnfinishedJobs();
            }

            executorService.shutdownNow();
            System.out.println("Sistem je zaustavljen.");
        } catch (Exception e) {
            System.out.println("Greška pri zaustavljanju sistema: " + e.getMessage());
        }
    }

    private void saveUnfinishedJobs() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("load_config.txt"))) {
            for (Map.Entry<String, JobStatus> entry : jobStatuses.entrySet()) {
                String jobName = entry.getKey();
                JobStatus status = entry.getValue();

                if (status == JobStatus.PENDING || status == JobStatus.RUNNING) {
                    String originalCommand = jobCommands.getOrDefault(jobName, null);
                    if (originalCommand != null) {
                        writer.write(originalCommand);
                        writer.newLine();
                        System.out.println("Sačuvan posao: " + jobName);
                    }
                }
            }
            System.out.println("Nezavršeni poslovi su sačuvani u fajl load_config.txt.");
        } catch (IOException e) {
            System.out.println("Greška pri čuvanju poslova: " + e.getMessage());
        }
    }
}
