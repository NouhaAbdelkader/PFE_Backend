package com.example.pfe_backend.repos.InterventionRepo;

import com.example.pfe_backend.entities.Intervention.Intervention;
import com.example.pfe_backend.entities.Intervention.Rapport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RapportRepo  extends JpaRepository<Rapport,  Long> {
    @Query("SELECT r FROM Rapport r WHERE r.intervention.idIntervention = :interventionId")
    Optional<Rapport> findByInterventionIdIntervention(@Param("interventionId") Long interventionId);
}
