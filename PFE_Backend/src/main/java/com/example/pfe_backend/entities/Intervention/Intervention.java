package com.example.pfe_backend.entities.Intervention;

import com.example.pfe_backend.entities.DerCo.Derco;
import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Intervention {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-generate ID
     private Long idIntervention;

    private Date dateDebut;
    private Date dateFin;
     @ManyToOne
    @JoinColumn(name = "derco_id")
    private Derco derco; // Renamed from equipementRepare for clarity

    private String commentaire;
    private Float coutIntervention;

    private Status status;
    private String delaiPrevisionnel;
    private Date dateResolutionPrevue;
    private Date dateResolutionReelle;

    @ManyToMany
    @JoinTable(
            name = "intervention_equipe",
            joinColumns = @JoinColumn(name = "intervention_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<NotifixUser> equipe;

    @ManyToOne
    @JoinColumn(name = "chef_equipe_id")
    private NotifixUser chefEquipe;

    @OneToOne(mappedBy = "intervention")
    @JsonBackReference // Prevents serializing rapport back to intervention
    private Rapport rapport;
}