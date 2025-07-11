package com.example.pfe_backend.services.NotifixUserservice;

import com.example.pfe_backend.Configurations.JWT.JwtResponse;
import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import com.example.pfe_backend.entities.notifixUser.Status;

import java.util.List;

public interface INotifixUserService {
    NotifixUser addNotifixUser(NotifixUser notifixUser);
    public List<NotifixUser> getUsersNotifix();
    public JwtResponse login(String email, String password);
    public NotifixUser changePassword(NotifixUser userTripAura) ;

    Boolean verifPassword(NotifixUser userTripAura , String currentPassword);
    NotifixUser getUserByEmail (String email);
    public NotifixUser updateNotifixUser(NotifixUser notifixUser) ;
    void  deleteNotifixUser(Long notifixUserId);
    public void logout(String email);
    NotifixUser getUserByid (Long id);
    List<NotifixUser> fetchFilteredUsers(String email, String nom, String prenom, Status status, Boolean pending, int page, int size);
    long countFilteredUsers(String email, String nom, String prenom, Status status, Boolean pending);


}
