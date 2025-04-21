package com.example.pfe_backend.repos.ChatRoomRepo;

import com.example.pfe_backend.entities.chatRoom.Media;
import com.example.pfe_backend.entities.chatRoom.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MediaRepository extends JpaRepository<Media, String> {
    Optional<Media> findOneByUserIdAndMediaType(Long userId, MediaType mediaType);

    Media findMediaById(String id );
}
