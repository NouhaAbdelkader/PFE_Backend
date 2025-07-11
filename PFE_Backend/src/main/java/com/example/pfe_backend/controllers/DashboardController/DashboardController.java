package com.example.pfe_backend.controllers.DashboardController;

import com.example.pfe_backend.services.DashboardService.DashboardService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@AllArgsConstructor
public class DashboardController {

    private DashboardService dashboardService;

    @GetMapping("/stats")
    public Map<String, Object> getDashboardStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String categorie,
            @RequestParam(required = false) String gravite) {
        return dashboardService.getDashboardStats(startDate, endDate, ville, categorie, gravite);
    }

    @GetMapping("/derco/gravite")
    public List<Map<String, Object>> getDercoByGravite(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String gravite) {
        return dashboardService.getDercoByGravite(startDate, endDate, ville, gravite);
    }

    @GetMapping("/equipement/etat")
    public List<Map<String, Object>> getEquipementByEtat(
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String categorie) {
        return dashboardService.getEquipementByEtat(ville, categorie);
    }

    @GetMapping("/derco/month")
    public List<Map<String, Object>> getDercoByMonth(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String gravite) {
        return dashboardService.getDercoByMonth(startDate, endDate, ville, gravite);
    }

    @GetMapping("/intervention/status")
    public List<Map<String, Object>> getInterventionByStatus(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String ville) {
        return dashboardService.getInterventionByStatus(startDate, endDate, ville);
    }

    @GetMapping("/equipement/villes")
    public List<String> getVilles() {
        return dashboardService.getVilles();
    }
}