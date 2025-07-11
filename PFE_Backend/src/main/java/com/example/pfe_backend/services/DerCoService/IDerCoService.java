package com.example.pfe_backend.services.DerCoService;

import com.example.pfe_backend.entities.DerCo.Derco;
import com.example.pfe_backend.entities.DerCo.DercoCsvDTO;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IDerCoService {
    public void addDerCo(List<DercoCsvDTO> dercoCsvList);
    public List<Derco> getAll();
    public Derco updateDerco( Derco updatedDerco);
    public  Derco newDerCo( Derco derco);
    public List<Derco> newDercos();
    public void deleteDerco(Long id);

}
