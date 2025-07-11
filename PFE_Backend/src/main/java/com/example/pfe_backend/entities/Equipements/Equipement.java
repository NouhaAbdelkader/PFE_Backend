package com.example.pfe_backend.entities.Equipements;

import com.example.pfe_backend.entities.DerCo.Derco;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Equipement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_equipement")
    @JsonProperty("idEquipement")
    private Long idEquipement;

    @Column(name = "ref_equipement")
    @JsonProperty("refEquipement")
    private String refEquipement;

    @Column(name = "ville")
    @JsonProperty("ville")
    private String ville;

    @Column(name = "adresse")
    @JsonProperty("adresse")
    private String adresse;

    @Column(name = "longitude")
    @JsonProperty("longitude")
    private Double longitude;

    @Column(name = "latitude")
    @JsonProperty("latitude")
    private Double latitude;

    @Column(name = "categorie")
    @JsonProperty("categorie")
    private String categorie;

    @Column(name = "type_equipement")
    @Enumerated(EnumType.STRING) // Store enum as string (CERCLE, POLYGONE)
    @JsonProperty("typeEquipement")
    private TypeEquipement typeEquipement;

    @Column(name = "date_installation")
    @JsonProperty("dateInstallation")
    private Date dateInstallation;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat_equipement")
    @JsonProperty("etatEquipement")
    private EtatEquipement etatEquipement;

    @OneToMany(mappedBy = "equipementImpacte", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Derco> derCos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "equipement_coordonnees", joinColumns = @JoinColumn(name = "equipement_id"))
    @Column(name = "coordonnees")
    @JsonProperty("coordonnees")
    private List<String> coordonnees;

    @Column(name = "rayon", nullable = true)
    @JsonProperty("rayon")
    private Double rayon;
}