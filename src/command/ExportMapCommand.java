package command;

import model.StationData;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

public class ExportMapCommand implements Runnable {
    private final String outputFile;
    private final Map<Character, StationData> stationsMap;
    private final ReentrantLock fileLock;


    public ExportMapCommand(String outputFile, Map<Character, StationData> stationsMap, ReentrantLock fileLock) {
        this.outputFile = outputFile;
        this.stationsMap = stationsMap;
        this.fileLock = fileLock;
    }

    @Override
    public void run() {
        if (stationsMap == null || stationsMap.isEmpty()) {
            System.out.println("Mapa nije dostupna ili je prazna. Nema šta da se eksportuje.");
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("Letter,Station count,Sum");
            writer.newLine();

            Map<Character, StationData> sortedMap = new TreeMap<>(stationsMap); // sortiranje po slovima

            for (Map.Entry<Character, StationData> entry : sortedMap.entrySet()) {
                char letter = entry.getKey();
                StationData data = entry.getValue();

                writer.write(letter + "," + data.getStationCount() + "," + data.getTemperatureSum());
                writer.newLine();
            }

            System.out.println("Mapa je uspešno eksportovana u fajl: " + outputFile);
        } catch (IOException e) {
            System.out.println("Greška pri eksportovanju mape: " + e.getMessage());
        }
    }
}
