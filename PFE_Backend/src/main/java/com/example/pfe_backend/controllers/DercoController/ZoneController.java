package com.example.pfe_backend.controllers.DercoController;

import com.example.pfe_backend.entities.DerCo.Derco;
import com.example.pfe_backend.entities.DerCo.Zone;
import com.example.pfe_backend.services.DerCoService.ZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/zone")
@RequiredArgsConstructor
public class ZoneController {
    private final ZoneService zoneService;
    @GetMapping("/all")
    public ResponseEntity<List<Zone>> getAllZones() {
        List<Zone> zones = zoneService.getAll();
        return ResponseEntity.ok(zones);
    }
}
