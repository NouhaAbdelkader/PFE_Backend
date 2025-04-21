package com.example.pfe_backend.repos.EquipementRepo;

import com.example.pfe_backend.entities.Equipements.Equipement;
import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipementRepo extends JpaRepository<Equipement, Long> {
}
