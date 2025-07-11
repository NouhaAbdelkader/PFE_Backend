package com.example.pfe_backend.services.NotifixUserservice;

import com.example.pfe_backend.Configurations.JWT.JwtResponse;
import com.example.pfe_backend.Configurations.JWT.JwtUtils;
import com.example.pfe_backend.entities.chatRoom.ChatRoom;
import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import com.example.pfe_backend.entities.notifixUser.Role;
import com.example.pfe_backend.entities.notifixUser.Status;
import com.example.pfe_backend.exceptions.CustomException;
import com.example.pfe_backend.repos.ChatRoomRepo.ChatRepository;
import com.example.pfe_backend.repos.NotifixUserRepo.NotifixUserRepo;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
public class NotifixUserService implements INotifixUserService {
    private static final Logger logger = LoggerFactory.getLogger(NotifixUserService.class);
    private final NotifixUserRepo notifixUserRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final ChatRepository chatRepository;

    @Override
    public NotifixUser addNotifixUser(NotifixUser notifixUser) {
        if (notifixUserRepo.existsNotifixUserByEmail(notifixUser.getEmail())) {
            throw new CustomException("There is already a Notifix user with this email");
        }
        if (notifixUserRepo.existsNotifixUserByNumero(notifixUser.getNumero())) {
            throw new CustomException("There is already a Notifix user with this number");
        }
        String encodedPassword = passwordEncoder.encode(notifixUser.getPassword());
        notifixUser.setPassword(encodedPassword);
        notifixUser.setStatus(Status.INACTIVE);
        NotifixUser savedUser = notifixUserRepo.save(notifixUser);
        logger.info("Added new user: email={}", savedUser.getEmail());
        return savedUser;
    }

    @Override
    public List<NotifixUser> getUsersNotifix() {
        List<NotifixUser> users = notifixUserRepo.findAll();
        Collections.reverse(users);
        logger.info("Retrieved {} users", users.size());
        return users;
    }
    @Override
    public List<NotifixUser> fetchFilteredUsers(String email, String nom, String prenom, Status status, Boolean pending, int page, int size) {
        logger.info("Fetching filtered users with email={}, nom={}, prenom={}, status={}, pending={}, page={}, size={}",
                email, nom, prenom, status, pending, page, size);

        // Normalize empty strings to null for query
        String emailFilter = (email != null && !email.trim().isEmpty()) ? email.trim() : null;
        String nomFilter = (nom != null && !nom.trim().isEmpty()) ? nom.trim() : null;
        String prenomFilter = (prenom != null && !prenom.trim().isEmpty()) ? prenom.trim() : null;

        // Create pageable object for pagination
        Pageable pageable = PageRequest.of(page - 1, size);

        // Fetch filtered users from repository
        List<NotifixUser> filteredUsers = notifixUserRepo.findByFilters(emailFilter, nomFilter, prenomFilter, status, pending);

        // Apply pagination manually since JPA query doesn't support it directly
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredUsers.size());
        List<NotifixUser> paginatedUsers = filteredUsers.subList(start, end);

