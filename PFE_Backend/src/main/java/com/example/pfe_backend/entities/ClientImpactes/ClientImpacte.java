package com.example.pfe_backend.entities.ClientImpactes;

import com.example.pfe_backend.entities.DerCo.Derco;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ClientImpacte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_client")
    @JsonProperty("idClient")
    private Long idClient;

    @Column(name = "nom")
     private String nom;

    @Column(name = "clientRef")
    private String clientRef;

    @Column(name = "prenom")
    private String prenom;

    @Column(name = "adresse")
    @JsonProperty("adresse")
    private String adresse;


    @Column(name = "ville")
    @JsonProperty("ville")
    private String ville;


    @Column(name = "longitude")
    @JsonProperty("longitude")
    private String longitude;

    @Column(name = "latitude")
    @JsonProperty("latitude")
    private String latitude;

    @Column(name = "numero")
    private String numero;

    @Column(name = "email")
    private String email;


    @Column(name = "gpspoint")
    private String gpspoint;


    @ManyToMany(mappedBy = "clients")
    @JsonProperty("dercos")
    private List<Derco> dercos = new ArrayList<>();

    @Column(name = "detected_at")
    @JsonProperty("detectedAt")
    private Date detectedAt;

}
