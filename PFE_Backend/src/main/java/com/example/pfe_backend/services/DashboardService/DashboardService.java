package com.example.pfe_backend.services.DashboardService;

import com.example.pfe_backend.repos.DerCoRepo.DerCoRepository;
import com.example.pfe_backend.repos.EquipementRepo.EquipementRepo;
import com.example.pfe_backend.repos.InterventionRepo.InterventionRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class DashboardService {

     private EquipementRepo equipementRepository;

     private DerCoRepository dercoRepository;

     private InterventionRepo interventionRepository;

    public Map<String, Object> getDashboardStats(String startDate, String endDate, String ville, String categorie, String gravite) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEquipements", equipementRepository.countEquipements(ville, categorie));
        stats.put("dercoActifs", dercoRepository.countActiveDercos(startDate, endDate, ville, gravite));
        stats.put("interventionResolutionRate", interventionRepository.calculateResolutionRate(startDate, endDate, ville));
        stats.put("totalClientsImpactes", dercoRepository.countClientsImpactes(startDate, endDate, ville, gravite));
        return stats;
    }

    public List<Map<String, Object>> getDercoByGravite(String startDate, String endDate, String ville, String gravite) {
        return dercoRepository.countByGravite(startDate, endDate, ville, gravite);
    }

    public List<Map<String, Object>> getEquipementByEtat(String ville, String categorie) {
        return equipementRepository.countByEtat(ville, categorie);
    }

    public List<Map<String, Object>> getDercoByMonth(String startDate, String endDate, String ville, String gravite) {
        return dercoRepository.countByMonth(startDate, endDate, ville, gravite);
    }

    public List<Map<String, Object>> getInterventionByStatus(String startDate, String endDate, String ville) {
        return interventionRepository.countByStatus(startDate, endDate, ville);
    }

    public List<String> getVilles() {
        return equipementRepository.findAllVilles();
    }
}