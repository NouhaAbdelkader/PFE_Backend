package com.example.pfe_backend.services.ChatRoomService;

import com.example.pfe_backend.entities.chatRoom.ChatRoom;
import com.example.pfe_backend.entities.chatRoom.MediaType;
import com.example.pfe_backend.entities.chatRoom.message;
import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import com.example.pfe_backend.exceptions.CustomException;
import com.example.pfe_backend.repos.ChatRoomRepo.ChatRepository;
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
    private MediaServiceImpl mediaService ;
    // private String text= "hello this jhon. you should work more . we are happy to see you. but i dont understand this. and i hate the way you act  ";

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatServiceImpl.class);

    public ChatRoom addChat(ChatRoom chat, Long idSender, Long idReceiver) {
        NotifixUser sender = userRepository.findNotifixUserByUserId(idSender);
        NotifixUser receiver = userRepository.findNotifixUserByUserId(idReceiver);
        if (sender != null && receiver != null) {
            chat.setSender(sender);
            chat.setReceiver(receiver);
            return chatRepository.save(chat);
        }
        return null;
    }

    public void deleteChatById(int id) {
        chatRepository.deleteById(id);
    }

    public ChatRoom getChatById(int id) {
        return chatRepository.findById(id).orElse(null);



    }

    public HashSet<ChatRoom> getChatsByUser(Long userId) throws ChatNotFoundException {
        NotifixUser u= userRepository.findNotifixUserByUserId(userId);
        HashSet<ChatRoom> chat = chatRepository.findChatRoomByReceiver(u);
        HashSet<ChatRoom> chat1 = chatRepository.findChatRoomBySender(u);

        chat1.addAll(chat);

        if (chat.isEmpty() && chat1.isEmpty()) {
            throw new ChatNotFoundException();
        } else if (chat1.isEmpty()) {
            return chat;
        } else {
            return chat1;
        }
    }
    public HashSet<ChatRoom> getChatsByUSenderIOrrReciver(Long userId,Long id) throws ChatNotFoundException {
        NotifixUser u= userRepository.findNotifixUserByUserId(userId);
        NotifixUser u2= userRepository.findNotifixUserByUserId(id);
        return chatRepository.findBySenderOrReceiver(u,u2);
    }

    public ChatRoom getChatBySenderAndReceiver(Long idSender, Long idReceiver) {
        NotifixUser sender = userRepository.findNotifixUserByUserId(idSender);
        NotifixUser receiver = userRepository.findNotifixUserByUserId(idReceiver);
        return chatRepository.findChatRoomBySenderAndReceiver(sender, receiver);

    }
    public ChatRoom getChatBySenderAndReceiver2( Long idReceiver,Long idSender) {
        NotifixUser sender = userRepository.findNotifixUserByUserId(idSender);
        NotifixUser receiver = userRepository.findNotifixUserByUserId(idReceiver);
        return chatRepository.findChatRoomByReceiverAndSender(receiver,sender);

    }


    public ChatRoom addMessage(message message, int chatId, Long idSender) throws ChatNotFoundException, CustomException {
        // Récupérer l'utilisateur expéditeur
        NotifixUser sender = userRepository.findNotifixUserByUserId(idSender);
        if (sender == null) {
            throw new CustomException("User not found with ID: " + idSender);
        }
        if (chatId == 0) {
            throw new CustomException("chat not found with ID: " + chatId);
        }

        // Récupérer la salle de discussion
        Optional<ChatRoom> chatOptional = chatRepository.findById(chatId);
        ChatRoom chatRoom = chatOptional.orElseThrow(ChatNotFoundException::new);

        // Assurer que le message a un expéditeur et une heure valide


        // Ajouter le message à la salle de discussion
        List<com.example.pfe_backend.entities.chatRoom.message> messages = chatRoom.getMessages();
        if (messages == null) {
            messages = new ArrayList<>();
        }
        message.setSendermessage(sender.getNom()+ " "+ sender.getPrenom() );
        message.setTime(new Date());
        messages.add(message);
        chatRoom.setMessages(messages);

        // Sauvegarder la salle de discussion mise à jour
        ChatRoom savedChatRoom = chatRepository.save(chatRoom);


        // Logging
        LOGGER.debug("Message added to chat room: {}", savedChatRoom.getId());
        LOGGER.debug("Sender: {}", sender);

        return savedChatRoom;
    }

    public ChatRoom addMediaMessage(message message, int chatId, MultipartFile image, String fileType, Long userId, MediaType mediaType) throws ChatNotFoundException, CustomException {
        // Récupérer l'utilisateur expéditeur
        NotifixUser sender = userRepository.findNotifixUserByUserId(userId);
        if (sender == null) {
            throw new CustomException("User not found with ID: " + userId);
        }
        if (chatId == 0) {
            throw new ChatNotFoundException();
        }

        // Récupérer la salle de discussion
        Optional<ChatRoom> chatOptional = chatRepository.findById(chatId);
        ChatRoom chatRoom = chatOptional.orElseThrow(ChatNotFoundException::new);

        // Assurer que le message a un expéditeur et une heure valide
        message.setSendermessage(sender.getNom()+ " "+ sender.getPrenom());
        message.setTime(new Date());

        // Ajouter le média au message
        String uploadedMediaId = null;
        try {
            uploadedMediaId = this.mediaService.uploadMessageMedia(chatId, image, mediaType, userId, fileType);
        } catch (Exception e) {
            // Gérer l'exception, par exemple, journalisation ou traitement spécifique
            // Vous pouvez remplacer cette gestion d'erreur par celle qui convient à votre cas
            e.printStackTrace();

        }
        message.setReplyMedia(uploadedMediaId);
        message.setReplymessage(null);

        // Ajouter le message à la salle de discussion
        List<com.example.pfe_backend.entities.chatRoom.message> messages = chatRoom.getMessages();
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
        chatRoom.setMessages(messages);

        // Sauvegarder la salle de discussion mise à jour
        ChatRoom savedChatRoom = chatRepository.save(chatRoom);

        // Logging
        LOGGER.debug("Message with media added to chat room: {}", savedChatRoom.getId());
        LOGGER.debug("Sender: {}", sender);

        return savedChatRoom;
    }

}





