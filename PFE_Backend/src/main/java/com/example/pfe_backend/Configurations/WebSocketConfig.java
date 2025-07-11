package com.example.pfe_backend.Configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration de WebSocket pour activer la communication en temps réel via le protocole STOMP.
 * Cette classe configure un courtier de messages (message broker) et un point de terminaison WebSocket,
 * permettant aux clients de se connecter et de recevoir des messages en temps réel.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure le courtier de messages (message broker) pour gérer la communication WebSocket.
     * Définit les préfixes pour les destinations des messages et configure les heartbeats pour
     * maintenir les connexions actives.
     *
      */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Active un courtier simple pour diffuser des messages vers les clients abonnés aux destinations commençant par "/topic"
        config.enableSimpleBroker("/topic")
                // Configure les intervalles de heartbeat (4000 ms pour client->serveur et serveur->client)
                // pour vérifier que la connexion est toujours active
                .setHeartbeatValue(new long[]{4000, 4000})
                // Associe un planificateur de tâches pour gérer les heartbeats
                .setTaskScheduler(taskScheduler());
        // Définit le préfixe pour les messages envoyés par l'application (client -> serveur)
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Enregistre un point de terminaison WebSocket pour permettre aux clients de se connecter.
     * Configure l'URL du point de terminaison et active le support SockJS pour la compatibilité
     * avec les navigateurs ne prenant pas en charge WebSocket nativement.
     *
     * @param registry Le registre des points de terminaison STOMP.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Ajoute un point de terminaison WebSocket à l'URL "/ws"
        registry.addEndpoint("/ws")
                // Autorise les connexions depuis l'origine "http://localhost:4200" (par exemple, une application Angular)
                .setAllowedOrigins("http://localhost:4200")
                // Active le fallback SockJS pour les clients sans support WebSocket
                .withSockJS();
    }

    /**
     * Crée et configure un planificateur de tâches pour gérer les heartbeats du courtier de messages.
     * Les heartbeats permettent de maintenir les connexions WebSocket actives en envoyant des messages périodiques.
     *
     * @return Un ThreadPoolTaskScheduler configuré pour les heartbeats.
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        // Crée un planificateur avec un pool de threads
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        // Définit la taille du pool de threads à 1 (suffisant pour les heartbeats)
        scheduler.setPoolSize(1);
        // Définit un préfixe pour les noms des threads créés
        scheduler.setThreadNamePrefix("wss-heartbeat-thread-");
        // Initialise le planificateur
        scheduler.initialize();
        return scheduler;
    }
}