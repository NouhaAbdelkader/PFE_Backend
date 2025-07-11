package com.example.pfe_backend.entities.Intervention;

import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.List;
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Rapport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Ensure ID is auto-generated
    private Long idRapport;
    private String description;

    @ElementCollection
    @CollectionTable(name = "rapport_photos", schema = "notifix", joinColumns = @JoinColumn(name = "rapport_id_rapport"))
    @Column(name = "photo", columnDefinition = "TEXT") // Use TEXT for large base64 strings
    private List<String> photos;
    @OneToOne
    @JoinColumn(name = "intervention_id")
    @JsonManagedReference // Serializes intervention normally
    private Intervention intervention;
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private NotifixUser createdBy;

    @Column
    private LocalDateTime resolutionDate;

    @Column(columnDefinition = "TEXT")
    private String actionsTaken;

    @Column(columnDefinition = "TEXT")
    private String materialsUsed;


    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus;

    @ManyToOne
    @JoinColumn(name = "approved_by_id")
    private NotifixUser approvedBy;

    @Column(columnDefinition = "TEXT")
    private String escalationDetails;
}