        logger.info("Found {} users, returning {} after pagination", filteredUsers.size(), paginatedUsers.size());
        return paginatedUsers;
    }
    @Override
    public long countFilteredUsers(String email, String nom, String prenom, Status status, Boolean pending) {
        String emailFilter = (email != null && !email.trim().isEmpty()) ? email.trim() : null;
        String nomFilter = (nom != null && !nom.trim().isEmpty()) ? nom.trim() : null;
        String prenomFilter = (prenom != null && !prenom.trim().isEmpty()) ? prenom.trim() : null;

        List<NotifixUser> filteredUsers = notifixUserRepo.findByFilters(emailFilter, nomFilter, prenomFilter, status, pending);
        logger.info("Counted {} filtered users", filteredUsers.size());
        return filteredUsers.size();
    }

    @Override
    public JwtResponse login(String email, String password) {
        NotifixUser user = notifixUserRepo.findByEmail(email);

        if (user == null) {
            logger.error("Login failed: No user with email {}", email);
            throw new CustomException("Login failed. There is no user with this email");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            logger.error("Login failed: Invalid password for email {}", email);
            throw new CustomException("Login failed. Invalid password");
        }
        user.setStatus(Status.ACTIVE);
        notifixUserRepo.save(user);
        String token = jwtUtils.generateJwtToken(user);
        String refreshToken = jwtUtils.generateRefreshToken(user);
        logger.info("User logged in: email={}", email);
        return new JwtResponse(token, user, refreshToken);
    }

    @Override
    public NotifixUser changePassword(NotifixUser notifixUser) {
        String encodedPassword = passwordEncoder.encode(notifixUser.getPassword());
        notifixUser.setPassword(encodedPassword);
        NotifixUser updatedUser = notifixUserRepo.save(notifixUser);
        logger.info("Password changed for user: email={}", updatedUser.getEmail());
        return updatedUser;
    }

    @Override
    public Boolean verifPassword(NotifixUser notifixUser, String currentPassword) {
        boolean isValid = passwordEncoder.matches(currentPassword, notifixUser.getPassword());
        logger.info("Password verification for user email={}: {}", notifixUser.getEmail(), isValid);
        return isValid;
    }

    @Override
    public NotifixUser getUserByEmail(String email) {
        NotifixUser user = notifixUserRepo.findByEmail(email);
        if (user == null) {
            logger.error("User not found with email: {}", email);
            throw new CustomException("User not found with email: " + email);
        }
        logger.info("Retrieved user by email: {}", email);
        return user;
    }

    @Override
    public NotifixUser updateNotifixUser(NotifixUser notifixUser) {
        NotifixUser updatedUser = notifixUserRepo.save(notifixUser);
        logger.info("Updated user: email={}", updatedUser.getEmail());
        return updatedUser;
    }

    @Override
    @Transactional
    public void deleteNotifixUser(Long notifixUserId) {
        if (!notifixUserRepo.existsById(notifixUserId)) {
            logger.error("User not found with ID: {}", notifixUserId);
            throw new CustomException("NotifixUser not found");
        }
        NotifixUser user = notifixUserRepo.findNotifixUserByUserId(notifixUserId);
        Set<ChatRoom> chatRooms = chatRepository.findChatRoomByReceiver(user);
        Set<ChatRoom> chatRooms1 = chatRepository.findChatRoomBySender(user);
        chatRooms.addAll(chatRooms1);
        for (ChatRoom chatRoom : chatRooms) {
            chatRepository.delete(chatRoom);
            logger.info("Deleted chat room for user ID: {}", notifixUserId);
        }
        notifixUserRepo.deleteById(notifixUserId);
        logger.info("Deleted user with ID: {}", notifixUserId);
    }

    @Override
    public void logout(String email) {
        NotifixUser user = notifixUserRepo.findByEmail(email);
        if (user == null) {
            logger.error("Logout failed: User not found with email {}", email);
            throw new CustomException("User not found");
        }
        user.setStatus(Status.INACTIVE);
        notifixUserRepo.save(user);
        logger.info("User logged out: email={}", email);
    }

    @Override
    public NotifixUser getUserByid(Long id) {
        NotifixUser user = notifixUserRepo.findById(id)
                .orElseThrow(() -> {
                    logger.error("User not found with ID: {}", id);
                    return new CustomException("User not found with ID: " + id);
                });
        logger.info("Retrieved user by ID: {}", id);
        return user;
    }

    @Transactional
    public NotifixUser updateUserPhoto(Long userId, String base64Photo) {
        NotifixUser user = notifixUserRepo.findById(userId)
                .orElseThrow(() -> {
                    logger.error("User not found with ID: {}", userId);
                    return new IllegalArgumentException("User with ID " + userId + " not found");
                });

        if (base64Photo == null || base64Photo.trim().isEmpty()) {
            logger.error("Photo cannot be empty for user ID: {}", userId);
            throw new IllegalArgumentException("Photo cannot be empty");
        }

        if (!base64Photo.matches("^data:image/(png|jpeg|jpg);base64,.+$")) {
            logger.error("Invalid Base64 image format for user ID: {}", userId);
            throw new IllegalArgumentException("Invalid Base64 image format");
        }

        user.setPhoto(base64Photo);
        NotifixUser updatedUser = notifixUserRepo.save(user);
        logger.info("Updated photo for user ID: {}", userId);
        return updatedUser;
    }

    public List<NotifixUser> findByRole(Role role) {
        List<NotifixUser> users = notifixUserRepo.findByRole(role);
        logger.info("Found {} users with role: {}", users.size(), role);
        return users;
    }
}