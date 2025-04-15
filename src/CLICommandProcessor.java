import command.*;
import model.StationData;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import command.JobStatus;


public class CLICommandProcessor implements Runnable {
    private final BlockingQueue<String> commandQueue;
    private final ExecutorService executorService;
    private final Map<String, JobStatus> jobStatuses;
    private final Map<Character, StationData> stationsMap;
    private final Map<String, String> jobCommands;
    private final String directoryPath;
    private final ReentrantLock fileLock;

    public CLICommandProcessor(BlockingQueue<String> commandQueue,
                               ExecutorService executorService,
                               String directoryPath,
                               Map<Character, StationData> stationsMap,
                               Map<String, JobStatus> jobStatuses,
                               Map<String, String> jobCommands,
                               ReentrantLock fileLock) {
        this.commandQueue = commandQueue;
        this.executorService = executorService;
        this.directoryPath = directoryPath;
        this.stationsMap = stationsMap;
        this.jobStatuses = jobStatuses;
        this.jobCommands = jobCommands;
        this.fileLock = fileLock;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String command = commandQueue.take();
                processCommand(command);
            } catch (InterruptedException e) {
                System.out.println("Greška pri preuzimanju komande: " + e.getMessage());
            }
        }
    }

    private void processCommand(String command) {
        String[] parts = command.trim().split("\\s+");
        if (parts.length == 0) {
            System.out.println("Greška: Prazna komanda.");
            return;
        }

        String mainCommand = parts[0].toUpperCase();
        Map<String, String> args = new HashMap<>();
        Map<String, String> shortToLong = Map.of(
                "-m", "--min",
                "-M", "--max",
                "-l", "--letter",
                "-o", "--output",
                "-j", "--job",
                "-s", "--save-jobs"
        );

        for (int i = 1; i < parts.length; i++) {
            String key = parts[i];
            if (key.startsWith("-")) {
                if (shortToLong.containsKey(key)) {
                    key = shortToLong.get(key);
                }
                if (i + 1 < parts.length && !parts[i + 1].startsWith("-")) {
                    args.put(key, parts[i + 1]);
                    i++;
                } else {
                    // Ovo je flag bez vrednosti (npr. --save-jobs)
                    args.put(key, "true");
                }
            } else {
                System.out.println("Greška: Nepoznata sintaksa " + parts[i]);
                return;
            }
        }

        switch (mainCommand) {
            case "SCAN":
                if (!args.containsKey("--min") || !args.containsKey("--max") || !args.containsKey("--job")) {
                    System.out.println("Greška: SCAN zahteva --min, --max i --job argumente.");
                    return;
                }

                String jobName = args.get("--job");
                jobStatuses.put(jobName, JobStatus.PENDING);
                jobCommands.put(jobName, command);

                executorService.submit(() -> {
                    jobStatuses.put(jobName, JobStatus.RUNNING);
                    try {
                        double min = Double.parseDouble(args.get("--min"));
                        double max = Double.parseDouble(args.get("--max"));
                        char letter = args.containsKey("--letter") ? args.get("--letter").charAt(0) : 'A';
                        String output = args.getOrDefault("--output", "output.txt");

                        new ScanCommand(min, max, letter, output, jobName, executorService, directoryPath).run();
                        jobStatuses.put(jobName, JobStatus.COMPLETED);
                    } catch (Exception e) {
                        jobStatuses.put(jobName, JobStatus.FAILED);
                        System.out.println("Greška u SCAN komandi: " + e.getMessage());
                    }
                });
                break;

            case "STATUS":
                if (!args.containsKey("--job")) {
                    System.out.println("Greška: STATUS zahteva --job argument.");
                    return;
                }
                executorService.submit(new StatusCommand(args.get("--job"), jobStatuses));
                break;

            case "MAP":
                executorService.submit(new MapCommand(stationsMap));
                break;
            case "EXPORTMAP":
                String output = args.getOrDefault("--output", "map_export.csv");
                executorService.submit(new ExportMapCommand(output, stationsMap,fileLock));
                break;
            case "START":
                boolean load = args.containsKey("--load-jobs") || args.containsKey("-l");
                executorService.submit(new StartCommand(executorService, commandQueue, load));
                break;
            case "SHUTDOWN":
                boolean save = args.containsKey("--save-jobs") || args.containsKey("-s");
                executorService.submit(new ShutdownCommand(
                        executorService, commandQueue, save, jobStatuses, jobCommands
                ));
                break;


            default:
                System.out.println("Greška: Nepoznata komanda " + mainCommand);
        }
    }
}

//START --load-jobs
//SCAN -l 2 -m 10.0 -M 2.0 -o output.txt -j job1
//SCAN -l H -m 10.0 -M 20.0 -o output.txt -job job1
//SCAN -l H -m 10.0 -M 20.0 -o output.txt -j job1
//SCAN -l H -m 10.0 -M 20.0 -o output.txt -j job2
//SCAN -l H -m 10.0 -M 20.0 -o output.txt -j job3
//STATUS job4
//STATUS -j job3
//START
//EXPORTMAP
//EXPORTMAP
//MAP
//SHUTDOWN --save
//SHUTDOWN --save-jobs