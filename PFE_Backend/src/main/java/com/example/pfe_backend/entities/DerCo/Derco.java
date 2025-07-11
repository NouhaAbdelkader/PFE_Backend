package com.example.pfe_backend.entities.DerCo;

import com.example.pfe_backend.entities.Equipements.Equipement;
import com.example.pfe_backend.entities.Intervention.Intervention;
import com.example.pfe_backend.entities.ClientImpactes.ClientImpacte;
import com.example.pfe_backend.entities.notifixUser.NotifixUser;
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
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "idDerCo")
public class Derco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_der_co")
    @JsonProperty("idDerCo")
    private Long idDerCo;

    @Column(name = "date_debut")
    @JsonProperty("dateDebut")
    private Date dateDebut;

    @Column(name = "date_detection")
    @JsonProperty("dateDetection")
    private Date dateDetection;

    @Column(name = "date_resolution_prevue")
    @JsonProperty("dateResolutionPrevue")
    private Date dateResolutionPrevue;

    @Column(name = "date_resolution_reelle")
    @JsonProperty("dateResolutionReelle")
    private Date dateResolutionReelle;

    @Column(name = "delai_previsionnel")
    @JsonProperty("delaiPrevisionnel")
    private String delaiPrevisionnel;

    @Column(name = "description")
    @JsonProperty("description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "gravite_der_co")
    @JsonProperty("graviteDerCo")
    private GraviteDerCo graviteDerCo;

    @Column(name = "nom_der_co")
    @JsonProperty("nomDerCo")
    private String nomDerCo;

    @Enumerated(EnumType.STRING)
    @Column(name = "priorite")
    @JsonProperty("priorite")
    private Priorite priorite;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @JsonProperty("status")
    private status status;

    @Column(name = "services_impactes")
    @JsonProperty("servicesImpactes")
    private String servicesImpactes;

    @Column(name = "script")
    @JsonProperty("script")
    private String script;

    @ManyToOne
    @JoinColumn(name = "equipement_impacte_id")
    @JsonProperty("equipementImpacte")
    private Equipement equipementImpacte;

    @ManyToOne
    @JoinColumn(name = "porteur_id")
    @JsonProperty("porteur")
    private NotifixUser porteur;

    @ManyToOne
    @JoinColumn(name = "zone_affectee_id")
    @JsonProperty("zoneAffectee")
    private Zone zoneAffectee;

    @OneToMany(mappedBy = "derco", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonProperty("interventions")
    private List<Intervention> interventions = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "derco_client", // Name of the join table
            joinColumns = @JoinColumn(name = "derco_id"), // Foreign key for Derco
            inverseJoinColumns = @JoinColumn(name = "client_id") // Foreign key for Client
    )
    @JsonProperty("clients")
    private List<ClientImpacte> clients = new ArrayList<>();
}