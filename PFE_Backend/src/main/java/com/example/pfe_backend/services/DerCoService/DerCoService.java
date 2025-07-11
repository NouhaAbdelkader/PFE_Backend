package com.example.pfe_backend.services.DerCoService;

import com.example.pfe_backend.entities.ClientImpactes.ClientImpacte;
import com.example.pfe_backend.entities.DerCo.*;
import com.example.pfe_backend.entities.Equipements.CategoryEquipement;
import com.example.pfe_backend.entities.Equipements.Equipement;
import com.example.pfe_backend.entities.Equipements.EtatEquipement;
import com.example.pfe_backend.entities.Equipements.TypeEquipement;
import com.example.pfe_backend.repos.ClientRepo.ClientRepository;
import com.example.pfe_backend.repos.DerCoRepo.DerCoRepository;
import com.example.pfe_backend.repos.DerCoRepo.ZoneRepository;
import com.example.pfe_backend.repos.EquipementRepo.EquipementRepo;
import com.example.pfe_backend.services.clientService.clientService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
@AllArgsConstructor
public class DerCoService implements IDerCoService {

    private static final Logger logger = LoggerFactory.getLogger(DerCoService.class);

    private final DerCoRepository dercoRepo;
    private final EquipementRepo equipementRepo;
    private final ZoneRepository zoneRepo;
    private final ClientRepository clientRepository;
    private final clientService clientService;

    /**
     * Creates multiple DerCo entries from a list of DercoCsvDTO objects, associating each with impacted
     */
    @Override
    @Transactional
    public void addDerCo(List<DercoCsvDTO> dercoCsvList) {
        for (DercoCsvDTO dto : dercoCsvList) {
            try {
                // Find equipment by reference
                Optional<Equipement> equipementOpt = equipementRepo.findByRefEquipement(dto.getRefEquipement());
                if (equipementOpt.isEmpty()) {
                    logger.warn("Equipment {} not found, Derco creation skipped.", dto.getRefEquipement());
                    continue;
                }

                Equipement equipement = equipementOpt.get();

                // Check for existing Derco to avoid duplicates
                String description = dto.getDescriptionPanne() != null ? dto.getDescriptionPanne() : "Missing description";
                Optional<Derco> existingDerco = dercoRepo.findByEquipementImpacte_EtatEquipement(
                        equipement.getIdEquipement(), EtatEquipement.NONFONCTIONNEL
                );

                if (existingDerco.isPresent()) {
                    logger.info("A Derco already exists for equipment {} with description '{}'. Creation skipped.",
                            dto.getRefEquipement(), description);
                    continue;
                }

                // Configure Derco entity with DTO and equipment data
                Derco derco = configureDerco(dto, equipement);

                // Set equipment status to NONFONCTIONNEL
                equipement.setEtatEquipement(EtatEquipement.NONFONCTIONNEL);

                // Fetch impacted clients based on equipment type (CERCLE or POLYGONE)
                List<ClientImpacte> impactedClients = new ArrayList<>();
                try {
                    TypeEquipement type = equipement.getTypeEquipement();
                    if (type == null) {
                        logger.error("Equipment type is null for equipment: {}", equipement.getRefEquipement());
                        throw new IllegalArgumentException("Equipment type is null");
                    }
                    if (type == TypeEquipement.CERCLE) {
                        if (equipement.getLatitude() == null || equipement.getLongitude() == null || equipement.getRayon() == null) {
                            logger.warn("Invalid circle equipment data: lat={}, lng={}, rayon={}",
                                    equipement.getLatitude(), equipement.getLongitude(), equipement.getRayon());
                            throw new IllegalArgumentException("Invalid circle equipment data");
                        }
                        impactedClients = clientService.fetchClientImpactesFromCercleEquipement(
                                equipement.getLatitude(), equipement.getLongitude(), equipement.getRayon()
                        );
                    } else if (type == TypeEquipement.POLYGONE) {
                        if (equipement.getCoordonnees() == null || equipement.getCoordonnees().isEmpty()) {
                            logger.warn("Invalid polygon equipment data: coordonnees={}", equipement.getCoordonnees());
                            throw new IllegalArgumentException("Invalid polygon equipment data");
                        }
                        impactedClients = clientService.fetchClientImpactesFromPolygonEquipement(equipement.getCoordonnees());
                    } else {
                        logger.warn("Unsupported equipment type: {}", type);
                        throw new IllegalArgumentException("Unsupported equipment type: " + type);
                    }
                } catch (IllegalArgumentException e) {
                    logger.error("Invalid equipment type format: {}", equipement.getTypeEquipement(), e);
                    throw new IllegalArgumentException("Invalid equipment type format: " + equipement.getTypeEquipement(), e);
                }

                // Process and save impacted clients
                List<ClientImpacte> savedClients = new ArrayList<>();
                for (ClientImpacte client : impactedClients) {
                    Optional<ClientImpacte> existingClientOpt = client.getClientRef() != null
                            ? clientRepository.findByClientRef(client.getClientRef())
                            : clientRepository.findByEmail(client.getEmail());

                    ClientImpacte clientToSave;
                    if (existingClientOpt.isPresent()) {
                        clientToSave = existingClientOpt.get();
                        logger.info("Using existing client: clientRef={}, email={}",
                                clientToSave.getClientRef(), clientToSave.getEmail());
                    } else {
                        try {
                            if (client.getLatitude() != null && !client.getLatitude().isEmpty()) {
                                client.setLatitude(client.getLatitude());
                            } else {
                                client.setLatitude(null);
                            }
                            if (client.getLongitude() != null && !client.getLongitude().isEmpty()) {
                                client.setLongitude(client.getLongitude());
                            } else {
                                client.setLongitude(null);
                            }
                            client.setDetectedAt(new Date()); // Set detection time for new clients
                            clientToSave = clientRepository.save(client);
                            logger.info("Saved new client: clientRef={}, email={}",
                                    clientToSave.getClientRef(), clientToSave.getEmail());
                        } catch (NumberFormatException e) {
                            logger.warn("Invalid coordinates for client {} {}: lat={}, lng={}",
                                    client.getNom(), client.getPrenom(), client.getLatitude(), client.getLongitude());
                            continue; // Skip clients with invalid coordinates
                        }
                    }
                    savedClients.add(clientToSave);
                }

                // Assign saved clients to Derco
                derco.setClients(savedClients);
                logger.info("Associated {} clients to Derco for equipment {}", savedClients.size(), dto.getRefEquipement());

                // Set Derco status to ACTIVE
                derco.setStatus(status.ACTIVE);

                // Save equipment and Derco
                equipementRepo.save(equipement);
                dercoRepo.save(derco);
                logger.info("Derco created for equipment {} with {} impacted clients",
                        dto.getRefEquipement(), savedClients.size());
            } catch (Exception e) {
                logger.error("Error while creating Derco for equipment {}: {}",
                        dto.getRefEquipement(), e.getMessage(), e);
            }
        }
    }

