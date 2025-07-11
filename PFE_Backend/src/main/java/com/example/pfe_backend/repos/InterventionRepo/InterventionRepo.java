package com.example.pfe_backend.repos.InterventionRepo;

import com.example.pfe_backend.entities.Intervention.Intervention;
import com.example.pfe_backend.entities.Intervention.Status;
import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface InterventionRepo extends JpaRepository<Intervention, Long> {
    @Query("SELECT i FROM Intervention i JOIN i.equipe e WHERE e.userId = :technicianId")
    List<Intervention> findByTechnicianId(@Param("technicianId") Long technicianId);

    List<Intervention> findInterventionByChefEquipe(NotifixUser technician);

    @Query("SELECT (COUNT(i) * 100.0 / (SELECT COUNT(*) FROM Intervention i2 WHERE (:startDate IS NULL OR i2.dateDebut >= :startDate) AND (:endDate IS NULL OR i2.dateDebut <= :endDate) AND (:ville IS NULL OR i2.derco.equipementImpacte.ville = :ville))) " +
            "FROM Intervention i WHERE i.status IN (com.example.pfe_backend.entities.Intervention.Status.RESOLUE, com.example.pfe_backend.entities.Intervention.Status.CLOTUREE) " +
            "AND (:startDate IS NULL OR i.dateDebut >= :startDate) AND (:endDate IS NULL OR i.dateDebut <= :endDate) " +
            "AND (:ville IS NULL OR i.derco.equipementImpacte.ville = :ville)")
    double calculateResolutionRate(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("ville") String ville);

    @Query("SELECT new map(i.status as label, COUNT(i) as value) FROM Intervention i " +
            "WHERE (:ville IS NULL OR i.derco.equipementImpacte.ville = :ville) " +
            "AND (:startDate IS NULL OR i.dateDebut >= :startDate) AND (:endDate IS NULL OR i.dateDebut <= :endDate) " +
            "GROUP BY i.status")
    List<Map<String, Object>> countByStatus(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("ville") String ville);

    @Query("SELECT i FROM Intervention i " +
            "WHERE (:status IS NULL OR i.status = :status) " +
            "AND (:city IS NULL OR (i.derco IS NOT NULL AND i.derco.equipementImpacte IS NOT NULL AND LOWER(i.derco.equipementImpacte.ville) LIKE LOWER(CONCAT('%', :city, '%')))) " +
            "AND (:address IS NULL OR (i.derco IS NOT NULL AND i.derco.equipementImpacte IS NOT NULL AND LOWER(i.derco.equipementImpacte.adresse) LIKE LOWER(CONCAT('%', :address, '%')))) " +
            "AND (:date IS NULL OR FUNCTION('DATE', i.dateDebut) = :date) " +
            "AND (:categorie IS NULL OR (i.derco IS NOT NULL AND i.derco.equipementImpacte IS NOT NULL AND i.derco.equipementImpacte.categorie = :categorie))")
    List<Intervention> findByFilters(@Param("status") Status status,
                                     @Param("city") String city,
                                     @Param("address") String address,
                                     @Param("date") Date date,
                                     @Param("categorie") String categorie);

    @Query("SELECT DISTINCT e.categorie FROM Equipement e WHERE e.categorie IS NOT NULL")
    List<String> findDistinctEquipmentCategories();

    @Query("SELECT DISTINCT i.derco.equipementImpacte.adresse FROM Intervention i " +
            "WHERE i.derco IS NOT NULL AND i.derco.equipementImpacte IS NOT NULL " +
            "AND LOWER(i.derco.equipementImpacte.ville) LIKE LOWER(CONCAT('%', :city, '%'))")
    List<String> findAddressesByCity(@Param("city") String city);
}