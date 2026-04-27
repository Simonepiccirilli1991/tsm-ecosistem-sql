package com.tsm_db_sql.db.wiam.service.inventario;

import com.tsm_db_sql.db.wiam.exception.InventarioException;
import com.tsm_db_sql.db.wiam.model.request.InventarioInquiryRequest;
import com.tsm_db_sql.db.wiam.model.response.InventarioInquiryResponse;
import com.tsm_db_sql.db.wiam.repository.InventarioRepository;
import com.tsm_db_sql.db.wiam.utils.InventarioSpecifications;
import com.tsm_db_sql.db.wiam.repository.UtenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

/**
 * Servizio per la ricerca multi-criterio nell'inventario di un utente.
 *
 * Flusso:
 * 1. Valida la request (username obbligatorio)
 * 2. Cerca l'utente per username
 * 3. Costruisce una Specification dinamica combinando solo i filtri presenti (AND)
 * 4. Esegue la query paginata sul repository
 * 5. Mappa i risultati da entity a response DTO
 *
 * Approccio tecnico:
 * Usa JPA Specifications (Criteria API) per costruire query dinamiche.
 * Ogni filtro opzionale viene aggiunto alla Specification solo se il valore
 * nella request non è null/vuoto. Questo evita di scrivere N metodi di repository
 * per ogni combinazione di filtri possibile.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventarioInquiryService {

    private final UtenteRepository utenteRepository;
    private final InventarioRepository inventarioRepository;

    public Page<InventarioInquiryResponse> inquiry(InventarioInquiryRequest request) {

        log.info("InventarioInquiry service avviato con request: {}", request);

        // Passo 1: validazione — solo username è obbligatorio
        request.validaRequest();

        // Passo 2: cerco l'utente nel DB — se non esiste, errore 404
        var utente = utenteRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.error("Utente non trovato con username: {}", request.username());
                    return new InventarioException(
                            "Utente non trovato con username: " + request.username(), "ERR-INV-404");
                });

        // Passo 3: costruisco la Specification dinamica.
        // Parto dal filtro "perUtente" che è SEMPRE applicato (ogni inquiry è scoped su un utente).
        // Poi aggiungo i filtri opzionali solo se il valore corrispondente nella request è presente.
        // Il metodo .and() combina le Specification in AND logico.
        var spec = Specification.where(InventarioSpecifications.perUtente(utente));

        // Filtro range prezzo acquisto — prezzoAcquistoDa (>=)
        if (!ObjectUtils.isEmpty(request.prezzoAcquistoDa())) {
            spec = spec.and(InventarioSpecifications.prezzoAcquistoDa(
                    request.prezzoAcquistoDa()));
        }

        // Filtro range prezzo acquisto — prezzoAcquistoA (<=)
        if (!ObjectUtils.isEmpty(request.prezzoAcquistoA())) {
            spec = spec.and(InventarioSpecifications.prezzoAcquistoA(
                    request.prezzoAcquistoA()));
        }

        // Filtro range data acquisto — dataAcquistoDa (>=)
        if (!ObjectUtils.isEmpty(request.dataAcquistoDa())) {
            spec = spec.and(InventarioSpecifications.dataAcquistoDa(
                    request.dataAcquistoDa()));
        }

        // Filtro range data acquisto — dataAcquistoA (<=)
        if (!ObjectUtils.isEmpty(request.dataAcquistoA())) {
            spec = spec.and(InventarioSpecifications.dataAcquistoA(
                    request.dataAcquistoA()));
        }

        // Filtro stato acquisto (Acquistato, Venduto, Cancellato)
        if (request.statoAcquisto() != null) {
            spec = spec.and(InventarioSpecifications.perStatoAcquisto(request.statoAcquisto()));
        }

        // Filtro brand prodotto (Pokemon, OnePiece, Lego, Generico)
        if (request.brandProdotto() != null) {
            spec = spec.and(InventarioSpecifications.perBrand(request.brandProdotto()));
        }

        // Filtro tipo prodotto (BoxSealed, CartaSingola, ecc.)
        if (request.tipoProdotto() != null) {
            spec = spec.and(InventarioSpecifications.perTipoProdotto(request.tipoProdotto()));
        }

        // Passo 4: eseguo la query paginata.
        // I valori di page e size hanno default (0 e 20) se non specificati nella request.
        // Ordino per dataAcquisto decrescente — gli acquisti più recenti appaiono per primi.
        var page = (request.page() != null) ? request.page() : 0;
        var size = (request.size() != null) ? request.size() : 20;
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dataAcquisto"));

        var risultati = inventarioRepository.findAll(spec, pageable);

        log.info("InventarioInquiry completato: {} risultati trovati (pagina {}/{})",
                risultati.getTotalElements(), page, risultati.getTotalPages());

        // Passo 5: mappo le entity a response DTO usando il factory method.
        // Page.map() applica la trasformazione a ogni elemento mantenendo la struttura paginata.
        return risultati.map(InventarioInquiryResponse::fromEntity);
    }
}
