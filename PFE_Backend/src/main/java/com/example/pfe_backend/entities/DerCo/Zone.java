package com.example.pfe_backend.entities.DerCo;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_zone")
    @JsonProperty("idZone")
    private Long idZone;

    @Column(name = "nom_zone")
    @JsonProperty("nomZone")
    private String nomZone;

    @Column(name = "ville")
    @JsonProperty("ville")
    private String ville;

    @Column(name = "description")
    @JsonProperty("description")
    private String description;

    @Column(name = "latitude")
    @JsonProperty("latitude")
    private Double latitude;

    @Column(name = "longitude")
    @JsonProperty("longitude")
    private Double longitude;

    @Column(name = "rayon", nullable = true)
    @JsonProperty("rayon")
    private Double rayon;

    @ElementCollection
    @CollectionTable(name = "zone_coordonnees", joinColumns = @JoinColumn(name = "zone_id"))
    @Column(name = "coordonnees")
    @JsonProperty("coordonnees")
    private List<String> coordonnees;
}