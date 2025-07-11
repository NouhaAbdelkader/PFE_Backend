package com.example.pfe_backend.services.clientService;

import com.example.pfe_backend.entities.ClientImpactes.ClientImpacte;

import java.util.List;

public interface IClientService {
    public List<ClientImpacte> fetchClientImpactesFromApi();
    public List<ClientImpacte> fetchClientImpactesFromPolygonEquipement( List<String> coordonnees);
    public List<ClientImpacte> fetchClientImpactesFromCercleEquipement(Double lat, Double Long, Double rayon);


    public ClientImpacte addClient(ClientImpacte client);
    public void deleteClient(Long idClient);
    public ClientImpacte updateClient(ClientImpacte c) ;
    public List<ClientImpacte> getAllClients();
    public ClientImpacte getClient(Long idClient) ;
}
