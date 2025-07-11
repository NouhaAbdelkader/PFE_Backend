package com.example.pfe_backend.repos.ClientRepo;

 import com.example.pfe_backend.entities.ClientImpactes.ClientImpacte;
import org.springframework.data.jpa.repository.JpaRepository;

 import java.util.Optional;

public interface ClientRepository extends JpaRepository<ClientImpacte, Long> {
 Optional<ClientImpacte> findByEmail(String email);
 Optional<ClientImpacte> findByIdClient(Long idClient);
 Optional<ClientImpacte> findByClientRef(String clientRef);
}
