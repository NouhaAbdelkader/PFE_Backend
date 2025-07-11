package com.example.pfe_backend.services.EquipementsService;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Scanner;

public class GeoUtils {
    private static final Logger logger = LoggerFactory.getLogger(GeoUtils.class);
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000;
    private static final int CONNECT_TIMEOUT_MS = 15000; // 15 seconds
    private static final int READ_TIMEOUT_MS = 15000; // 15 seconds

    public static String getCityFromCoordinates(double lat, double lon) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String urlString = "https://nominatim.openstreetmap.org/reverse?lat=" + lat +
                        "&lon=" + lon + "&format=json&zoom=14&addressdetails=1&accept-language=fr,en";
                logger.debug("Attempt {}: Requesting city from Nominatim: {}", attempt, urlString);

                URL url = new URL(urlString);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "PFE-Backend/1.0 (contact: nouha.abdelkader@esprit.tn)");
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);

                // Force TLS 1.2
                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, null, new SecureRandom());
                conn.setSSLSocketFactory(sslContext.getSocketFactory());

                Thread.sleep(1000); // Respect Nominatim's rate limit

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    logger.error("Attempt {}: Nominatim API error: HTTP {}", attempt, responseCode);
                    return "Geo error (HTTP " + responseCode + ")";
                }

                Scanner scanner = new Scanner(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
                }
                scanner.close();

                logger.debug("Attempt {}: Nominatim response: {}", attempt, response.toString());

                JSONObject json = new JSONObject(response.toString());
                if (!json.has("address")) {
                    logger.error("Attempt {}: No address found in Nominatim response", attempt);
                    return "Unknown city (no address)";
                }

                JSONObject address = json.getJSONObject("address");

                String city = null;
                if (address.has("city") && !address.isNull("city")) {
                    city = address.getString("city");
                } else if (address.has("town") && !address.isNull("town")) {
                    city = address.getString("town");
                } else if (address.has("village") && !address.isNull("village")) {
                    city = address.getString("village");
                } else if (address.has("municipality") && !address.isNull("municipality")) {
                    city = address.getString("municipality");
                } else if (address.has("suburb") && !address.isNull("suburb")) {
                    city = address.getString("suburb");
                } else if (address.has("county") && !address.isNull("county")) {
                    city = address.getString("county");
                } else if (address.has("state_district") && !address.isNull("state_district")) {
                    city = address.getString("state_district");
                } else if (address.has("state") && !address.isNull("state")) {
                    city = address.getString("state");
                }

                String cityLocalized = null;
                if (json.has("display_name") && !json.isNull("display_name")) {
                    String displayName = json.getString("display_name");
                    String[] parts = displayName.split(",");
                    if (parts.length > 0) {
                        cityLocalized = parts[0].trim();
                    }
                }

                if (cityLocalized != null && !cityLocalized.isEmpty()) {
                    city = cityLocalized;
                }

                if (city != null && !city.isEmpty()) {
                    return city;
                } else {
                    logger.error("Attempt {}: No city/town/village/municipality/suburb/county/state_district/state found. Address: {}",
                            attempt, address.toString());
                    return "Unknown city";
                }

            } catch (Exception e) {
                logger.error("Attempt {}: Error fetching city from Nominatim: {}", attempt, e.getMessage());
                if (attempt == MAX_RETRIES) {
                    logger.error("All retries failed: {}", e.getMessage(), e);
                    return "Geo error: " + e.getMessage();
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return "Geo error: Interrupted during retry";
                }
            }
        }
        return "Geo error: Max retries reached";
    }

    public static String getAddressFromCoordinates(double lat, double lon) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String urlString = "https://nominatim.openstreetmap.org/reverse?lat=" + lat +
                        "&lon=" + lon + "&format=json&zoom=18&addressdetails=1&accept-language=fr,en";
                logger.debug("Attempt {}: Requesting address from Nominatim: {}", attempt, urlString);

                URL url = new URL(urlString);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "PFE-Backend/1.0 (contact: nouha.abdelkader@esprit.tn)");
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);

                // Force TLS 1.2
                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, null, new SecureRandom());
                conn.setSSLSocketFactory(sslContext.getSocketFactory());

                Thread.sleep(1000); // Respect Nominatim's rate limit

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    logger.error("Attempt {}: Nominatim API error: HTTP {}", attempt, responseCode);
                    return "Geo error (HTTP " + responseCode + ")";
                }

                Scanner scanner = new Scanner(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
                }
                scanner.close();

                logger.debug("Attempt {}: Nominatim response: {}", attempt, response.toString());

                JSONObject json = new JSONObject(response.toString());
                if (!json.has("address")) {
                    logger.error("Attempt {}: No address found in Nominatim response", attempt);
                    return "Unknown address (no address)";
                }

                JSONObject address = json.getJSONObject("address");
                StringBuilder fullAddress = new StringBuilder();

                if (address.has("house_number") && !address.isNull("house_number")) {
                    fullAddress.append(address.getString("house_number")).append(" ");
                }
                if (address.has("road") && !address.isNull("road")) {
                    fullAddress.append(address.getString("road"));
                }

                String city = null;
                if (address.has("city") && !address.isNull("city")) {
                    city = address.getString("city");
                } else if (address.has("town") && !address.isNull("town")) {
                    city = address.getString("town");
                } else if (address.has("village") && !address.isNull("village")) {
                    city = address.getString("village");
                }
                if (city != null && !city.isEmpty()) {
                    if (fullAddress.length() > 0) fullAddress.append(", ");
                    fullAddress.append(city);
                }

                if (address.has("postcode") && !address.isNull("postcode")) {
                    if (fullAddress.length() > 0) fullAddress.append(", ");
                    fullAddress.append(address.getString("postcode"));
                }

                if (address.has("country") && !address.isNull("country")) {
                    if (fullAddress.length() > 0) fullAddress.append(", ");
                    fullAddress.append(address.getString("country"));
                }

                if (fullAddress.length() == 0 && json.has("display_name") && !json.isNull("display_name")) {
                    String displayName = json.getString("display_name");
                    fullAddress.append(displayName);
                }

                if (fullAddress.length() == 0) {
                    logger.error("Attempt {}: No detailed address found. Address: {}", attempt, address.toString());
                    return "Unknown address";
                }

                return fullAddress.toString();

            } catch (Exception e) {
                logger.error("Attempt {}: Error fetching address from Nominatim: {}", attempt, e.getMessage());
                if (attempt == MAX_RETRIES) {
                    logger.error("All retries failed: {}", e.getMessage(), e);
                    return "Geo error: " + e.getMessage();
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return "Geo error: Interrupted during retry";
                }
            }
        }
        return "Geo error: Max retries reached";
    }
}