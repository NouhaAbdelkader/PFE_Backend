package com.example.pfe_backend.entities.Equipements;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Equipement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEquipement;

    private String ville;
    private Double longitude;
    private Double latitude;

    @Enumerated(EnumType.STRING)
    private CategoryEquipement categorie;

    @Enumerated(EnumType.STRING)
    private TypeEquipement typeEquipement;

    @Temporal(TemporalType.DATE)
    private Date dateInstallation;

    @Enumerated(EnumType.STRING)
    private EtatEquipement etatEquipement;
}