package com.example.pfe_backend.repos.NotifixUserRepo;

import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import com.example.pfe_backend.entities.notifixUser.Role;
import com.example.pfe_backend.entities.notifixUser.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotifixUserRepo extends JpaRepository<NotifixUser, Long> {
    public Boolean existsNotifixUserByEmail(String email);
    public Boolean existsNotifixUserByNumero(String numero);
    public NotifixUser findByEmail(String email);
    public NotifixUser findNotifixUserByUserId(Long id);
    List<NotifixUser> findByRole(Role role);

    @Query("SELECT u FROM NotifixUser u WHERE " +
            "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
            "(:nom IS NULL OR LOWER(u.nom) LIKE LOWER(CONCAT('%', :nom, '%'))) AND " +
            "(:prenom IS NULL OR LOWER(u.prenom) LIKE LOWER(CONCAT('%', :prenom, '%'))) AND " +
            "(:status IS NULL OR u.status = :status) AND " +
            "(:pending IS NULL OR (:pending = true AND u.role IS NULL) OR (:pending = false))")
    List<NotifixUser> findByFilters(
            @Param("email") String email,
            @Param("nom") String nom,
            @Param("prenom") String prenom,
            @Param("status") Status status,
            @Param("pending") Boolean pending
    );

}
