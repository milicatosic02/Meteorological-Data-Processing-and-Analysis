package command;

import model.StationData;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

public class PeriodicMapLogger implements Runnable {
    private final Map<Character, StationData> stationsMap;
    private final String outputFile;
    private final ReentrantLock fileLock;

    public PeriodicMapLogger(Map<Character, StationData> stationsMap, String outputFile, ReentrantLock fileLock) {
        this.stationsMap = stationsMap;
        this.outputFile = outputFile;
        this.fileLock = fileLock;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(60_000); // Pauza od 60 sekundi

                if (stationsMap == null || stationsMap.isEmpty()) {
                    System.out.println("[LOG] Mapa nije dostupna. Preskačem logovanje.");
                    continue;
                }

                fileLock.lock();
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {
                    writer.write("=== Periodični log: " + LocalDateTime.now() + " ===");
                    writer.newLine();
                    writer.write("Letter,Station count,Sum");
                    writer.newLine();

                    Map<Character, StationData> sorted = new TreeMap<>(stationsMap);
                    for (Map.Entry<Character, StationData> entry : sorted.entrySet()) {
                        char letter = entry.getKey();
                        StationData data = entry.getValue();
                        writer.write(letter + "," + data.getStationCount() + "," + data.getTemperatureSum());
                        writer.newLine();
                    }

                    writer.newLine();
                    System.out.println("[LOG] Periodični log mape je uspešno upisan u fajl: " + outputFile);
                } catch (IOException e) {
                    System.out.println("[LOG] Greška pri pisanju u log fajl: " + e.getMessage());
                } finally {
                    fileLock.unlock();
                }

            } catch (InterruptedException e) {
                System.out.println("[LOG] Periodični logger prekinut.");
                break;
            }
        }
    }
}
