package com.example.pfe_backend.services.interventionService;

import com.example.pfe_backend.entities.Intervention.Intervention;
import java.util.Date;
import java.util.List;

public interface IInterventionService {
    Intervention addIntervention(Intervention intervention);
    List<Intervention> getAllInterventions();
    void deleteIntervention(Long idIntervention);
    Intervention UpdateIntervention(Intervention intervention);
    List<Intervention> getInterventionsForTechnician(Long technicianId);
    List<Intervention> getInterventionsForChef(Long technicianId);
    List<Intervention> getFilteredInterventions(String status, String city, String address, Date date, String categorie);
    List<String> getAddressesByCity(String city);
    List<String> getDistinctEquipmentCategories();
}