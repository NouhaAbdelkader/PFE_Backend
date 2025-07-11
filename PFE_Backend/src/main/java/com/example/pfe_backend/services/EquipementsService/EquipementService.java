package com.example.pfe_backend.services.EquipementsService;

import com.example.pfe_backend.entities.DerCo.Zone;
import com.example.pfe_backend.entities.Equipements.Equipement;
import com.example.pfe_backend.exceptions.CustomException;
import com.example.pfe_backend.repos.DerCoRepo.ZoneRepository;
import com.example.pfe_backend.repos.EquipementRepo.EquipementRepo;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Service class for managing equipment-related operations.
 */
@Service
@AllArgsConstructor
public class EquipementService implements IEquipementService {

    private EquipementRepo equipementRepository;
    private ZoneRepository zoneRepository;

    /**
     * Adds a new equipement to the repository and sets its city and address based on coordinates.

     */
    @Override
    public Equipement addEquipement(Equipement equipement) {
        if (equipement.getLatitude() != null && equipement.getLongitude() != null) {
            String ville = GeoUtils.getCityFromCoordinates(equipement.getLatitude(), equipement.getLongitude());
            String adresse = GeoUtils.getAddressFromCoordinates(equipement.getLatitude(), equipement.getLongitude());
            equipement.setVille(ville);
            equipement.setAdresse(adresse);
        }
        return equipementRepository.save(equipement);
    }

    /**
     * Retrieves all equipment from the repository in reverse order.

     */
    @Override
    public List<Equipement> getEquipements() {
        List<Equipement> equipements = equipementRepository.findAll();
        Collections.reverse(equipements);
        return equipements;
    }

    /**
     * Updates an existing equipment and synchronizes its associated zone information.

     */
    @Override
    public Equipement UpdateEquipement(Equipement equipement) {
        if (!equipementRepository.existsById(equipement.getIdEquipement())) {
            throw new CustomException("Equipement not found with ID: " + equipement.getIdEquipement());
        }

        Equipement updatedEquipement = equipementRepository.save(equipement);

        Optional<Zone> zoneOpt = zoneRepository.findByNomZone(equipement.getRefEquipement());
        if (zoneOpt.isPresent()) {
            Zone zone = zoneOpt.get();
            zone.setLatitude(equipement.getLatitude());
            zone.setLongitude(equipement.getLongitude());
            zone.setRayon(equipement.getRayon());
            zone.setCoordonnees(equipement.getCoordonnees());
            zone.setVille(equipement.getVille());
            zoneRepository.save(zone);
        } else {
            Zone newZone = new Zone();
            newZone.setNomZone(equipement.getRefEquipement());
            newZone.setVille(equipement.getVille());
            newZone.setDescription("Zone created for equipment " + equipement.getRefEquipement());
            newZone.setLatitude(equipement.getLatitude());
            newZone.setLongitude(equipement.getLongitude());
            newZone.setRayon(equipement.getRayon());
            newZone.setCoordonnees(equipement.getCoordonnees());
            zoneRepository.save(newZone);
        }

        return updatedEquipement;
    }

    /**
     * Deletes an equipement by its ID.

     */
    @Override
    public void deleteEquipement(Long EquipementId) {
        if (!equipementRepository.existsById(EquipementId)) {
            throw new CustomException("Equipement not found");
        }
        equipementRepository.deleteById(EquipementId);
    }

    /**
     * Retrieves a paginated list of equipment filtered by specified criteria.

     */
    @Override
    public List<Equipement> fetchFilteredEquipements(Date dateInstallation, String ville, String adresse, String categorie, String etat, int page, int size) {
        // Normalize empty strings to null for query
        String villeFilter = (ville != null && !ville.trim().isEmpty()) ? ville.trim() : null;
        String adresseFilter = (adresse != null && !adresse.trim().isEmpty()) ? adresse.trim() : null;
        String categorieFilter = (categorie != null && !categorie.trim().isEmpty()) ? categorie.trim() : null;
        String etatFilter = (etat != null && !etat.trim().isEmpty()) ? etat.trim() : null;

        // Create pageable object for pagination
        Pageable pageable = PageRequest.of(page - 1, size);

        // Fetch filtered equipments from repository
        List<Equipement> filteredEquipements = equipementRepository.findByFilters(
                dateInstallation, villeFilter, adresseFilter, categorieFilter, etatFilter);

        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredEquipements.size());
        List<Equipement> paginatedEquipements = filteredEquipements.subList(start, end);

        return paginatedEquipements;
    }

    /**
     * Counts the number of equipment matching the specified filter criteria.

     */
    @Override
    public long countFilteredEquipements(Date dateInstallation, String ville, String adresse, String categorie, String etat) {
        String villeFilter = (ville != null && !ville.trim().isEmpty()) ? ville.trim() : null;
        String adresseFilter = (adresse != null && !adresse.trim().isEmpty()) ? adresse.trim() : null;
        String categorieFilter = (categorie != null && !categorie.trim().isEmpty()) ? categorie.trim() : null;
        String etatFilter = (etat != null && !etat.trim().isEmpty()) ? etat.trim() : null;

        List<Equipement> filteredEquipements = equipementRepository.findByFilters(
                dateInstallation, villeFilter, adresseFilter, categorieFilter, etatFilter);
        return filteredEquipements.size();
    }
}