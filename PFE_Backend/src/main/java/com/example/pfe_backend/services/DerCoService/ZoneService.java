package com.example.pfe_backend.services.DerCoService;

import com.example.pfe_backend.entities.DerCo.Zone;
import com.example.pfe_backend.repos.DerCoRepo.ZoneRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@AllArgsConstructor
public class ZoneService implements IzoneService {
    private final ZoneRepository zoneRepo;

    @Override
    public List<Zone> getAll() {
        return zoneRepo.findAll();
    }
}
