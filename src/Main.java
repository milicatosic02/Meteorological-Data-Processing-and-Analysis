import command.JobStatus;
import model.StationData;

import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    public static void main(String[] args) {
        // 1. Putanja direktorijuma za nadgledanje (možeš zameniti sa args[0] ako želiš da se prosleđuje)
        String directoryPath = "data";

        // 2. Zajedničke strukture
        Map<Character, StationData> stationsMap = new ConcurrentHashMap<>();
        Map<String, JobStatus> jobStatuses = new ConcurrentHashMap<>();
        Map<String, String> jobCommands = new ConcurrentHashMap<>();
        BlockingQueue<String> commandQueue = new LinkedBlockingQueue<>();
        ReentrantLock fileLock = new ReentrantLock();

        // 3. ExecutorService za zadatke
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        // 4. Pokretanje monitoring niti
        MonitoringDirectoryThread directoryWatcher = new MonitoringDirectoryThread(directoryPath, executorService, stationsMap, fileLock);

        Thread monitorThread = new Thread(directoryWatcher);
        monitorThread.start();

        // 5. Pokretanje CLI command processor niti (uzima iz commandQueue)
        CLICommandProcessor cliProcessor = new CLICommandProcessor(
                commandQueue, executorService, directoryPath,
                stationsMap, jobStatuses, jobCommands, fileLock
        );
        Thread cliThread = new Thread(cliProcessor);
        cliThread.start();

        // 6. Pokretanje periodičnog logovanja
        Thread periodicLogger = new Thread(
                new command.PeriodicMapLogger(stationsMap, "map_export.csv", fileLock)
        );
        periodicLogger.start();

        // 7. Čitanje komandi sa System.in i dodavanje u queue
        Thread inputThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String line = scanner.nextLine().trim();
                if (line.equalsIgnoreCase("STOP")) {
                    break; // STOP ne ide u queue, već zaustavlja samo ovu nit
                }
                commandQueue.offer(line);
            }
        });
        inputThread.start();

        System.out.println("Sistem je pokrenut. Unesite komande ili STOP za izlaz iz CLI unosa.");
    }
}
