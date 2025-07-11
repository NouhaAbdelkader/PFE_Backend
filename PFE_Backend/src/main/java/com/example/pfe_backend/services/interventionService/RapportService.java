package com.example.pfe_backend.services.interventionService;

import com.example.pfe_backend.entities.Intervention.ApprovalStatus;
import com.example.pfe_backend.entities.Intervention.Intervention;
import com.example.pfe_backend.entities.Intervention.Rapport;
import com.example.pfe_backend.entities.Intervention.Status;
import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import com.example.pfe_backend.repos.InterventionRepo.InterventionRepo;
import com.example.pfe_backend.repos.InterventionRepo.RapportRepo;
import com.example.pfe_backend.repos.NotifixUserRepo.NotifixUserRepo;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

@Service
@AllArgsConstructor
public class RapportService implements IRapportService {

    private RapportRepo rapportRepo;
    private InterventionRepo interventionRepo;
    private NotifixUserRepo notifixUserRepo ;
    @Override
    @Transactional
    public Rapport createRapport(Rapport rapport) {
        // Validate that rapport has an intervention with an ID
        if (rapport.getIntervention() == null || rapport.getIntervention().getIdIntervention() == null) {
            throw new IllegalArgumentException("Rapport must be associated with a valid Intervention ID");
        }

        // Fetch the managed Intervention from the database
        Long interventionId = rapport.getIntervention().getIdIntervention();
        Intervention managedIntervention = interventionRepo.findById(interventionId)
                .orElseThrow(() -> new IllegalArgumentException("Intervention with ID " + interventionId + " not found"));

        // Update the intervention status to RESOLUE
        managedIntervention.setStatus(Status.RESOLUE);

        // Update dateResolutionReelle to current time (optional, if required)
        managedIntervention.setDateResolutionReelle(new java.util.Date());

        // Save the updated intervention
        interventionRepo.save(managedIntervention);

        // Set the managed intervention on the rapport
        rapport.setIntervention(managedIntervention);

        // Set the approval status to PENDING
        rapport.setApprovalStatus(ApprovalStatus.PENDING);

        // Set createdAt to current time (if not set by frontend)
        if (rapport.getCreatedAt() == null) {
            rapport.setCreatedAt(java.time.LocalDateTime.now());
        }

        // Save and return the rapport
        return rapportRepo.save(rapport);
    }

    @Override
    public Rapport getRapportByInterventionId(Long interventionId) {
        return rapportRepo.findByInterventionIdIntervention(interventionId)
                .orElse(null);
    }

    @Override
    @Transactional
    public void ApprouverRapport(Long rapportId, Long approvedById) {
        if (rapportId == null) {
            throw new IllegalArgumentException("Rapport ID cannot be null");
        }
        Rapport rapport = rapportRepo.findById(rapportId)
                .orElseThrow(() -> new IllegalArgumentException("Report with ID " + rapportId + " not found"));
        if (rapport.getApprovalStatus() == ApprovalStatus.APPROVED) {
            throw new IllegalStateException("Rapport is already approved");
        }
        if (rapport.getIntervention() == null) {
            throw new IllegalStateException("No intervention associated with this rapport");
        }
        if (approvedById == null) {
            throw new IllegalArgumentException("ApprovedBy ID cannot be null");
        }

        NotifixUser approver = notifixUserRepo.findById(approvedById)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + approvedById + " not found"));

        rapport.setApprovalStatus(ApprovalStatus.APPROVED);
        rapport.setResolutionDate(LocalDateTime.now());
        rapport.setApprovedBy(approver);

        rapport.getIntervention().setStatus(Status.RESOLUE);
        rapport.getIntervention().setDateResolutionReelle(new Date());

        rapportRepo.save(rapport);
        interventionRepo.save(rapport.getIntervention());
    }
    @Override
    public Rapport getRapportById(Long rapportId) {
        return rapportRepo.findById(rapportId).orElse(null);
    }
    @Override
    @Transactional
    public void RejeterRapport(Long rapportId, String rejectionReason, Long approvedById) {
        if (rapportId == null) {
            throw new IllegalArgumentException("Rapport cannot be null");
        }
        Rapport rapport = rapportRepo.findById(rapportId)
                .orElseThrow(() -> new IllegalArgumentException("Report with ID " + rapportId + " not found"));
        if (rapport.getApprovalStatus() == ApprovalStatus.REJECTED) {
            throw new IllegalStateException("Rapport is already rejected");
        }
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        if (approvedById == null) {
            throw new IllegalArgumentException("ApprovedBy ID cannot be null");
        }

        NotifixUser approver = notifixUserRepo.findById(approvedById)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + approvedById + " not found"));
        rapport.setApprovedBy(approver);

        rapport.setApprovalStatus(ApprovalStatus.REJECTED);
        rapport.setRejectionReason(rejectionReason);

        rapportRepo.save(rapport);
    }
}