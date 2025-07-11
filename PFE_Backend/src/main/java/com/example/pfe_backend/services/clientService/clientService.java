package com.example.pfe_backend.services.clientService;

import com.exalead.searchapi.client.SearchAPIClient;
import com.exalead.searchapi.client.SearchAPIClientFactory;
import com.exalead.searchapi.xmlv10.client.*;
import com.example.pfe_backend.entities.ClientImpactes.ClientImpacte;

import com.example.pfe_backend.repos.ClientRepo.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

@Service

public class clientService implements IClientService {
    private  final ClientRepository clientRepository;
    private static final Logger log = LoggerFactory.getLogger(clientService.class);
    private final SearchAPIClient client;
    private final SearchClient searchClient;
    private final String searchApiUrl;

    public clientService(ClientRepository clientRepository, @Value("${search.api.url:http://149.202.64.60:25010/search-api/search}") String searchApiUrl) {
        this.clientRepository = clientRepository;
        this.searchApiUrl = searchApiUrl;
        try {
            this.client = SearchAPIClientFactory.build(searchApiUrl);
            // Use SearchClientFactory as in the functional method
            this.searchClient = SearchClientFactory.createSearchClient(searchApiUrl, SearchAPIVersion.V6R2018X);
            log.info("Initialized SearchClient with URL: {}", searchApiUrl);
        } catch (MalformedURLException e) {
            log.error("Invalid Search API URL: {}", searchApiUrl, e);
            throw new RuntimeException("Invalid Search API URL: " + searchApiUrl, e);
        }
    }

    @Override
    public List<ClientImpacte> fetchClientImpactesFromApi() {
        List<ClientImpacte> clientImpactes = new ArrayList<>();
        try {
            log.info("Fetching client impactes from API: {}", searchApiUrl);
            SearchQuery sq = new SearchQuery("class:(person)"); // Fixed query syntax
            sq.addParameter(SearchParameter.NRESULTS, "-1"); // -1 pour récuperer tout les clients
            sq.addParameter("applicationId", "default"); // Add per documentation
            log.info("Executing search query: {}", sq.getQuery());
            SearchAnswer sa = searchClient.getResults(sq);
            List<Hit> hits = sa.getHits();
            if (!hits.isEmpty()) {
                for (Hit hit : hits) {
                    ClientImpacte clientImpacte = new ClientImpacte();
                    if (hit.getMeta("lastname") != null && !hit.getMeta("lastname").getStringValue().isEmpty()) {
                        clientImpacte.setNom(hit.getMeta("lastname").getStringValue());
                    }
                    if (hit.getMeta("firstname") != null && !hit.getMeta("firstname").getStringValue().isEmpty()) {
                        clientImpacte.setPrenom(hit.getMeta("firstname").getStringValue());
                    }
                    if (hit.getMeta("email") != null && !hit.getMeta("email").getStringValue().isEmpty()) {
                        clientImpacte.setEmail(hit.getMeta("email").getStringValue());
                    }
                    if (hit.getMeta("phonenumber") != null && !hit.getMeta("phonenumber").getStringValue().isEmpty()) {
                        clientImpacte.setNumero(hit.getMeta("phonenumber").getStringValue());
                    }
                    if (hit.getMeta("latitude") != null && !hit.getMeta("latitude").getStringValue().isEmpty()) {
                        clientImpacte.setLatitude(hit.getMeta("latitude").getStringValue());
                    }
                    if (hit.getMeta("longitude") != null && !hit.getMeta("longitude").getStringValue().isEmpty()) {
                        clientImpacte.setLongitude(hit.getMeta("longitude").getStringValue());
                    }
                    if (hit.getMeta("gpspoint") != null && !hit.getMeta("gpspoint").getStringValue().isEmpty()) {
                        clientImpacte.setGpspoint(hit.getMeta("gpspoint").getStringValue());
                    }
                    if (hit.getMeta("city") != null && !hit.getMeta("city").getStringValue().isEmpty()) {
                        clientImpacte.setVille(hit.getMeta("city").getStringValue());
                    }
                    if (hit.getMeta("street") != null && !hit.getMeta("street").getStringValue().isEmpty()) {
                        clientImpacte.setAdresse(hit.getMeta("street").getStringValue());
                    }
                    if (hit.getMeta("customerid") != null && !hit.getMeta("customerid").getStringValue().isEmpty()) {
                        clientImpacte.setClientRef(hit.getMeta("customerid").getStringValue());
                    }
                    clientImpactes.add(clientImpacte);
                    log.info("ClientImpacte added: {}", clientImpacte); // Log each ClientImpacte
                }
                log.info("Total ClientImpactes fetched: {}", clientImpactes.size());
            } else {
                log.info("No hits found from API");
            }
        } catch (SearchClientException e) {
            log.error("Failed to fetch client impactes from API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch client impactes from API", e);
        }
        return clientImpactes;
    }

