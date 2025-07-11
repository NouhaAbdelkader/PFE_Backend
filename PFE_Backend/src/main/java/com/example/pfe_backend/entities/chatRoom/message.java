package com.example.pfe_backend.entities.chatRoom;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
public class message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Date time;

    private String replymessage;

    private String sendermessage;

    private String replyMedia;
    // @Lob
    // private byte[] replyMediaContent;

    @ManyToOne
    @JoinColumn(name = "chat_room_id")
    @JsonBackReference
    private ChatRoom chatRoom;

    // Ajouter la relation avec Media
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "media_id")
    private Media media;
    // Nouveau champ pour suivre les utilisateurs ayant vu le message
    @ElementCollection
    @CollectionTable(name = "message_seen_by", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "user_id")
    private List<Long> seenBy = new ArrayList<>();
    public message() {
    }
}