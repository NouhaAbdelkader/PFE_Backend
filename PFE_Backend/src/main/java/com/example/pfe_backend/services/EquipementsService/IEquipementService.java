package com.example.pfe_backend.services.EquipementsService;

import com.example.pfe_backend.entities.Equipements.Equipement;

import java.util.Date;
import java.util.List;

public interface IEquipementService {
    public Equipement addEquipement(Equipement equipement);
    public List<Equipement> getEquipements();
    public Equipement UpdateEquipement(Equipement equipement);
    void  deleteEquipement(Long EquipementId);
    List<Equipement> fetchFilteredEquipements(Date dateInstallation, String ville, String adresse, String categorie, String etat, int page, int size);
    long countFilteredEquipements(Date dateInstallation, String ville, String adresse, String categorie, String etat);
}
