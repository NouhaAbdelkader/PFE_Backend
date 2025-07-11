package com.example.pfe_backend.controllers.InterventionController;

import com.example.pfe_backend.entities.Intervention.Intervention;
import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import com.example.pfe_backend.services.interventionService.InterventionService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/interventions")
public class InterventionController {

    private  final InterventionService interventionService;


    @PostMapping("/add")
    public ResponseEntity<Intervention> addIntervention(@Valid @RequestBody Intervention intervention) {
        Intervention createdIntervention = interventionService.addIntervention(intervention);
        return new ResponseEntity<>(createdIntervention, HttpStatus.CREATED);
    }


    @GetMapping("/all")
    public ResponseEntity<List<Intervention>> getAllInterventions() {
        List<Intervention> interventions = interventionService.getAllInterventions();
        return ResponseEntity.ok(interventions);
    }

    @PutMapping("/update")
    public ResponseEntity<Intervention> updateIntervention(@RequestBody Intervention intervention) {
        Intervention intervention1 = interventionService.UpdateIntervention(intervention);
        return ResponseEntity.ok(intervention1);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteIntervention(@PathVariable Long id) {
        interventionService.deleteIntervention(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/technician/{technicianId}")
    public ResponseEntity<List<Intervention>> getInterventionsForTechnician(
            @PathVariable Long technicianId) {
        List<Intervention> interventions = interventionService.getInterventionsForTechnician(technicianId);
        return ResponseEntity.ok(interventions);
    }

    @GetMapping("/chef/{chefId}")
    public ResponseEntity<List<Intervention>> getInterventionsForChef(
            @PathVariable Long chefId) {
        List<Intervention> interventions = interventionService.getInterventionsForChef(chefId);
        return ResponseEntity.ok(interventions);
    }


    @GetMapping("/filter")
    public ResponseEntity<List<Intervention>> getFilteredInterventions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date date,
            @RequestParam(required = false) String categorie) { // Add categorie parameter
        try {
            List<Intervention> interventions = interventionService.getFilteredInterventions(status, city, address, date, categorie);
            return ResponseEntity.ok(interventions);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getDistinctCategories() {
        try {
            List<String> categories = interventionService.getDistinctEquipmentCategories();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/addresses-by-city")
    public ResponseEntity<List<String>> getAddressesByCity(@RequestParam String city) {
        try {
            List<String> addresses = interventionService.getAddressesByCity(city);
            return ResponseEntity.ok(addresses);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    // New endpoint to close an intervention
    @PutMapping("/{id}/close")
    public ResponseEntity<Intervention> closeIntervention(
            @PathVariable Long id,
            @RequestParam boolean createAnother) {
        try {
            Intervention closedIntervention = interventionService.closeIntervention(id, createAnother);
            return ResponseEntity.ok(closedIntervention);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
