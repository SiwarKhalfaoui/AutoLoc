package tn.esprit.autoloc.service;

import java.util.List;
import tn.esprit.autoloc.domain.Paiement;

/**
 * Lecture seule : un paiement est créé, modifié et supprimé uniquement
 * à travers son contrat (cascade ALL + orphanRemoval sur Contrat.paiements).
 */
public interface IPaiementService {

    Paiement findById(Long id);

    List<Paiement> findAll();
}