    @Override
    public List<ClientImpacte> fetchClientImpactesFromPolygonEquipement(  List<String> coordonnees) {
        List<ClientImpacte> clientImpactes = new ArrayList<>();
        try {
            log.info("Fetching client impactes from API: {}", searchApiUrl);
            if (coordonnees == null || coordonnees.size() < 3) {
                log.warn("Invalid polygon coordinates: at least 3 points required, got {}", coordonnees);
                return clientImpactes;
            }

            // Build geo:WITHIN query: geo:WITHIN(lat1,lng1; lat2,lng2; ...; lat1,lng1)
            StringBuilder polygon = new StringBuilder();
            for (int i = 0; i < coordonnees.size(); i++) {
                String coord = coordonnees.get(i).trim();
                String[] parts = coord.split("\\s*,\\s*");
                if (parts.length != 2) {
                    log.warn("Invalid coordinate format: {}", coord);
                    return clientImpactes;
                }
                try {
                    double coordLat = Double.parseDouble(parts[0]);
                    double coordLon = Double.parseDouble(parts[1]);
                    polygon.append(coordLat).append(",").append(coordLon);
                    if (i < coordonnees.size() - 1) {
                        polygon.append("; ");
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid coordinate values: {}", coord, e);
                    return clientImpactes;
                }
            }
            // Close the polygon by repeating the first point
            String firstCoord = coordonnees.get(0).trim();
            String[] firstParts = firstCoord.split("\\s*,\\s*");
            polygon.append("; ").append(firstParts[0]).append(",").append(firstParts[1]);
            String geoQuery = String.format("geo:WITHIN(%s)", polygon);
            SearchQuery sq = new SearchQuery(geoQuery);
            sq.addParameter(SearchParameter.NRESULTS, "-1");
            sq.addParameter("applicationId", "default");
            log.info("Executing search query: {}", sq.getQuery());
            SearchAnswer sa = searchClient.getResults(sq);
            List<Hit> hits = sa.getHits();
            if (!hits.isEmpty()) {
                for (Hit hit : hits) {
                    ClientImpacte clientImpacte = new ClientImpacte();
                    if (hit.getMeta("lastname") != null && !hit.getMeta("lastname").getStringValue().isEmpty()) {
                        clientImpacte.setNom(hit.getMeta("lastname").getStringValue().trim());
                    }
                    if (hit.getMeta("firstname") != null && !hit.getMeta("firstname").getStringValue().isEmpty()) {
                        clientImpacte.setPrenom(hit.getMeta("firstname").getStringValue().trim());
                    }
                    if (hit.getMeta("email") != null && !hit.getMeta("email").getStringValue().isEmpty()) {
                        clientImpacte.setEmail(hit.getMeta("email").getStringValue().trim());
                    }
                    if (hit.getMeta("phonenumber") != null && !hit.getMeta("phonenumber").getStringValue().isEmpty()) {
                        clientImpacte.setNumero(hit.getMeta("phonenumber").getStringValue().trim());
                    }
                    if (hit.getMeta("latitude") != null && !hit.getMeta("latitude").getStringValue().isEmpty()) {
                        clientImpacte.setLatitude(hit.getMeta("latitude").getStringValue().trim());
                    }
                    if (hit.getMeta("longitude") != null && !hit.getMeta("longitude").getStringValue().isEmpty()) {
                        clientImpacte.setLongitude(hit.getMeta("longitude").getStringValue().trim());
                    }
                    if (hit.getMeta("gpspoint") != null && !hit.getMeta("gpspoint").getStringValue().isEmpty()) {
                        clientImpacte.setGpspoint(hit.getMeta("gpspoint").getStringValue().trim());
                    }
                    if (hit.getMeta("city") != null && !hit.getMeta("city").getStringValue().isEmpty()) {
                        clientImpacte.setVille(hit.getMeta("city").getStringValue().trim());
                    }
                    if (hit.getMeta("street") != null && !hit.getMeta("street").getStringValue().isEmpty()) {
                        clientImpacte.setAdresse(hit.getMeta("street").getStringValue().trim());
                    }
                    if (hit.getMeta("customerid") != null && !hit.getMeta("customerid").getStringValue().isEmpty()) {
                        clientImpacte.setClientRef(hit.getMeta("customerid").getStringValue().trim());
                    }
                    clientImpactes.add(clientImpacte);
                    log.info("ClientImpacte added: {}", clientImpacte);
                }
                log.info("Total ClientImpactes fetched: {}", clientImpactes.size());
            } else {
                log.info("No hits found from API");
            }
        } catch (SearchClientException e) {
            log.error("Failed to fetch client impactes from API: {}", e.getMessage(), e);
            log.info("Returning empty list due to API failure");
        }
        return clientImpactes;
    }

    @Override
    public List<ClientImpacte> fetchClientImpactesFromCercleEquipement(Double lat, Double Long, Double rayon) {
        List<ClientImpacte> clientImpactes = new ArrayList<>();
        try {
            log.info("Fetching client impactes from API: {}", searchApiUrl);
            SearchQuery sq = new SearchQuery("geo:(DISTANCE("+lat+", "+Long+", "+rayon+"))"); // Fixed query syntax
            sq.addParameter(SearchParameter.NRESULTS, "-1");  // -1 pour récuperer tout les clients
            sq.addParameter("applicationId", "default"); // Add per documentation
            log.info("Executing search query: {}", sq.getQuery());
            SearchAnswer sa = searchClient.getResults(sq);
            List<Hit> hits = sa.getHits();
            if (!hits.isEmpty()) {
                for (Hit hit : hits) {
                    ClientImpacte clientImpacte = new ClientImpacte();
                    if (hit.getMeta("lastname") != null && !hit.getMeta("lastname").getStringValue().isEmpty()) {
                        clientImpacte.setNom(hit.getMeta("lastname").getStringValue());
                    }
                    if (hit.getMeta("firstname") != null && !hit.getMeta("firstname").getStringValue().isEmpty()) {
                        clientImpacte.setPrenom(hit.getMeta("firstname").getStringValue());
                    }
                    if (hit.getMeta("email") != null && !hit.getMeta("email").getStringValue().isEmpty()) {
                        clientImpacte.setEmail(hit.getMeta("email").getStringValue());
                    }
                    if (hit.getMeta("phonenumber") != null && !hit.getMeta("phonenumber").getStringValue().isEmpty()) {
                        clientImpacte.setNumero(hit.getMeta("phonenumber").getStringValue());
                    }
                    if (hit.getMeta("latitude") != null && !hit.getMeta("latitude").getStringValue().isEmpty()) {
                        clientImpacte.setLatitude(hit.getMeta("latitude").getStringValue());
                    }
                    if (hit.getMeta("longitude") != null && !hit.getMeta("longitude").getStringValue().isEmpty()) {
                        clientImpacte.setLongitude(hit.getMeta("longitude").getStringValue());
                    }
                    if (hit.getMeta("gpspoint") != null && !hit.getMeta("gpspoint").getStringValue().isEmpty()) {
                        clientImpacte.setGpspoint(hit.getMeta("gpspoint").getStringValue());
                    }
                    if (hit.getMeta("city") != null && !hit.getMeta("city").getStringValue().isEmpty()) {
                        clientImpacte.setVille(hit.getMeta("city").getStringValue());
                    }
                    if (hit.getMeta("street") != null && !hit.getMeta("street").getStringValue().isEmpty()) {
                        clientImpacte.setAdresse(hit.getMeta("street").getStringValue());
                    }
                    if (hit.getMeta("customerid") != null && !hit.getMeta("customerid").getStringValue().isEmpty()) {
                        clientImpacte.setClientRef(hit.getMeta("customerid").getStringValue());
                    }
                    clientImpactes.add(clientImpacte);
                    log.info("ClientImpacte added: {}", clientImpacte); // Log each ClientImpacte
                }
                log.info("Total ClientImpactes fetched: {}", clientImpactes.size());
            } else {
                log.info("No hits found from API");
            }
        } catch (SearchClientException e) {
            log.error("Failed to fetch client impactes from API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch client impactes from API", e);
        }
        return clientImpactes;
    }



    @Override
    public ClientImpacte addClient(ClientImpacte client) {
        return clientRepository.save(client);
    }

    @Override
    public void deleteClient(Long idClient) {
    }

    @Override
    public ClientImpacte updateClient(ClientImpacte c) {
        return null;
    }

    @Override
    public List<ClientImpacte> getAllClients() {
        return clientRepository.findAll();
    }

    @Override
    public ClientImpacte getClient(Long idClient) {
        return null;
    }
}