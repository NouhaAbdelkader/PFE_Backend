package com.example.pfe_backend.services.DerCoService;

import com.example.pfe_backend.entities.weather.DailyWeather;
import com.example.pfe_backend.entities.weather.HourlyWeather;
import com.example.pfe_backend.entities.Equipements.Equipement;
import com.example.pfe_backend.repos.EquipementRepo.EquipementRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

    @Value("${weatherapi.api.key}")
    private String weatherApiKey;

    @Value("${groq.api.key}")
    private String groqApiKey;

    private static final String WEATHER_API_URL = "https://api.openweathermap.org/data/2.5/forecast";
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EquipementRepo equipementRepo;

    public WeatherService(RestTemplate restTemplate, ObjectMapper objectMapper, EquipementRepo equipementRepo) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.equipementRepo = equipementRepo;
        logger.info("Clé API OpenWeatherMap : {}", weatherApiKey != null ? "configurée" : "manquante");
        logger.info("Clé API Groq : {}", groqApiKey != null ? "configurée" : "manquante");
        if (groqApiKey == null || groqApiKey.trim().isEmpty()) {
            logger.error("Clé API Groq non configurée ou vide. Les appels à l'API Groq échoueront.");
        }
    }

    public WeatherResponse getWeeklyPrecipitation(double lat, double lon, String equipmentType, String address) {
        // Step 1: Fetch and process weather data
        WeatherData weatherData = fetchWeatherData(lat, lon, equipmentType, address);
        if (weatherData.dailyForecasts == null || weatherData.dailyForecasts.isEmpty()) {
            logger.warn("No weather data available for lat={}, lon={}", lat, lon);
            return new WeatherResponse(
                    null, null, "Error: No weather data available.", 0.0,
                    weatherData.equipmentType, weatherData.address
            );
        }

        // Step 2: Calculate thresholds and identify risk
        RiskAssessment riskAssessment = calculateRiskThresholds(weatherData.dailyForecasts, weatherData.equipmentType);

        // Step 3: Generate alert only if equipment is at risk
        String alertMessage;
        double riskScore;
        if (riskAssessment.isAtRisk) {
            logger.info("Equipment at risk detected: type={}, lat={}, lon={}. Generating AI alert.",
                    weatherData.equipmentType, lat, lon);
            Map<String, Object> aiResponse = generateAIAlert(riskAssessment, weatherData);
            alertMessage = (String) aiResponse.getOrDefault("alertMessage", "Alert: Check the equipment.");
            riskScore = (Double) aiResponse.getOrDefault("riskScore", 0.0);
        } else {
            alertMessage = String.format("No significant risk for %s at %s.", weatherData.equipmentType, weatherData.address);
            riskScore = 0.3;
            logger.info("No risk detected for equipment: type={}, lat={}, lon={}. Using default alert: {}",
                    weatherData.equipmentType, lat, lon, alertMessage);
        }

        return new WeatherResponse(
                weatherData.dailyForecasts,
                weatherData.hourlyForecast,
                alertMessage,
                riskScore,
                weatherData.equipmentType,
                weatherData.address
        );
    }

    private WeatherData fetchWeatherData(double lat, double lon, String equipmentType, String address) {
        Optional<Equipement> equipment = equipementRepo.findByLatitudeAndLongitude(lat, lon);

        String validatedEquipmentType = equipmentType;
        String validatedAddress = address;
        if (equipment.isPresent()) {
            validatedEquipmentType = equipment.get().getCategorie();
            validatedAddress = Optional.ofNullable(equipment.get().getAdresse()).orElse("Address not specified");
            logger.info("Équipement trouvé pour lat={}, lon={}: type={}, adresse={}",
                    lat, lon, validatedEquipmentType, validatedAddress);
        } else {
            validatedEquipmentType = Optional.ofNullable(equipmentType).orElse("UNKNOWN");
            validatedAddress = Optional.ofNullable(address).orElse("Address not specified");
            logger.warn("Aucun équipement trouvé pour lat={}, lon={}", lat, lon);
        }

        String url = String.format("%s?lat=%s&lon=%s&units=metric&appid=%s", WEATHER_API_URL, lat, lon, weatherApiKey);
        logger.info("Requête API météo : {}", url);

        try {
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                logger.error("Aucune donnée reçue de l'API OpenWeatherMap pour lat={}, lon={}", lat, lon);
                return new WeatherData(null, null, validatedEquipmentType, validatedAddress, lat, lon);
            }

            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode listNode = rootNode.path("list");

            Map<String, DailyWeather> dailyForecastsMap = new TreeMap<>();
            List<HourlyWeather> hourlyForecast = new ArrayList<>();

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime next24Hours = now.plusHours(24);

            if (listNode.isArray()) {
                for (JsonNode item : listNode) {
                    String dateTime = item.path("dt_txt").asText();
                    LocalDateTime forecastDateTime = LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    String date = dateTime.split(" ")[0];
                    String time = dateTime.split(" ")[1].substring(0, 5);

                    double temp = item.path("main").path("temp").asDouble(0.0);
                    double humidity = item.path("main").path("humidity").asDouble(0.0);
                    double windSpeed = item.path("wind").path("speed").asDouble(0.0);
                    double rain = item.path("rain").path("3h").asDouble(0.0);
                    double clouds = item.path("clouds").path("all").asDouble(0.0);
                    JsonNode weatherNode = item.path("weather").get(0);
                    String description = weatherNode != null ? weatherNode.path("description").asText("N/A") : "N/A";

                    String basicAlert = generateBasicAlert(rain, windSpeed, validatedEquipmentType);

                    if (forecastDateTime.isAfter(now) && forecastDateTime.isBefore(next24Hours)) {
                        hourlyForecast.add(new HourlyWeather(dateTime, time, temp, humidity, windSpeed, rain, clouds, description, basicAlert));
                    }

                    dailyForecastsMap.compute(date, (key, value) -> {
                        if (value == null) {
                            return new DailyWeather(date, temp, humidity, windSpeed, rain, clouds, description, basicAlert, 1);
                        }
                        int count = value.getCount() + 1;
                        double newTemp = (value.getTemp() * value.getCount() + temp) / count;
                        double newHumidity = (value.getHumidity() * value.getCount() + humidity) / count;
                        double newWindSpeed = (value.getWindSpeed() * value.getCount() + windSpeed) / count;
                        double newRain = value.getPrecipitation() + rain;
                        double newClouds = (value.getClouds() * value.getCount() + clouds) / count;
                        return new DailyWeather(date, newTemp, newHumidity, newWindSpeed, newRain, newClouds, description, basicAlert, count);
                    });
                }
                logger.info("Prévisions horaires récupérées : {} entrées", hourlyForecast.size());
                logger.info("Prévisions quotidiennes récupérées : {} jours", dailyForecastsMap.size());
            } else {
                logger.warn("Aucune donnée de prévision trouvée dans la réponse de l'API.");
            }

            return new WeatherData(
                    new ArrayList<>(dailyForecastsMap.values()),
                    hourlyForecast,
                    validatedEquipmentType,
                    validatedAddress,
                    lat,
                    lon
            );

        } catch (HttpClientErrorException e) {
            logger.error("Erreur HTTP lors de l'appel à l'API OpenWeatherMap : {} - {}", e.getStatusCode(), e.getMessage());
            return new WeatherData(null, null, validatedEquipmentType, validatedAddress, lat, lon);
        } catch (Exception e) {
            logger.error("Erreur inattendue dans fetchWeatherData pour lat={}, lon={} : {}", lat, lon, e.getMessage(), e);
            return new WeatherData(null, null, validatedEquipmentType, validatedAddress, lat, lon);
        }
    }

    private RiskAssessment calculateRiskThresholds(List<DailyWeather> dailyForecasts, String equipmentType) {
        if (dailyForecasts == null || dailyForecasts.isEmpty()) {
            logger.warn("No daily forecasts available for risk assessment.");
            return new RiskAssessment(false, null, 0.0, 0.0);
        }

        DailyWeather criticalDay = dailyForecasts.stream()
                .max(Comparator.comparingDouble(DailyWeather::getPrecipitation))
                .orElse(dailyForecasts.get(0));

        double precipitation = criticalDay.getPrecipitation();
        double windSpeed = criticalDay.getWindSpeed();
        boolean isAtRisk = false;

        switch (equipmentType.toUpperCase()) {
            case "TRAPPE":
                isAtRisk = precipitation > 5;
                break;
            case "POTEAU":
                isAtRisk = windSpeed > 15;
                break;
            case "CABLE":
                isAtRisk = precipitation > 5 || windSpeed > 10;
                break;
            case "ARMOIRE":
                isAtRisk = precipitation > 5 || windSpeed > 12;
                break;
            case "COFFRET":
                isAtRisk = precipitation > 7;
                break;
            default:
                isAtRisk = false;
        }

        logger.info("Risk assessment for equipment={} : isAtRisk={}, precipitation={} mm, windSpeed={} m/s, criticalDate={}",
                equipmentType, isAtRisk, precipitation, windSpeed, criticalDay.getDate());
        return new RiskAssessment(isAtRisk, criticalDay.getDate(), precipitation, windSpeed);
    }

    private Map<String, Object> generateAIAlert(RiskAssessment riskAssessment, WeatherData weatherData) {
        logger.info("Initiating AI alert generation for equipment: type={}, address={}, lat={}, lon={}",
                weatherData.equipmentType, weatherData.address, weatherData.latitude, weatherData.longitude);
        if (groqApiKey == null || groqApiKey.trim().isEmpty()) {
            logger.error("Groq API key is missing or empty. Falling back to simulation for equipment={}",
                    weatherData.equipmentType);
            return simulateAIResponse(riskAssessment, weatherData);
        }

        int maxRetries = 3;
        int retryDelayMillis = 2000;

        DecimalFormat df = new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(Locale.US));
        String weatherSummary = String.format(
                "Location: %s (lat: %.2f, lon: %.2f), Equipment: %s, Precipitation: %s mm, Wind: %s m/s, Description: %s, Date: %s",
                weatherData.address, weatherData.latitude, weatherData.longitude, weatherData.equipmentType,
                df.format(riskAssessment.precipitation), df.format(riskAssessment.windSpeed),
                weatherData.dailyForecasts.get(0).getDescription(), riskAssessment.criticalDate
        );

        String prompt = String.format(
                "You are an AI agent specialized in weather risks for equipment. " +
                        "Weather data: %s. " +
                        "Equipment: %s. " +
                        "Risk thresholds: " +
                        "- POTEAU: wind > 15 m/s. " +
                        "- CABLE: wind > 10 m/s or precipitation > 5 mm. " +
                        "- TRAPPE: precipitation > 5 mm. " +
                        "- ARMOIRE: precipitation > 5 mm or wind > 12 m/s. " +
                        "- COFFRET: precipitation > 7 mm. " +
                        "Task: Generate an alert message for the equipment at risk (e.g., 'Flood risk for the hatch at [address] on [date]. Check waterproofing.'). " +
                        "Calculate a risk score between 0 and 1 (e.g., 0.8 for high risk). " +
                        "Return a JSON object: {\"alertMessage\": \"message\", \"riskScore\": score}.",
                weatherSummary, weatherData.equipmentType
        );
        logger.debug("AI prompt constructed for equipment={}: {}", weatherData.equipmentType, prompt);

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            logger.info("Attempting Groq API call {} of {} for equipment={}", attempt, maxRetries, weatherData.equipmentType);
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(groqApiKey);
                headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

                Map<String, Object> message = Map.of("role", "user", "content", prompt);
                Map<String, Object> requestBody = Map.of(
                        "model", "llama3-70b-8192",
                        "messages", List.of(message),
                        "temperature", 0.7,
                        "max_tokens", 300,
                        "response_format", Map.of("type", "json_object")
                );

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
                logger.debug("Sending Groq API request: URL={}, Headers={}", GROQ_API_URL, headers);
                String response = restTemplate.postForObject(GROQ_API_URL, request, String.class);
                logger.debug("Received Groq API response for equipment={}: {}", weatherData.equipmentType, response);

                JsonNode json = objectMapper.readTree(response);
                String content = json.path("choices").get(0).path("message").path("content").asText();
                logger.debug("Parsed Groq response content for equipment={}: {}", weatherData.equipmentType, content);

                JsonNode result = objectMapper.readTree(content);
                String alertMessage = result.path("alertMessage").asText("Alert: Check the equipment.");
                double riskScore = result.path("riskScore").asDouble(0.0);

                if (alertMessage.equals("Alert: Check the equipment.") || riskScore == 0.0) {
                    logger.warn("Invalid or incomplete AI response for equipment={}: alertMessage={}, riskScore={}. Falling back to simulation.",
                            weatherData.equipmentType, alertMessage, riskScore);
                    return simulateAIResponse(riskAssessment, weatherData);
                }

                logger.info("Successful Groq AI response for equipment={}: alertMessage={}, riskScore={}",
                        weatherData.equipmentType, alertMessage, riskScore);
                return Map.of("alertMessage", alertMessage, "riskScore", riskScore);

            } catch (HttpClientErrorException e) {
                logger.error("HTTP error during Groq API call for equipment={} (attempt {}/{}): status={}, response={}",
                        weatherData.equipmentType, attempt, maxRetries, e.getStatusCode(), e.getResponseBodyAsString());
                if (e.getStatusCode().value() == 429 && attempt < maxRetries) {
                    logger.warn("Rate limit hit (429) for equipment={}. Waiting {} ms before retry.",
                            weatherData.equipmentType, retryDelayMillis);
                    try {
                        Thread.sleep(retryDelayMillis);
                    } catch (InterruptedException ie) {
                        logger.error("Interrupted during retry wait for equipment={}: {}",
                                weatherData.equipmentType, ie.getMessage());
                        return simulateAIResponse(riskAssessment, weatherData);
                    }
                } else {
                    logger.error("Groq API call failed after {} attempts for equipment={}. Falling back to simulation.",
                            attempt, weatherData.equipmentType);
                    return simulateAIResponse(riskAssessment, weatherData);
                }
            } catch (Exception e) {
                logger.error("Unexpected error during Groq API call for equipment={} (attempt {}/{}): {}",
                        weatherData.equipmentType, attempt, maxRetries, e.getMessage(), e);
                return simulateAIResponse(riskAssessment, weatherData);
            }
        }
        logger.error("All Groq API attempts failed for equipment={}. Falling back to simulation.",
                weatherData.equipmentType);
        return simulateAIResponse(riskAssessment, weatherData);
    }

    private String generateBasicAlert(double precipitation, double windSpeed, String equipmentType) {
        switch (equipmentType.toUpperCase()) {
            case "TRAPPE":
                return precipitation > 5 ? "⚠️ Alert: Flood risk for the hatch!" : null;
            case "POTEAU":
                return windSpeed > 15 ? "⚠️ Alert: Risk of damage to the pole due to strong winds!" : null;
            case "CABLE":
                return (precipitation > 5 || windSpeed > 10) ? "⚠️ Alert: Risk to the cable due to precipitation or winds!" : null;
            case "ARMOIRE":
                return (precipitation > 5 || windSpeed > 12) ? "⚠️ Alert: Risk to the cabinet due to precipitation or winds!" : null;
            case "COFFRET":
                return precipitation > 7 ? "⚠️ Alert: Flood risk for the box!" : null;
            default:
                return null;
        }
    }

    private Map<String, Object> simulateAIResponse(RiskAssessment riskAssessment, WeatherData weatherData) {
        logger.info("Simulating AI response for equipment: type={}, address={}, lat={}, lon={}",
                weatherData.equipmentType, weatherData.address, weatherData.latitude, weatherData.longitude);
        double precipitation = riskAssessment.precipitation;
        double windSpeed = riskAssessment.windSpeed;
        String address = weatherData.address;
        String date = riskAssessment.criticalDate != null ? riskAssessment.criticalDate : "N/A";

        double riskScore = 0.0;
        StringBuilder alertMessage = new StringBuilder();

        switch (weatherData.equipmentType.toUpperCase()) {
            case "TRAPPE":
                if (precipitation > 5) {
                    riskScore = Math.min(0.8 + precipitation * 0.01, 1.0);
                    alertMessage.append(String.format("Flood risk for the hatch at %s on %s. Check waterproofing.", address, date));
                } else {
                    alertMessage.append(String.format("No significant alert for the hatch at %s on %s.", address, date));
                    riskScore = 0.3;
                }
                break;
            case "POTEAU":
                if (windSpeed > 15) {
                    riskScore = Math.min(0.7 + windSpeed * 0.01, 1.0);
                    alertMessage.append(String.format("Risk of damage to the pole at %s on %s due to winds of %.1f m/s. Secure the equipment.", address, date, windSpeed));
                } else {
                    alertMessage.append(String.format("No significant alert for the pole at %s on %s.", address, date));
                    riskScore = 0.3;
                }
                break;
            case "CABLE":
                if (precipitation > 5 || windSpeed > 10) {
                    riskScore = Math.min(0.6 + (precipitation + windSpeed) * 0.01, 1.0);
                    alertMessage.append(String.format("Risk to the cable at %s on %s due to precipitation (%.1f mm) or winds (%.1f m/s). Check stability.", address, date, precipitation, windSpeed));
                } else {
                    alertMessage.append(String.format("No significant alert for the cable at %s on %s.", address, date));
                    riskScore = 0.3;
                }
                break;
            case "ARMOIRE":
                if (precipitation > 5 || windSpeed > 12) {
                    riskScore = Math.min(0.6 + (precipitation + windSpeed) * 0.01, 1.0);
                    alertMessage.append(String.format("Risk to the cabinet at %s on %s. Check waterproofing and stability.", address, date));
                } else {
                    alertMessage.append(String.format("No significant alert for the cabinet at %s on %s.", address, date));
                    riskScore = 0.3;
                }
                break;
            case "COFFRET":
                if (precipitation > 7) {
                    riskScore = Math.min(0.7 + precipitation * 0.01, 1.0);
                    alertMessage.append(String.format("Flood risk for the box at %s on %s. Check waterproofing.", address, date));
                } else {
                    alertMessage.append(String.format("No significant alert for the box at %s on %s.", address, date));
                    riskScore = 0.3;
                }
                break;
            default:
                alertMessage.append(String.format("No significant alert for the equipment %s at %s on %s.", weatherData.equipmentType, address, date));
                riskScore = 0.3;
        }

        logger.info("Simulated AI response for equipment={}: alertMessage={}, riskScore={}",
                weatherData.equipmentType, alertMessage.toString(), riskScore);
        return Map.of("alertMessage", alertMessage.toString(), "riskScore", riskScore);
    }

    private static class WeatherData {
        final List<DailyWeather> dailyForecasts;
        final List<HourlyWeather> hourlyForecast;
        final String equipmentType;
        final String address;
        final double latitude;
        final double longitude;

        WeatherData(List<DailyWeather> dailyForecasts, List<HourlyWeather> hourlyForecast,
                    String equipmentType, String address, double latitude, double longitude) {
            this.dailyForecasts = dailyForecasts;
            this.hourlyForecast = hourlyForecast;
            this.equipmentType = equipmentType;
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private static class RiskAssessment {
        final boolean isAtRisk;
        final String criticalDate;
        final double precipitation;
        final double windSpeed;

        RiskAssessment(boolean isAtRisk, String criticalDate, double precipitation, double windSpeed) {
            this.isAtRisk = isAtRisk;
            this.criticalDate = criticalDate;
            this.precipitation = precipitation;
            this.windSpeed = windSpeed;
        }
    }

    public static class WeatherResponse {
        private final List<DailyWeather> dailyForecasts;
        private final List<HourlyWeather> hourlyForecast;
        private final String alertMessage;
        private final double riskScore;
        private final String equipmentType;
        private final String address;

        public WeatherResponse(List<DailyWeather> dailyForecasts, List<HourlyWeather> hourlyForecast,
                               String alertMessage, double riskScore, String equipmentType, String address) {
            this.dailyForecasts = dailyForecasts;
            this.hourlyForecast = hourlyForecast;
            this.alertMessage = alertMessage != null ? alertMessage : "No alert available";
            this.riskScore = riskScore;
            this.equipmentType = equipmentType != null ? equipmentType : "UNKNOWN";
            this.address = address != null ? address : "Address not specified";
        }

        public List<DailyWeather> getDailyForecasts() { return dailyForecasts; }
        public List<HourlyWeather> getHourlyForecast() { return hourlyForecast; }
        public String getAlertMessage() { return alertMessage; }
        public double getRiskScore() { return riskScore; }
        public String getEquipmentType() { return equipmentType; }
        public String getAddress() { return address; }
    }
}