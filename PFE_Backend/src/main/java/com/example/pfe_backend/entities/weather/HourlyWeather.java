package com.example.pfe_backend.entities.weather;

public   class HourlyWeather {
    private final String dateTime;
    private final String time;
    private final double temp;
    private final double humidity;
    private final double windSpeed;
    private final double precipitation;
    private final double clouds;
    private final String description;
    private final String alert;

    public HourlyWeather(String dateTime, String time, double temp, double humidity, double windSpeed, double precipitation, double clouds, String description, String alert) {
        this.dateTime = dateTime;
        this.time = time;
        this.temp = temp;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.precipitation = precipitation;
        this.clouds = clouds;
        this.description = description;
        this.alert = alert;
    }

    public String getDateTime() { return dateTime; }
    public String getTime() { return time; }
    public double getTemp() { return temp; }
    public double getHumidity() { return humidity; }
    public double getWindSpeed() { return windSpeed; }
    public double getPrecipitation() { return precipitation; }
    public double getClouds() { return clouds; }
    public String getDescription() { return description; }
    public String getAlert() { return alert; }
}

