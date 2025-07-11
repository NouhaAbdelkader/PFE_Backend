package com.example.pfe_backend.repos.ChatRoomRepo;

import com.example.pfe_backend.entities.chatRoom.message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepo extends JpaRepository<message, Integer> {
    // Trouver les messages non lus par un utilisateur dans une conversation
    @Query("SELECT m FROM message m WHERE m.chatRoom.id = :chatRoomId AND :userId NOT IN (SELECT s FROM m.seenBy s)")
    List<message> findUnreadMessagesByChatRoomAndUser(@Param("chatRoomId") Integer chatRoomId, @Param("userId") Long userId);
}