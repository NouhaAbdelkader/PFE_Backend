package com.example.pfe_backend.entities.chatRoom;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class Media {
    @Id
    private String id;

    private Long userId;
    private String fileType;
    private byte[] picture;
    private String title;
    private Integer chatId;
    private MediaType mediaType;

    // Corriger la relation
    @OneToOne(mappedBy = "media")
    @JsonBackReference
    private message message;
}