    /**
     * Retrieves all DerCo entities from the database, ordered in reverse (most recent first).

     */
    @Override
    public List<Derco> getAll() {
        try {
            List<Derco> dercos = dercoRepo.findAll();
            Collections.reverse(dercos);
            logger.info("Retrieved all Dercos: {} found", dercos.size());
            return dercos;
        } catch (Exception e) {
            logger.error("Error while retrieving Dercos: {}", e.getMessage(), e);
            throw new RuntimeException("Error while retrieving Dercos", e);
        }
    }

    /**
     * Updates an existing DerCo entity with new details and regenerates its script.
     */
    @Override
    public Derco updateDerco(Derco updatedDerco) {
        try {
            // Find the existing DerCo entity by ID
            Optional<Derco> existingDercoOpt = dercoRepo.findById(updatedDerco.getIdDerCo());
            if (existingDercoOpt.isEmpty()) {
                logger.error("Derco with ID {} not found for update", updatedDerco.getIdDerCo());
                throw new RuntimeException("Derco not found with ID: " + updatedDerco.getIdDerCo());
            }

            Derco existingDerco = existingDercoOpt.get();

            // Update fields
            existingDerco.setNomDerCo(updatedDerco.getNomDerCo());
            existingDerco.setDescription(updatedDerco.getDescription());
            existingDerco.setDateDetection(updatedDerco.getDateDetection());
            existingDerco.setDelaiPrevisionnel(updatedDerco.getDelaiPrevisionnel());
            existingDerco.setDateResolutionPrevue(updatedDerco.getDateResolutionPrevue());
            existingDerco.setDateResolutionReelle(updatedDerco.getDateResolutionReelle());
            existingDerco.setGraviteDerCo(updatedDerco.getGraviteDerCo());
            existingDerco.setPriorite(updatedDerco.getPriorite());
            existingDerco.setServicesImpactes(updatedDerco.getServicesImpactes());
            existingDerco.setEquipementImpacte(updatedDerco.getEquipementImpacte());
            existingDerco.setZoneAffectee(updatedDerco.getZoneAffectee());

            // Regenerate script based on updated data
            String typePanne = inferTypePanneFromDescription(existingDerco.getDescription());
            String newScript = generateScript(
                    existingDerco.getDescription(),
                    typePanne,
                    existingDerco.getServicesImpactes(),
                    existingDerco.getDateResolutionPrevue()
            );
            existingDerco.setScript(newScript);

            // Save the updated DerCo
            Derco savedDerco = dercoRepo.save(existingDerco);
            logger.info("Derco with ID {} updated successfully with script: {}", savedDerco.getIdDerCo(), savedDerco.getScript());
            return savedDerco;
        } catch (Exception e) {
            logger.error("Error while updating Derco with ID {}: {}", updatedDerco.getIdDerCo(), e.getMessage(), e);
            throw new RuntimeException("Error while updating Derco", e);
        }
    }

