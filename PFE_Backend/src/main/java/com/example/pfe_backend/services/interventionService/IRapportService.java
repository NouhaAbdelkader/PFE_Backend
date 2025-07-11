package com.example.pfe_backend.services.interventionService;

import com.example.pfe_backend.entities.Intervention.Rapport;

public interface IRapportService {

    Rapport createRapport(Rapport rapport);
    Rapport getRapportByInterventionId(Long interventionId);
    void ApprouverRapport(Long rapportId, Long approvedById);
    void RejeterRapport(Long rapportId, String rejectionReason, Long approvedById);
    Rapport getRapportById(Long rapportId);
}
