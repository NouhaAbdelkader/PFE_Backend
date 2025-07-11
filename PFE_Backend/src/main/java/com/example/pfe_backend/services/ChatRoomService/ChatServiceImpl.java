package com.example.pfe_backend.services.ChatRoomService;

import com.example.pfe_backend.entities.chatRoom.ChatRoom;
import com.example.pfe_backend.entities.chatRoom.MediaType;
import com.example.pfe_backend.entities.chatRoom.message;
import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import com.example.pfe_backend.exceptions.CustomException;
import com.example.pfe_backend.repos.ChatRoomRepo.ChatRepository;
import com.example.pfe_backend.repos.ChatRoomRepo.MessageRepo;
import com.example.pfe_backend.repos.NotifixUserRepo.NotifixUserRepo;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@AllArgsConstructor
public class ChatServiceImpl {
    private NotifixUserRepo userRepository;
    private ChatRepository chatRepository;
    private MediaServiceImpl mediaService;
    private MessageRepo messageRepository ;
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatServiceImpl.class);

    public ChatRoom addChat(ChatRoom chat, Long idSender, Long idReceiver) {
        LOGGER.info("Adding chat for senderId: {}, receiverId: {}", idSender, idReceiver);
        NotifixUser sender = userRepository.findNotifixUserByUserId(idSender);
        NotifixUser receiver = userRepository.findNotifixUserByUserId(idReceiver);
        if (sender == null) {
            LOGGER.error("Sender not found with ID: {}", idSender);
            throw new CustomException("Sender not found with ID: " + idSender);
        }
        if (receiver == null) {
            LOGGER.error("Receiver not found with ID: {}", idReceiver);
            throw new CustomException("Receiver not found with ID: " + idReceiver);
        }
        chat.setSender(sender);
        chat.setReceiver(receiver);
        chat.setMessages(new ArrayList<>());
        ChatRoom savedChat = chatRepository.save(chat);
        LOGGER.info("Chat saved with ID: {}", savedChat.getId());
        return savedChat;
    }
    public void deleteChatById(int id) {
        chatRepository.deleteById(id);
    }

    public ChatRoom getChatById(int id) {
        return chatRepository.findById(id).orElseThrow(() -> new RuntimeException());
    }

    public HashSet<ChatRoom> getChatsByUser(Long userId) throws ChatNotFoundException {
        NotifixUser user = userRepository.findNotifixUserByUserId(userId);
        if (user == null) {
            throw new ChatNotFoundException( );
        }
        HashSet<ChatRoom> chats = new HashSet<>();
        chats.addAll(chatRepository.findChatRoomBySender(user));
        chats.addAll(chatRepository.findChatRoomByReceiver(user));
        if (chats.isEmpty()) {
            throw new ChatNotFoundException( );
        }
        return chats;
    }

    public ChatRoom getChatBetweenUsers(Long userId1, Long userId2) {
        NotifixUser user1 = userRepository.findNotifixUserByUserId(userId1);
        NotifixUser user2 = userRepository.findNotifixUserByUserId(userId2);
        if (user1 == null || user2 == null) {
            return null;
        }

        ChatRoom chatRoom = chatRepository.findChatRoomBySenderAndReceiver(user1, user2);
        if (chatRoom != null) {
            return chatRoom;
        }
        return chatRepository.findChatRoomBySenderAndReceiver(user2, user1);
    }

    public ChatRoom addMessage(message message, int chatId, Long senderId) throws ChatNotFoundException {
        ChatRoom chatRoom = chatRepository.findById(chatId)
                .orElseThrow(() -> new CustomException("Chat not found with ID: " + chatId));

        if (message.getTime() == null) {
            message.setTime(new Date());
        }
        if (message.getChatRoom() == null) {
            message.setChatRoom(chatRoom);
        }

        List<message> messages = chatRoom.getMessages();
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
        chatRoom.setMessages(messages);

        ChatRoom savedChatRoom = chatRepository.save(chatRoom);
        LOGGER.info("Message added to chat room: {}", savedChatRoom.getId());
        return savedChatRoom;
    }

    public ChatRoom addMediaMessage(message message, int chatId, MultipartFile image, String fileType, Long userId, MediaType mediaType) throws ChatNotFoundException {
        ChatRoom chatRoom = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException());

        if (message.getTime() == null) {
            message.setTime(new Date());
        }
        if (message.getChatRoom() == null) {
            message.setChatRoom(chatRoom);
        }

        String uploadedMediaId = null;
        try {
            uploadedMediaId = mediaService.uploadMessageMedia(chatId, image, mediaType, userId, fileType);
            message.setReplyMedia(uploadedMediaId);
        } catch (Exception e) {
            LOGGER.error("Error uploading media: {}", e.getMessage(), e);
            throw new CustomException("Failed to upload media.");
        }

        List<message> messages = chatRoom.getMessages();
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
        chatRoom.setMessages(messages);

        ChatRoom savedChatRoom = chatRepository.save(chatRoom);
        LOGGER.info("Media message added to chat room: {}", savedChatRoom.getId());
        return savedChatRoom;
    }
    public List<message> markMessagesAsSeen(Integer chatId, Long userId) {
        LOGGER.info("Marking messages as seen for chatId: {}, userId: {}", chatId, userId);
        ChatRoom chatRoom = chatRepository.findById(chatId)
                .orElseThrow(() -> new CustomException("Chat not found with ID: " + chatId));

        NotifixUser user = userRepository.findNotifixUserByUserId(userId);
        if (userId == null) {
            LOGGER.error("User not found with ID: {}", userId);
            throw new CustomException("User not found with ID: " + userId);
        }

        // Trouver les messages non lus
        List<message> unreadMessages = messageRepository.findUnreadMessagesByChatRoomAndUser(chatId, userId);

        // Marquer chaque message comme lus
        for (message message : unreadMessages) {
            List<Long> seenBy = message.getSeenBy();
            if (!seenBy.contains(userId)) {
                seenBy.add(userId);
                message.setSeenBy(seenBy);
                messageRepository.save(message);
                LOGGER.info("Message ID {} marked as seen by userId: {}", message.getId(), userId);
            }
        }

        return unreadMessages; // Retourner les messages marqués pour notification
    }
}