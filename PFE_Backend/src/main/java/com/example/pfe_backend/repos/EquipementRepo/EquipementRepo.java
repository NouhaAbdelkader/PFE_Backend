package com.example.pfe_backend.repos.EquipementRepo;

import com.example.pfe_backend.entities.Equipements.Equipement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface EquipementRepo extends JpaRepository<Equipement, Long> {
    Optional<Equipement> findByRefEquipement(String refEquipement);

    @Query("SELECT COUNT(e) FROM Equipement e WHERE (:ville IS NULL OR e.ville = :ville) AND (:categorie IS NULL OR e.categorie = :categorie)")
    long countEquipements(@Param("ville") String ville, @Param("categorie") String categorie);

    @Query("SELECT new map(e.etatEquipement as label, COUNT(e) as value) FROM Equipement e WHERE (:ville IS NULL OR e.ville = :ville) AND (:categorie IS NULL OR e.categorie = :categorie) GROUP BY e.etatEquipement")
    List<Map<String, Object>> countByEtat(@Param("ville") String ville, @Param("categorie") String categorie);

    @Query("SELECT DISTINCT e.ville FROM Equipement e")
    List<String> findAllVilles();

    @Query("SELECT e FROM Equipement e WHERE " +
            "(:dateInstallation IS NULL OR (MONTH(e.dateInstallation) = MONTH(:dateInstallation) AND YEAR(e.dateInstallation) = YEAR(:dateInstallation))) AND " +
            "(:ville IS NULL OR LOWER(e.ville) LIKE LOWER(CONCAT('%', :ville, '%'))) AND " +
            "(:adresse IS NULL OR LOWER(e.adresse) LIKE LOWER(CONCAT('%', :adresse, '%'))) AND " +
            "(:categorie IS NULL OR e.categorie = :categorie) AND " +
            "(:etat IS NULL OR e.etatEquipement = :etat)")
    List<Equipement> findByFilters(
            @Param("dateInstallation") Date dateInstallation,
            @Param("ville") String ville,
            @Param("adresse") String adresse,
            @Param("categorie") String categorie,
            @Param("etat") String etat
    );

    @Query("SELECT COUNT(e) FROM Equipement e WHERE " +
            "(:dateInstallation IS NULL OR (MONTH(e.dateInstallation) = MONTH(:dateInstallation) AND YEAR(e.dateInstallation) = YEAR(:dateInstallation))) AND " +
            "(:ville IS NULL OR LOWER(e.ville) LIKE LOWER(CONCAT('%', :ville, '%'))) AND " +
            "(:adresse IS NULL OR LOWER(e.adresse) LIKE LOWER(CONCAT('%', :adresse, '%'))) AND " +
            "(:categorie IS NULL OR e.categorie = :categorie) AND " +
            "(:etat IS NULL OR e.etatEquipement = :etat)")
    long countByFilters(
            @Param("dateInstallation") Date dateInstallation,
            @Param("ville") String ville,
            @Param("adresse") String adresse,
            @Param("categorie") String categorie,
            @Param("etat") String etat
    );


    @Query("SELECT e FROM Equipement e WHERE ABS(e.latitude - :latitude) <= 0.0001 AND ABS(e.longitude - :longitude) <= 0.0001")
    Optional<Equipement> findByLatitudeAndLongitude(@Param("latitude") Double latitude, @Param("longitude") Double longitude);
}