package com.tgv.tgv_workfloor.service;

import com.tgv.tgv_workfloor.depot.ClientDepot;
import com.tgv.tgv_workfloor.depot.CommandeDepot;
import com.tgv.tgv_workfloor.exception.RegleMetierException;
import com.tgv.tgv_workfloor.exception.RessourceIntrouvableException;
import com.tgv.tgv_workfloor.modele.Client;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService {

    private final ClientDepot clients;
    private final CommandeDepot commandes;

    public ClientService(ClientDepot clients, CommandeDepot commandes) {
        this.clients = clients;
        this.commandes = commandes;
    }

    public List<Client> lister() {
        return clients.findAllByOrderByNom();
    }

    public Client trouver(Long id) {
        return clients.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Client", id));
    }

    public int nombreDeCommandes(Client client) {
        return commandes.findByClient(client).size();
    }

    public synchronized Client creer(Client client) {
        client.setId(null);
        return clients.save(client);
    }

    public synchronized Client modifier(Long id, Client donnees) {
        Client client = trouver(id);
        client.setNom(donnees.getNom());
        client.setAdresse(donnees.getAdresse());
        client.setCodePostal(donnees.getCodePostal());
        client.setVille(donnees.getVille());
        client.setTelephone(donnees.getTelephone());
        client.setLatitude(donnees.getLatitude());
        client.setLongitude(donnees.getLongitude());
        return client;
    }

    public synchronized void supprimer(Long id) {
        Client client = trouver(id);
        int nb = commandes.findByClient(client).size();
        if (nb > 0) {
            throw new RegleMetierException("Le client « " + client.getNom() + " » a " + nb
                    + " commande(s) : supprimez-les avant de supprimer le client.");
        }
        clients.delete(client);
    }
}
