package command;

import model.StationData;

import java.util.*;

public class MapCommand implements Runnable {
    private final Map<Character, StationData> stationsMap;

    public MapCommand(Map<Character, StationData> stationsMap) {
        this.stationsMap = stationsMap;
    }

    @Override
    public void run() {
        if (stationsMap == null || stationsMap.isEmpty()) {
            System.out.println("Mapa još uvek nije dostupna.");
            return;
        }

        List<Character> keys = new ArrayList<>(stationsMap.keySet());
        Collections.sort(keys);

        int lineCount = 0;
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < keys.size() && lineCount < 13; i += 2, lineCount++) {
            char first = keys.get(i);
            StationData data1 = stationsMap.get(first);

            output.append(first).append(": ")
                    .append(data1.getStationCount()).append(" - ").append(data1.getTemperatureSum());

            if (i + 1 < keys.size()) {
                char second = keys.get(i + 1);
                StationData data2 = stationsMap.get(second);
                output.append(" | ")
                        .append(second).append(": ")
                        .append(data2.getStationCount()).append(" - ").append(data2.getTemperatureSum());
            }

            output.append("\n");
        }

        System.out.print(output.toString());
    }
}
