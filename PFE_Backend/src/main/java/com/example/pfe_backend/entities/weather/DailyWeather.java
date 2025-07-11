package com.example.pfe_backend.entities.weather;

public  class DailyWeather {
    private final String date;
    private final double temp;
    private final double humidity;
    private final double windSpeed;
    private final double precipitation;
    private final double clouds;
    private final String description;
    private final String alert;
    private final int count;

    public DailyWeather(String date, double temp, double humidity, double windSpeed, double precipitation, double clouds, String description, String alert, int count) {
        this.date = date;
        this.temp = temp;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.precipitation = precipitation;
        this.clouds = clouds;
        this.description = description;
        this.alert = alert;
        this.count = count;
    }

    public String getDate() { return date; }
    public double getTemp() { return temp; }
    public double getHumidity() { return humidity; }
    public double getWindSpeed() { return windSpeed; }
    public double getPrecipitation() { return precipitation; }
    public double getClouds() { return clouds; }
    public String getDescription() { return description; }
    public String getAlert() { return alert; }
    public int getCount() { return count; }
}
