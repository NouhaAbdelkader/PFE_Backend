package com.example.pfe_backend.controllers.ClientImpacteController;

 import com.example.pfe_backend.entities.ClientImpactes.ClientImpacte;
 import com.example.pfe_backend.services.clientService.clientService;
 import lombok.AllArgsConstructor;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.http.HttpStatus;
 import org.springframework.http.ResponseEntity;
 import org.springframework.web.bind.annotation.*;

 import java.util.Arrays;
 import java.util.List;

@RestController
@RequestMapping("/clients")
@AllArgsConstructor
public class ClientImpacteController {

    private  final clientService clientService;
    private static final Logger log = LoggerFactory.getLogger(ClientImpacteController.class);
    @GetMapping("/impactes")
    public ResponseEntity<List<ClientImpacte>> getClientImpactes() {
        List<ClientImpacte> clientImpactes = clientService.fetchClientImpactesFromApi();
        log.info("Returning {} client impactes", clientImpactes.size());
        return ResponseEntity.ok(clientImpactes);
    }

    @GetMapping("/impactes/cercle-equipement")
    public ResponseEntity<List<ClientImpacte>> getClientImpactesByCercleEquipement(
            @RequestParam("lat") Double lat,
            @RequestParam("long") Double longitude,
            @RequestParam("rayon") Double rayon) {
        try {
            log.info("Received request to fetch client impactes for lat={}, long={}, rayon={}", lat, longitude, rayon);
            List<ClientImpacte> clientImpactes = clientService.fetchClientImpactesFromCercleEquipement(lat, longitude, rayon);
            log.info("Returning {} client impactes", clientImpactes.size());
            return ResponseEntity.ok(clientImpactes);
        } catch (RuntimeException e) {
            log.error("Failed to fetch client impactes for lat={}, long={}, rayon={}: {}", lat, longitude, rayon, e.getMessage());
            return ResponseEntity.ok(List.of()); // Return empty list with 200 OK
        }
    }


    @GetMapping("/impactes/polygon-equipement")
    public ResponseEntity<List<ClientImpacte>> getClientImpactesByPolygonEquipement(

            @RequestParam("coordonnees") String coordonnees) {
        try {
            log.info("Received request to fetch client impactes for lat={}, long={}, coordonnees={}",   coordonnees);
            List<String> coordsList = Arrays.asList(coordonnees.split(";"));
            List<ClientImpacte> clientImpactes = clientService.fetchClientImpactesFromPolygonEquipement(  coordsList);
            log.info("Returning {} client impactes", clientImpactes.size());
            return ResponseEntity.ok(clientImpactes);
        } catch (RuntimeException e) {
            log.error("Failed to fetch client impactes for lat={}, long={}, coordonnees={}: {}",  coordonnees, e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }














    // POST /clients
    @PostMapping
    public ResponseEntity<ClientImpacte> addClient(@RequestBody ClientImpacte client) {
        try {
            log.info("Received request to add client: {}", client);
            ClientImpacte savedClient = clientService.addClient(client);
            log.info("Successfully added client with ID: {}", savedClient.getIdClient());
            return new ResponseEntity<>(savedClient, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            log.error("Failed to add client: {}", e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    // DELETE /clients/{idClient}
    @DeleteMapping("/{idClient}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long idClient) {
        try {
            log.info("Received request to delete client with ID: {}", idClient);
            clientService.deleteClient(idClient);
            log.info("Successfully deleted client with ID: {}", idClient);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            log.error("Failed to delete client with ID {}: {}", idClient, e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // PUT /clients/{idClient}
    @PutMapping("/{idClient}")
    public ResponseEntity<ClientImpacte> updateClient(@PathVariable Long idClient, @RequestBody ClientImpacte client) {
        try {
            log.info("Received request to update client with ID: {}", idClient);
            client.setIdClient(idClient); // Ensure ID is set for update
            ClientImpacte updatedClient = clientService.updateClient(client);
            log.info("Successfully updated client with ID: {}", idClient);
            return ResponseEntity.ok(updatedClient);
        } catch (RuntimeException e) {
            log.error("Failed to update client with ID {}: {}", idClient, e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    // GET /clients
    @GetMapping("/all")
    public ResponseEntity<List<ClientImpacte>> getAllClients() {
        try {
            log.info("Received request to fetch all clients");
            List<ClientImpacte> clients = clientService.getAllClients();
            log.info("Returning {} clients", clients.size());
            return ResponseEntity.ok(clients);
        } catch (RuntimeException e) {
            log.error("Failed to fetch all clients: {}", e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    // GET /clients/{idClient}
    @GetMapping("/{idClient}")
    public ResponseEntity<ClientImpacte> getClient(@PathVariable Long idClient) {
        try {
            log.info("Received request to fetch client with ID: {}", idClient);
            ClientImpacte client = clientService.getClient(idClient);
            if (client != null) {
                log.info("Successfully fetched client with ID: {}", idClient);
                return ResponseEntity.ok(client);
            } else {
                log.warn("Client with ID {} not found", idClient);
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }
        } catch (RuntimeException e) {
            log.error("Failed to fetch client with ID {}: {}", idClient, e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }
}
