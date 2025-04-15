package model;

public class StationData {
    private int stationCount;
    private double temperatureSum;

    public StationData(int stationCount, double temperatureSum) {
        this.stationCount = stationCount;
        this.temperatureSum = temperatureSum;
    }

    public void addData(double temperature) {
        stationCount++;
        temperatureSum += temperature;
    }

    public int getStationCount() {
        return stationCount;
    }

    public double getTemperatureSum() {
        return temperatureSum;
    }

    @Override
    public String toString() {
        return stationCount + " - " + temperatureSum;
    }
}
