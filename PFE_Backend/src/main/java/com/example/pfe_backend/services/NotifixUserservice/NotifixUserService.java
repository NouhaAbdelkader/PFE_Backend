package com.example.pfe_backend.services.NotifixUserservice;

import com.example.pfe_backend.Configurations.JWT.JwtResponse;
import com.example.pfe_backend.Configurations.JWT.JwtUtils;
import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import com.example.pfe_backend.entities.notifixUser.Status;
import com.example.pfe_backend.exceptions.CustomException;
import com.example.pfe_backend.repos.NotifixUserRepo.NotifixUserRepo;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@AllArgsConstructor
public class NotifixUserService implements  INotifixUserService{
    private  NotifixUserRepo notifixUserRepo;
    private final PasswordEncoder passwordEncoder;
    private JwtUtils jwtUtils ;

    @Override
    public NotifixUser addNotifixUser(NotifixUser notifixUser) {
        if(notifixUserRepo.existsNotifixUserByEmail(notifixUser.getEmail()))
        {
            throw new CustomException("there is already a Notifix user  with this email");
        }
        if(notifixUserRepo.existsNotifixUserByNumero(notifixUser.getNumero()))
        {
            throw new CustomException("there is already a Notifix user  with this number");
        }
        else {
            String encodedPassword = passwordEncoder.encode(notifixUser.getPassword());
            notifixUser.setPassword(encodedPassword);
            notifixUser.setStatus(Status.INACTIVE);
            return notifixUserRepo.save(notifixUser);
        }
    }

    @Override
    public List<NotifixUser> getUsersNotifix() {
        return notifixUserRepo.findAll();
    }

    public JwtResponse login(String email, String password) {
        NotifixUser user = notifixUserRepo.findByEmail(email);

        if (user == null) {
            throw new CustomException("Login failed. There is no user with this email");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new CustomException("Login failed. Invalid password");
        }
        user.setStatus(Status.ACTIVE);
        notifixUserRepo.save(user); // ⬅️ Sauvegarde le changement de status
        String token = jwtUtils.generateJwtToken(user);
        String refreshhtoken = jwtUtils.generateRefreshToken(user);
        return new JwtResponse(token, user, refreshhtoken );
    }

    @Override
    public NotifixUser changePassword(NotifixUser notifixUser) {
        String encodedPassword = passwordEncoder.encode(notifixUser.getPassword());
        notifixUser.setPassword(encodedPassword);
        return notifixUserRepo.save(notifixUser);

    }

    @Override
    public Boolean verifPassword(NotifixUser notifixUser, String currentPassword) {
        // Check if the provided password matches the encoded password stored in the user entity
        if (passwordEncoder.matches(currentPassword, notifixUser.getPassword())) {
            return true;
        }
        return false;
    }

    @Override
    public NotifixUser getUserByEmail(String email) {
        return notifixUserRepo.findByEmail(email);
    }

    @Override
    public NotifixUser updateNotifixUser(NotifixUser notifixUser) {

        return notifixUserRepo.save(notifixUser);
    }

    @Override
    public void deleteNotifixUser(Long notifixUserId) {
        if (!notifixUserRepo.existsById(notifixUserId) ){
            throw new CustomException("notifixUser not found");
        }
        notifixUserRepo.deleteById(notifixUserId);
    }

    @Override
    public void logout(String email) {
        NotifixUser user = notifixUserRepo.findByEmail(email);
        if (user == null) {
            throw new CustomException("User not found");
        }
        user.setStatus(Status.INACTIVE);
        notifixUserRepo.save(user);
    }


}

