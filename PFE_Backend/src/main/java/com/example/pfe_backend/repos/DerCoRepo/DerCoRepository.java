package com.example.pfe_backend.repos.DerCoRepo;

import com.example.pfe_backend.entities.DerCo.Derco;
import com.example.pfe_backend.entities.DerCo.GraviteDerCo;
import com.example.pfe_backend.entities.DerCo.Priorite;
import com.example.pfe_backend.entities.DerCo.status;
import com.example.pfe_backend.entities.Equipements.EtatEquipement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DerCoRepository extends JpaRepository<Derco, Long> {

    @Query("SELECT d FROM Derco d WHERE d.equipementImpacte.idEquipement = :equipementId " +
            "AND d.equipementImpacte.etatEquipement = :etatEquipement " +
            "AND d.dateResolutionReelle IS NULL")
    Optional<Derco> findByEquipementImpacte_EtatEquipement(Long equipementId, EtatEquipement etatEquipement);

    @Query("SELECT COUNT(d) FROM Derco d WHERE d.status = 'ACTIVE' AND (:ville IS NULL OR d.equipementImpacte.ville = :ville) AND (:gravite IS NULL OR d.graviteDerCo = :gravite) AND (:startDate IS NULL OR d.dateDetection >= :startDate) AND (:endDate IS NULL OR d.dateDetection <= :endDate)")
    long countActiveDercos(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("ville") String ville, @Param("gravite") String gravite);

    @Query("SELECT COUNT(DISTINCT dc) FROM Derco d JOIN d.clients dc WHERE (:ville IS NULL OR d.equipementImpacte.ville = :ville) AND (:gravite IS NULL OR d.graviteDerCo = :gravite) AND (:startDate IS NULL OR d.dateDetection >= :startDate) AND (:endDate IS NULL OR d.dateDetection <= :endDate)")
    long countClientsImpactes(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("ville") String ville, @Param("gravite") String gravite);

    @Query("SELECT d.graviteDerCo AS label, COUNT(d) AS value FROM Derco d WHERE (:ville IS NULL OR d.equipementImpacte.ville = :ville) AND (:gravite IS NULL OR d.graviteDerCo = :gravite) AND (:startDate IS NULL OR d.dateDetection >= :startDate) AND (:endDate IS NULL OR d.dateDetection <= :endDate) GROUP BY d.graviteDerCo")
    List<Map<String, Object>> countByGravite(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("ville") String ville, @Param("gravite") String gravite);

    @Query("SELECT FUNCTION('DATE_FORMAT', d.dateDetection, '%Y-%m') AS month, COUNT(d) AS count FROM Derco d WHERE (:ville IS NULL OR d.equipementImpacte.ville = :ville) AND (:gravite IS NULL OR d.graviteDerCo = :gravite) AND (:startDate IS NULL OR d.dateDetection >= :startDate) AND (:endDate IS NULL OR d.dateDetection <= :endDate) GROUP BY FUNCTION('DATE_FORMAT', d.dateDetection, '%Y-%m')")
    List<Map<String, Object>> countByMonth(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("ville") String ville, @Param("gravite") String gravite);

    List<Derco> findByStatus(status status);

    @Query("SELECT d FROM Derco d " +
            "WHERE (:detectionDate IS NULL OR FUNCTION('YEAR', d.dateDetection) = FUNCTION('YEAR', :detectionDate) AND FUNCTION('MONTH', d.dateDetection) = FUNCTION('MONTH', :detectionDate)) " +
            "AND (:city IS NULL OR LOWER(d.equipementImpacte.ville) LIKE LOWER(CONCAT('%', :city, '%'))) " +
            "AND (:address IS NULL OR LOWER(d.equipementImpacte.adresse) LIKE LOWER(CONCAT('%', :address, '%'))) " +
            "AND (:category IS NULL OR d.equipementImpacte.categorie = :category) " +
            "AND (:equipmentRef IS NULL OR LOWER(d.equipementImpacte.refEquipement) LIKE LOWER(CONCAT('%', :equipmentRef, '%'))) " +
            "AND (:severity IS NULL OR d.graviteDerCo = :severity) " +
            "AND (:priority IS NULL OR d.priorite = :priority) " +
            "AND (:status IS NULL OR d.status = :status)")
    List<Derco> findByFilters(
            @Param("detectionDate") Date detectionDate,
            @Param("city") String city,
            @Param("address") String address,
            @Param("category") String category, // Changed from CategoryEquipement to String
            @Param("equipmentRef") String equipmentRef,
            @Param("severity") GraviteDerCo severity,
            @Param("priority") Priorite priority,
            @Param("status") status status
    );
}