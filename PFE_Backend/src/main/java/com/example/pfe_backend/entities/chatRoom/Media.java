package com.example.pfe_backend.entities.chatRoom;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Media {
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String  id;
    private Integer chatId;
    private Long userId;
    private String title;
    @Lob
    private byte[] picture;     private String fileType;
    @Enumerated(EnumType.STRING)

    private MediaType mediaType;
}
