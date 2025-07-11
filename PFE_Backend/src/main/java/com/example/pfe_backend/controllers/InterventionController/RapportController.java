package com.example.pfe_backend.controllers.InterventionController;

import com.example.pfe_backend.entities.Intervention.Rapport;
import com.example.pfe_backend.services.interventionService.RapportService;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/rapports")
public class RapportController {

    private RapportService rapportService;

    @PostMapping("/create")
    public ResponseEntity<?> createRapport(@RequestBody Rapport rapport) {
        try {
            Rapport savedRapport = rapportService.createRapport(rapport);
            return ResponseEntity.ok(savedRapport);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to save report: Data too large for photo column");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred: " + e.getMessage());
        }
    }

    @GetMapping("/intervention/{interventionId}")
    public ResponseEntity<Rapport> getRapportByInterventionId(@PathVariable Long interventionId) {
        Rapport rapport = rapportService.getRapportByInterventionId(interventionId);
        return rapport != null ? ResponseEntity.ok(rapport) : ResponseEntity.notFound().build();
    }


    @PutMapping("/{rapportId}/approve")
    public ResponseEntity<?> approveRapport(@PathVariable Long rapportId, @RequestBody Map<String, Long> requestBody) {
        try {
            Long approvedById = requestBody.get("approvedById");
            if (approvedById == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("approvedById is required");
            }
            rapportService.ApprouverRapport(rapportId, approvedById);
            Rapport updatedRapport = rapportService.getRapportById(rapportId); // Assume service has this or fetch via repo
            return ResponseEntity.ok(updatedRapport);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to approve report: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred: " + e.getMessage());
        }
    }

    @PutMapping("/{rapportId}/reject")
    public ResponseEntity<?> rejectRapport(@PathVariable Long rapportId, @RequestBody Map<String, Object> requestBody) {
        try {
            String rejectionReason = (String) requestBody.get("rejectionReason");
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("rejectionReason is required");
            }
            Long approvedById = requestBody.get("approvedById") != null ? ((Number) requestBody.get("approvedById")).longValue() : null;
            if (approvedById == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("approvedById is required");
            }
            rapportService.RejeterRapport(rapportId, rejectionReason, approvedById);
            Rapport updatedRapport = rapportService.getRapportById(rapportId); // Assume service has this or fetch via repo
            return ResponseEntity.ok(updatedRapport);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to reject report: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred: " + e.getMessage());
        }
    }
}