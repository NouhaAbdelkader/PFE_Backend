package com.example.pfe_backend.repos.NotifixUserRepo;

import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotifixUserRepo extends JpaRepository<NotifixUser, Long> {
    public Boolean existsNotifixUserByEmail(String email);
    public Boolean existsNotifixUserByNumero(String numero);
    public NotifixUser findByEmail(String email);
    public NotifixUser findNotifixUserByUserId(Long id);

}
