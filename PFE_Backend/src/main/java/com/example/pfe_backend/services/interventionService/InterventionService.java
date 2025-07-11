package com.example.pfe_backend.services.interventionService;

import com.example.pfe_backend.entities.DerCo.Derco;
import com.example.pfe_backend.entities.DerCo.status;
import com.example.pfe_backend.entities.Intervention.Intervention;
import com.example.pfe_backend.entities.Intervention.Status;
import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import com.example.pfe_backend.repos.DerCoRepo.DerCoRepository;
import com.example.pfe_backend.repos.InterventionRepo.InterventionRepo;
import com.example.pfe_backend.repos.NotifixUserRepo.NotifixUserRepo;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;


@Service
@AllArgsConstructor
public class InterventionService implements IInterventionService {

    private final InterventionRepo interventionRepo;
    private final DerCoRepository dercoRepo;
    private final NotifixUserRepo notifixUserRepo;

    /**
     * Ajoute une nouvelle intervention en définissant son statut à EN_COURS et en calculant le délai prévisionnel.

     */
    @Override
    @Transactional
    public Intervention addIntervention(Intervention intervention) {
        // Définit le statut de l'intervention à EN_COURS
        intervention.setStatus(Status.EN_COURS);

        // Définit la date de début à l'heure actuelle si non fournie
        if (intervention.getDateDebut() == null) {
            intervention.setDateDebut(new Date());
        }

        // Calcule le délai prévisionnel si la date de résolution prévue est fournie
        if (intervention.getDateResolutionPrevue() != null) {
            Instant start = intervention.getDateDebut().toInstant();
            Instant end = intervention.getDateResolutionPrevue().toInstant();
            Duration duration = Duration.between(start, end);

            // Convertit la durée en jours et heures
            long days = duration.toDays();
            long hours = duration.toHours() % 24;

            // Formate le délai prévisionnel
            StringBuilder delai = new StringBuilder();
            if (days > 0) {
                delai.append(days).append(days == 1 ? " jour" : " jours");
            }
            if (hours > 0) {
                if (days > 0) delai.append(" ");
                delai.append(hours).append(hours == 1 ? " heure" : " heures");
            }
            if (days == 0 && hours == 0) {
                delai.append("Moins d'une heure");
            }
            intervention.setDelaiPrevisionnel(delai.toString());
        } else {
            // Valeur par défaut si aucune date de résolution prévue
            intervention.setDelaiPrevisionnel("N/A");
        }

        // Sauvegarde et retourne l'intervention
        return interventionRepo.save(intervention);
    }

    /**
     * Récupère toutes les interventions, triées par ordre décroissant (les plus récentes en premier).

     */
    @Override
    public List<Intervention> getAllInterventions() {
        List<Intervention> interventions = interventionRepo.findAll();
        Collections.reverse(interventions); // Inverse l'ordre pour afficher les plus récentes en premier
        return interventions;
    }

    /**
     * Supprime une intervention par son ID.

     */
    @Override
    public void deleteIntervention(Long idIntervention) {
        interventionRepo.deleteById(idIntervention);
    }

    /**
     * Met à jour une intervention existante avec les nouvelles informations fournies.

     */
    @Override
    public Intervention UpdateIntervention(Intervention intervention) {
        return interventionRepo.save(intervention);
    }

    /**
     * Récupère toutes les interventions associées à un technicien spécifique.

     */
    @Override
    public List<Intervention> getInterventionsForTechnician(Long technicianId) {
        return interventionRepo.findByTechnicianId(technicianId);
    }

    /**
     * Récupère toutes les interventions gérées par un chef d'équipe spécifique.

     */
    @Override
    public List<Intervention> getInterventionsForChef(Long technicianId) {
        NotifixUser technician = notifixUserRepo.findById(technicianId)
                .orElseThrow(() -> new IllegalArgumentException("Chef d'équipe non trouvé avec l'ID : " + technicianId));
        return interventionRepo.findInterventionByChefEquipe(technician);
    }

    /**
     * Récupère les interventions filtrées selon des critères comme le statut, la ville, l'adresse, la date, et la catégorie.

     */
    @Override
    public List<Intervention> getFilteredInterventions(String status, String city, String address, Date date, String categorie) {
        Status statusEnum = null;
        try {
            if (status != null && !status.isEmpty()) {
                statusEnum = Status.valueOf(status);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Valeur de statut invalide : " + status, e);
        }
        return interventionRepo.findByFilters(statusEnum, city, address, date, categorie);
    }

    /**
     * Récupère la liste des adresses distinctes pour une ville donnée.

     */
    @Override
    public List<String> getAddressesByCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return interventionRepo.findAddressesByCity(city);
    }

    /**
     * Récupère la liste des catégories d'équipements distinctes associées aux interventions.

      */
    @Override
    public List<String> getDistinctEquipmentCategories() {
        return interventionRepo.findDistinctEquipmentCategories();
    }

    /**
     * Clôture une intervention en changeant son statut à CLOTUREE et met à jour le statut du DerCo associé.
     * Peut optionnellement garder le DerCo actif pour de nouvelles interventions.

     */
     @Transactional
    public Intervention closeIntervention(Long interventionId, boolean createAnother) {
        // Vérifie l'existence de l'intervention
        Intervention intervention = interventionRepo.findById(interventionId)
                .orElseThrow(() -> new IllegalArgumentException("Intervention non trouvée avec l'ID : " + interventionId));

        // Vérifie si l'intervention est dans le statut RESOLUE
        if (intervention.getStatus() != Status.RESOLUE) {
            throw new IllegalStateException("Seules les interventions RESOLUE peuvent être clôturées");
        }

        // Change le statut de l'intervention à CLOTUREE
        intervention.setStatus(Status.CLOTUREE);
        interventionRepo.save(intervention);

        // Vérifie l'existence du DerCo associé
        Derco derco = intervention.getDerco();
        if (derco == null) {
            throw new IllegalStateException("Aucun DerCo associé à cette intervention");
        }

        // Met à jour le statut du DerCo
        derco.setStatus(status.RESOLVED);

        // Garde le DerCo actif si une nouvelle intervention est prévue
        if (createAnother) {
            derco.setStatus(status.ACTIVE);
        }

        dercoRepo.save(derco);

        return intervention;
    }
}