    /**
     * Creates a new DerCo entity with validated fields and associates it with impacted clients and equipment.
     */
    @Override
    @Transactional
    public Derco newDerCo(@Valid @NotNull Derco derco) {
        try {
            logger.info("Received Derco: nomDerCo={}, description={}, servicesImpactes={}, dateDetection={}, dateResolutionPrevue={}",
                    derco.getNomDerCo(), derco.getDescription(), derco.getServicesImpactes(),
                    derco.getDateDetection(), derco.getDateResolutionPrevue());

            // Validate required fields
            if (derco.getNomDerCo() == null || derco.getNomDerCo().isEmpty()) {
                throw new IllegalArgumentException("DerCo name is required");
            }
            if (derco.getDescription() == null || derco.getDescription().isEmpty()) {
                throw new IllegalArgumentException("Description is required");
            }
            if (derco.getServicesImpactes() == null || derco.getServicesImpactes().isEmpty()) {
                throw new IllegalArgumentException("Impacted services are required");
            }
            if (derco.getDateDetection() == null) {
                throw new IllegalArgumentException("Detection date is required");
            }
            if (derco.getDateResolutionPrevue() == null) {
                throw new IllegalArgumentException("Estimated resolution date is required");
            }
            if (derco.getGraviteDerCo() == null) {
                throw new IllegalArgumentException("Severity is required");
            }
            if (derco.getPriorite() == null) {
                throw new IllegalArgumentException("Priority is required");
            }
            if (derco.getEquipementImpacte() == null || derco.getEquipementImpacte().getRefEquipement() == null) {
                throw new IllegalArgumentException("Equipment reference is required");
            }
            if (derco.getScript() == null || derco.getScript().isEmpty()) {
                throw new IllegalArgumentException("Script is required");
            }

            // Load equipment from database
            Optional<Equipement> equipementOpt = equipementRepo.findByRefEquipement(
                    derco.getEquipementImpacte().getRefEquipement()
            );
            if (equipementOpt.isEmpty()) {
                throw new IllegalArgumentException("Equipment not found: " + derco.getEquipementImpacte().getRefEquipement());
            }
            Equipement equipement = equipementOpt.get();
            derco.setEquipementImpacte(equipement);
            equipement.setEtatEquipement(EtatEquipement.NONFONCTIONNEL);

            // Fetch impacted clients based on equipment type
            List<ClientImpacte> impactedClients = new ArrayList<>();
            try {
                TypeEquipement type = equipement.getTypeEquipement();
                if (type == null) {
                    logger.error("Equipment type is null for equipment: {}", equipement.getRefEquipement());
                    throw new IllegalArgumentException("Equipment type is null");
                }
                if (type == TypeEquipement.CERCLE) {
                    if (equipement.getLatitude() == null || equipement.getLongitude() == null || equipement.getRayon() == null) {
                        logger.warn("Invalid circle equipment data: lat={}, lng={}, rayon={}",
                                equipement.getLatitude(), equipement.getLongitude(), equipement.getRayon());
                        throw new IllegalArgumentException("Invalid circle equipment data");
                    }
                    impactedClients = clientService.fetchClientImpactesFromCercleEquipement(
                            equipement.getLatitude(), equipement.getLongitude(), equipement.getRayon()
                    );
                } else if (type == TypeEquipement.POLYGONE) {
                    if (equipement.getCoordonnees() == null || equipement.getCoordonnees().isEmpty()) {
                        logger.warn("Invalid polygon equipment data: coordonnees={}", equipement.getCoordonnees());
                        throw new IllegalArgumentException("Invalid polygon equipment data");
                    }
                    impactedClients = clientService.fetchClientImpactesFromPolygonEquipement(equipement.getCoordonnees());
                } else {
                    logger.warn("Unsupported equipment type: {}", type);
                    throw new IllegalArgumentException("Unsupported equipment type: " + type);
                }
            } catch (IllegalArgumentException e) {
                logger.error("Invalid equipment type format: {}", equipement.getTypeEquipement(), e);
                throw new IllegalArgumentException("Invalid equipment type format: " + equipement.getTypeEquipement(), e);
            }

            // Process and save clients
            List<ClientImpacte> savedClients = new ArrayList<>();
            for (ClientImpacte client : impactedClients) {
                Optional<ClientImpacte> existingClientOpt = client.getIdClient() != null
                        ? clientRepository.findByIdClient(client.getIdClient())
                        : Optional.empty();

                ClientImpacte clientToSave;
                if (existingClientOpt.isPresent()) {
                    clientToSave = existingClientOpt.get();
                    logger.info("Using existing client: email={}, idClient={}", clientToSave.getEmail(), clientToSave.getIdClient());
                } else {
                    try {
                        if (client.getLatitude() != null && !client.getLatitude().isEmpty()) {
                            client.setLatitude(client.getLatitude());
                        } else {
                            client.setLatitude(null);
                        }
                        if (client.getLongitude() != null && !client.getLongitude().isEmpty()) {
                            client.setLongitude(client.getLongitude());
                        } else {
                            client.setLongitude(null);
                        }
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid coordinates for client {} {}: lat={}, lng={}",
                                client.getNom(), client.getPrenom(), client.getLatitude(), client.getLongitude());
                        continue; // Skip clients with invalid coordinates
                    }
                    clientToSave = clientRepository.save(client);
                    logger.info("Saved new client: email={}, idClient={}", clientToSave.getEmail(), clientToSave.getIdClient());
                }
                savedClients.add(clientToSave);
            }

            // Assign saved clients to Derco
            derco.setClients(savedClients);
            logger.info("Associated {} clients to Derco", savedClients.size());

            // Handle affected zone
            Zone zoneAffectee;
            String nomZone = equipement.getRefEquipement();
            Optional<Zone> existingZoneOpt = zoneRepo.findByNomZone(nomZone);
            if (existingZoneOpt.isPresent()) {
                zoneAffectee = existingZoneOpt.get();
                logger.info("Reusing existing zone: nomZone={}", zoneAffectee.getNomZone());
            } else {
                zoneAffectee = new Zone();
                zoneAffectee.setNomZone(nomZone);
                zoneAffectee.setVille(equipement.getVille());
                zoneAffectee.setDescription("Zone created for equipment " + equipement.getRefEquipement());
                zoneAffectee.setLatitude(equipement.getLatitude());
                zoneAffectee.setLongitude(equipement.getLongitude());
                zoneAffectee.setRayon(equipement.getRayon());
                zoneAffectee.setCoordonnees(equipement.getCoordonnees() != null ? new ArrayList<>(equipement.getCoordonnees()) : new ArrayList<>());
                logger.info("Creating new zone: nomZone={}", zoneAffectee.getNomZone());
            }

            // Save equipment and zone
            equipementRepo.save(equipement);
            zoneRepo.save(zoneAffectee);
            derco.setZoneAffectee(zoneAffectee);
            derco.setStatus(status.ACTIVE);

            // Calculate estimated resolution delay
            long diffInMillies = Math.abs(derco.getDateResolutionPrevue().getTime() - derco.getDateDetection().getTime());
            long heuresDelai = TimeUnit.MILLISECONDS.toHours(diffInMillies);
            derco.setDelaiPrevisionnel(heuresDelai + "h");

            // Save the Derco
            Derco savedDerco = dercoRepo.save(derco);
            logger.info("New Derco added with ID {}, delaiPrevisionnel={}", savedDerco.getIdDerCo(), savedDerco.getDelaiPrevisionnel());
            return savedDerco;
        } catch (Exception e) {
            logger.error("Error while adding new Derco: {}", e.getMessage(), e);
            throw new RuntimeException("Error while adding new Derco", e);
        }
    }

    /**
     * Scheduled task that runs hourly to update active DerCo entities by adding new impacted clients.
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void ForActiveDercos() {
        logger.info("Starting scheduled task to update clients for active Dercos");

        // Retrieve all active DerCo entities
        List<Derco> activeDercos = dercoRepo.findByStatus(status.ACTIVE);
        logger.info("Found {} active Dercos", activeDercos.size());

        for (Derco derco : activeDercos) {
            try {
                // Get associated equipment
                Equipement equipement = derco.getEquipementImpacte();
                if (equipement == null) {
                    logger.warn("No equipment associated with Derco ID {}", derco.getIdDerCo());
                    continue;
                }

                // Fetch impacted clients based on equipment type
                List<ClientImpacte> fetchedClients = new ArrayList<>();
                if (equipement.getTypeEquipement() == TypeEquipement.CERCLE) {
                    if (equipement.getLatitude() != null && equipement.getLongitude() != null && equipement.getRayon() != null) {
                        fetchedClients = clientService.fetchClientImpactesFromCercleEquipement(
                                equipement.getLatitude(), equipement.getLongitude(), equipement.getRayon()
                        );
                    } else {
                        logger.warn("Invalid circle equipment data for Derco ID {}: lat={}, lng={}, rayon={}",
                                derco.getIdDerCo(), equipement.getLatitude(), equipement.getLongitude(), equipement.getRayon());
                        continue;
                    }
                } else if (equipement.getTypeEquipement() == TypeEquipement.POLYGONE) {
                    if (equipement.getCoordonnees() != null && !equipement.getCoordonnees().isEmpty()) {
                        fetchedClients = clientService.fetchClientImpactesFromPolygonEquipement(equipement.getCoordonnees());
                    } else {
                        logger.warn("Invalid polygon equipment data for Derco ID {}: coordonnees={}",
                                derco.getIdDerCo(), equipement.getCoordonnees());
                        continue;
                    }
                } else {
                    logger.warn("Unsupported equipment type for Derco ID {}: {}", derco.getIdDerCo(), equipement.getTypeEquipement());
                    continue;
                }

                // Process fetched clients and add new ones
                List<ClientImpacte> existingClients = derco.getClients();
                List<ClientImpacte> newClients = new ArrayList<>();
                for (ClientImpacte fetchedClient : fetchedClients) {
                    Optional<ClientImpacte> existingClientOpt = clientRepository.findByClientRef(fetchedClient.getClientRef())
                            .or(() -> clientRepository.findByEmail(fetchedClient.getEmail()));

                    ClientImpacte clientToAssign;
                    if (existingClientOpt.isPresent()) {
                        clientToAssign = existingClientOpt.get();
                        logger.info("Found existing client: clientRef={}, email={}", clientToAssign.getClientRef(), clientToAssign.getEmail());
                    } else {
                        fetchedClient.setDetectedAt(new Date());
                        try {
                            clientToAssign = clientService.addClient(fetchedClient);
                            logger.info("Saved new client: clientRef={}, email={}, detectedAt={}",
                                    clientToAssign.getClientRef(), clientToAssign.getEmail(), clientToAssign.getDetectedAt());
                        } catch (Exception e) {
                            logger.warn("Failed to save new client: clientRef={}, error={}", fetchedClient.getClientRef(), e.getMessage());
                            continue;
                        }
                    }

                    if (!existingClients.contains(clientToAssign)) {
                        newClients.add(clientToAssign);
                    }
                }

                // Update Derco with new clients if any
                if (!newClients.isEmpty()) {
                    existingClients.addAll(newClients);
                    derco.setClients(existingClients);
                    dercoRepo.save(derco);
                    logger.info("Added {} new clients to Derco ID {}", newClients.size(), derco.getIdDerCo());
                } else {
                    logger.info("No new clients found for Derco ID {}", derco.getIdDerCo());
                }

            } catch (Exception e) {
                logger.error("Error updating clients for Derco ID {}: {}", derco.getIdDerCo(), e.getMessage(), e);
            }
        }
        logger.info("Completed scheduled task to update clients for active Dercos");
    }

    /**
     * Retrieves DerCo entities that have no associated interventions.
     */
    @Override
    public List<Derco> newDercos() {
        List<Derco> allDercos = dercoRepo.findAll();
        return allDercos.stream()
                .filter(derco -> derco.getInterventions() == null || derco.getInterventions().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Deletes a DerCo entity by ID, clearing associated clients and updating equipment status.
     */
    @Override
    @Transactional
    public void deleteDerco(Long id) {
        try {
            logger.info("Attempting to delete Derco with ID {}", id);

            // Verify Derco exists
            Optional<Derco> dercoOpt = dercoRepo.findById(id);
            if (dercoOpt.isEmpty()) {
                logger.error("Derco with ID {} not found", id);
                throw new IllegalArgumentException("Derco not found with ID: " + id);
            }
            Derco derco = dercoOpt.get();

            // Clear client relationships
            if (derco.getClients() != null && !derco.getClients().isEmpty()) {
                logger.info("Removing {} clients from Derco ID {}", derco.getClients().size(), id);
                derco.getClients().clear();
            }

            // Update equipment status to functional
            if (derco.getEquipementImpacte() != null) {
                Equipement equipement = derco.getEquipementImpacte();
                logger.info("Updating equipment {} for Derco ID {}", equipement.getRefEquipement(), id);
                equipement.setEtatEquipement(EtatEquipement.FONCTIONNEL);
                derco.setEquipementImpacte(null);
                equipementRepo.save(equipement);
            }

            // Save Derco to persist relationship changes
            dercoRepo.save(derco);
            logger.info("Relationships cleared for Derco ID {}", id);

            // Delete the Derco
            dercoRepo.delete(derco);
            logger.info("Derco with ID {} deleted successfully", id);

        } catch (Exception e) {
            logger.error("Error while deleting Derco with ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error while deleting Derco: " + e.getMessage(), e);
        }
    }

    /**
     * Configures a new DerCo entity from a DercoCsvDTO and associated equipment, setting fields like

     */
    private Derco configureDerco(DercoCsvDTO dto, Equipement equipement) {
        Derco derco = new Derco();

        // Set detection date, defaulting to current date if null
        derco.setDateDetection(dto.getDateDetection() != null ? dto.getDateDetection() : new Date());
        logger.info("Detection date for equipment {}: {}", dto.getRefEquipement(), dto.getDateDetection());

        // Set description, defaulting to "Missing description" if null
        derco.setDescription(dto.getDescriptionPanne() != null ? dto.getDescriptionPanne() : "Missing description");

        // Set impacted services, defaulting to "Unspecified services" if null
        String impactedServices = dto.getServicesImpactes() != null ? dto.getServicesImpactes() : "Unspecified services";
        derco.setServicesImpactes(impactedServices);

        derco.setEquipementImpacte(equipement);
        derco.setNomDerCo("Issue for " + dto.getRefEquipement());

        // Configure associated zone
        Optional<Zone> zoneOpt = zoneRepo.findByNomZone(dto.getRefEquipement());
        Zone zone;
        if (zoneOpt.isPresent()) {
            zone = zoneOpt.get();
            logger.info("Existing zone found for nomZone {}: ID {}", dto.getRefEquipement(), zone.getIdZone());
            zone.setLatitude(equipement.getLatitude());
            zone.setLongitude(equipement.getLongitude());
            zone.setRayon(equipement.getRayon());
            zone.setCoordonnees(equipement.getCoordonnees() != null ? new ArrayList<>(equipement.getCoordonnees()) : new ArrayList<>());
            zone.setVille(equipement.getVille());
            zoneRepo.save(zone);
        } else {
            logger.info("Zone with nomZone {} not found, creating a new zone.", dto.getRefEquipement());
            zone = new Zone();
            zone.setNomZone(dto.getRefEquipement());
            zone.setVille(equipement.getVille());
            zone.setDescription("Zone created for equipment " + dto.getRefEquipement());
            zone.setLatitude(equipement.getLatitude());
            zone.setLongitude(equipement.getLongitude());
            zone.setRayon(equipement.getRayon());
            zone.setCoordonnees(equipement.getCoordonnees() != null ? new ArrayList<>(equipement.getCoordonnees()) : new ArrayList<>());
            zoneRepo.save(zone);
            logger.info("New zone created for nomZone {}: ID {}", zone.getNomZone(), zone.getIdZone());
        }
        derco.setZoneAffectee(zone);

        // Infer issue type and set severity and priority
        String typePanne = dto.getTypePanne() != null && !dto.getTypePanne().isEmpty()
                ? dto.getTypePanne()
                : inferTypePanneFromDescription(dto.getDescriptionPanne());
        logger.info("Inferred issue type for equipment {}: {}", dto.getRefEquipement(), typePanne);

        GraviteDerCo gravite;
        Priorite priorite;
        switch (typePanne) {
            case "DEFAUT_NATUREL":
            case "DEFAUT_EXTERNE":
                gravite = GraviteDerCo.CRITIQUE;
                priorite = Priorite.HAUTE;
                break;
            case "USURE":
                gravite = GraviteDerCo.MOYEN;
                priorite = Priorite.MOYENNE;
                break;
            case "DEFAUT_ELECTRIQUE":
            default:
                gravite = GraviteDerCo.FAIBLE;
                priorite = Priorite.BASSE;
                break;
        }
        derco.setGraviteDerCo(gravite);
        derco.setPriorite(priorite);

        // Set delay and estimated resolution date
        setDelaiAndDateResolutionPrevue(derco, gravite);

        // Generate user-facing script
        String descriptionUser = generateScript(dto.getDescriptionPanne(), typePanne, impactedServices, derco.getDateResolutionPrevue());
        derco.setScript(descriptionUser);

        logger.info("Derco configured - refEquipement: {}, issueType: {}, severity: {}, priority: {}, userDescription: {}",
                dto.getRefEquipement(), typePanne, gravite, priorite, descriptionUser);
        return derco;
    }

    /**
     * Sets the estimated resolution delay and date for a DerCo based on its severity.

     */
    private void setDelaiAndDateResolutionPrevue(Derco derco, GraviteDerCo gravite) {
        long heuresDelai;
        switch (gravite) {
            case CRITIQUE:
                heuresDelai = 72;
                break;
            case GRAVE:
                heuresDelai = 48;
                break;
            case MOYEN:
                heuresDelai = 24;
                break;
            case FAIBLE:
            default:
                heuresDelai = 12;
                break;
        }

        derco.setDelaiPrevisionnel(heuresDelai + "h");

        Date dateDetection = derco.getDateDetection();
        if (dateDetection != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dateDetection);
            calendar.add(Calendar.HOUR_OF_DAY, (int) heuresDelai);
            derco.setDateResolutionPrevue(calendar.getTime());
        } else {
            logger.warn("Detection date is null for Derco, estimated resolution date not set.");
        }
    }

    /**
     * Generates a user-facing script describing the disruption, its cause, and expected resolution time.

     */
    private String generateScript(String descriptionPanne, String typePanne, String servicesImpactes, Date dateResolutionPrevue) {
        String serviceMessage = "Your " + servicesImpactes.toLowerCase() + " service is interrupted";

        String causeMessage = "";
        descriptionPanne = descriptionPanne != null ? descriptionPanne.toLowerCase() : "";
        switch (typePanne) {
            case "DEFAUT_NATUREL":
                if (descriptionPanne.contains("storm")) {
                    causeMessage = " due to a storm";
                } else if (descriptionPanne.contains("flood")) {
                    causeMessage = " due to a flood";
                } else {
                    causeMessage = " due to a natural event";
                }
                break;
            case "DEFAUT_EXTERNE":
                if (descriptionPanne.contains("construction")) {
                    causeMessage = " due to construction work in your area";
                } else if (descriptionPanne.contains("accident")) {
                    causeMessage = " due to an accident";
                } else {
                    causeMessage = " due to an external incident";
                }
                break;
            case "USURE":
                causeMessage = " due to equipment wear";
                break;
            case "DEFAUT_ELECTRIQUE":
                causeMessage = " due to an electrical issue";
                break;
            default:
                causeMessage = " due to a technical issue";
                break;
        }

        String resolutionMessage;
        if (dateResolutionPrevue != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy 'at' HH:mm");
            resolutionMessage = "We expect to restore the service by " + dateFormat.format(dateResolutionPrevue) + ".";
        } else {
            resolutionMessage = "We are working to restore the service as soon as possible.";
        }

        return serviceMessage + causeMessage + ". " + resolutionMessage;
    }

    /**
     * Infers the type of issue (panne) from its description.

     */
    private String inferTypePanneFromDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "UNKNOWN";
        }

        description = description.toLowerCase();
        if (description.contains("storm") || description.contains("flood")) {
            return "DEFAUT_NATUREL";
        } else if (description.contains("construction") || description.contains("accident")) {
            return "DEFAUT_EXTERNE";
        } else if (description.contains("wear") || description.contains("degraded")) {
            return "USURE";
        } else if (description.contains("electrical") || description.contains("overload")) {
            return "DEFAUT_ELECTRIQUE";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * Retrieves DerCo entities based on specified filters (e.g., date, city, equipment, severity).

     */
    @Transactional(readOnly = true)
    public List<Derco> findByFilters(Derco filter) {
        try {
            // Extract category filter
            String category = null;
            if (filter.getEquipementImpacte() != null &&
                    filter.getEquipementImpacte().getCategorie() != null &&
                    !filter.getEquipementImpacte().getCategorie().isEmpty()) {
                category = filter.getEquipementImpacte().getCategorie().toUpperCase();
                logger.info("Using category string '{}'", category);
            } else {
                logger.info("No category filter provided or empty category, skipping category filter");
            }

            // Convert string enums to enum types
            GraviteDerCo severity = null;
            if (filter.getGraviteDerCo() != null) {
                try {
                    severity = GraviteDerCo.valueOf(filter.getGraviteDerCo().toString());
                    logger.info("Converted severity string '{}' to enum '{}'",
                            filter.getGraviteDerCo(), severity);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid GraviteDerCo value: {}. Ignoring severity filter.",
                            filter.getGraviteDerCo());
                    severity = null;
                }
            }

            Priorite priority = null;
            if (filter.getPriorite() != null) {
                try {
                    priority = Priorite.valueOf(filter.getPriorite().toString());
                    logger.info("Converted priority string '{}' to enum '{}'",
                            filter.getPriorite(), priority);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid Priorite value: {}. Ignoring priority filter.",
                            filter.getPriorite());
                    priority = null;
                }
            }

            status status = null;
            if (filter.getStatus() != null) {
                try {
                    status = status.valueOf(filter.getStatus().toString());
                    logger.info("Converted status string '{}' to enum '{}'",
                            filter.getStatus(), status);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid status value: {}. Ignoring status filter.",
                            filter.getStatus());
                    status = null;
                }
            }

            // Log filter application
            logger.info("Applying filters: dateDetection={}, city={}, address={}, category={}, equipmentRef={}, severity={}, priority={}, status={}",
                    filter.getDateDetection(),
                    filter.getEquipementImpacte() != null ? filter.getEquipementImpacte().getVille() : null,
                    filter.getEquipementImpacte() != null ? filter.getEquipementImpacte().getAdresse() : null,
                    category,
                    filter.getEquipementImpacte() != null ? filter.getEquipementImpacte().getRefEquipement() : null,
                    severity,
                    priority,
                    status);

            // Query DerCos with filters
            List<Derco> filteredDercos = dercoRepo.findByFilters(
                    filter.getDateDetection(),
                    filter.getEquipementImpacte() != null ? filter.getEquipementImpacte().getVille() : null,
                    filter.getEquipementImpacte() != null ? filter.getEquipementImpacte().getAdresse() : null,
                    category,
                    filter.getEquipementImpacte() != null ? filter.getEquipementImpacte().getRefEquipement() : null,
                    severity,
                    priority,
                    status
            );

            logger.info("Found {} DerCos matching the filters", filteredDercos.size());
            return filteredDercos;
        } catch (Exception e) {
            logger.error("Error while filtering DerCos: {}", e.getMessage(), e);
            throw new RuntimeException("Error while filtering DerCos", e);
        }
    }
}