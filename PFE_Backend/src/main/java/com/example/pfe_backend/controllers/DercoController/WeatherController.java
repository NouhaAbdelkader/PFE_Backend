package com.example.pfe_backend.controllers.DercoController;

import com.example.pfe_backend.services.DerCoService.WeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/weather")
public class WeatherController {

    private static final Logger logger = LoggerFactory.getLogger(WeatherController.class);
    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/{lat}/{lon}")
    public ResponseEntity<WeatherService.WeatherResponse> getWeeklyPrecipitation(
            @PathVariable double lat,
            @PathVariable double lon,
            @RequestParam(required = false, defaultValue = "UNKNOWN") String equipmentType,
            @RequestParam(required = false, defaultValue = "N/A") String address) {
        try {
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                logger.warn("Coordonnées invalides : lat={}, lon={}", lat, lon);
                return ResponseEntity.badRequest().body(new WeatherService.WeatherResponse(
                        null, null, "Coordonnées invalides.", 0.0, equipmentType, address));
            }

            WeatherService.WeatherResponse response = weatherService.getWeeklyPrecipitation(lat, lon, equipmentType, address);
            if (response.getDailyForecasts() == null && response.getHourlyForecast() == null) {
                logger.error("Échec de la récupération des données météo : {}", response.getAlertMessage());
                return ResponseEntity.status(500).body(response);
            }

            logger.info("Prévisions météo récupérées pour lat={}, lon={}, equipmentType={}, address={}", lat, lon, equipmentType, address);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur inattendue : {}", e.getMessage());
            return ResponseEntity.status(500).body(new WeatherService.WeatherResponse(
                    null, null, "Erreur interne : " + e.getMessage(), 0.0, equipmentType, address));
        }
    }
}