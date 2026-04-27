package com.tsm_db_sql.db.wiam.service.inventario;

import com.tsm_db_sql.db.wiam.exception.InventarioException;
import com.tsm_db_sql.db.wiam.model.request.AggiungiVenditaRequest;
import com.tsm_db_sql.db.wiam.model.response.BaseResponse;
import com.tsm_db_sql.db.wiam.repository.InventarioRepository;
import com.tsm_db_sql.db.wiam.repository.UtenteRepository;
import com.tsm_db_sql.db.wiam.utils.StatoAcq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Servizio per registrare la vendita di un item già acquistato.
 *
 * Flusso:
 * 1. Valida la request
 * 2. Cerca l'utente per username
 * 3. Cerca l'item per ID e verifica che appartenga all'utente
 * 4. Verifica che l'item sia in stato "Acquistato" (non già venduto o cancellato)
 * 5. Imposta i dati della vendita e calcola il prezzo netto
 * 6. Aggiorna lo stato a "Venduto"
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AggiungiVenditaService {

    private final UtenteRepository utenteRepository;
    private final InventarioRepository inventarioRepository;

    public BaseResponse aggiungiVendita(AggiungiVenditaRequest request) {

        log.info("AggiungiVendita service avviato con request: {}", request);

        // Passo 1: validazione dei campi obbligatori
        request.validaRequest();

        // Passo 2: cerco l'utente nel DB — se non esiste, errore 404
        var utente = utenteRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.error("Utente non trovato con username: {}", request.username());
                    return new InventarioException(
                            "Utente non trovato con username: " + request.username(), "ERR-INV-404");
                });

        // Passo 3: cerco l'item per ID nel repository.
        // Poi verifico che l'item appartenga effettivamente all'utente che fa la richiesta,
        // confrontando l'ID dell'utente proprietario dell'item con quello trovato per username.
        // Questo previene che un utente possa vendere item di un altro utente.
        var item = inventarioRepository.findById(request.itemId())
                .orElseThrow(() -> {
                    log.error("Item inventario non trovato con id: {}", request.itemId());
                    return new InventarioException(
                            "Item inventario non trovato con id: " + request.itemId(), "ERR-INV-404");
                });

        // Verifica appartenenza: l'item deve essere dell'utente che fa la request
        if (!item.getUtente().getId().equals(utente.getId())) {
            log.error("Item id {} non appartiene all'utente: {}", request.itemId(), request.username());
            throw new InventarioException(
                    "Item non appartiene all'utente indicato", "ERR-INV-403");
        }

        // Passo 4: verifico che l'item sia in stato "Acquistato".
        // Non si può vendere un item già venduto (serve prima annullare la vendita)
        // e non si può vendere un item cancellato.
        if (item.getStatoAcquisto() != StatoAcq.Acquistato) {
            log.error("Item id {} non è in stato Acquistato, stato attuale: {}",
                    request.itemId(), item.getStatoAcquisto());
            throw new InventarioException(
                    "L'item non è in stato Acquistato, impossibile registrare vendita. Stato attuale: "
                            + item.getStatoAcquisto(), "ERR-INV-400");
        }

        // Passo 5: imposto i dati della vendita sull'item.
        // Il prezzo e i costi vengono convertiti da stringa a BigDecimal per precisione decimale.
        var prezzoVendita = new BigDecimal(request.prezzoVendita());
        item.setPrezzoVendita(prezzoVendita);
        item.setPiattaformaVendita(request.piattaformaVendita());
        item.setDataVendita(LocalDate.parse(request.dataVendita()));

        // I costi accessori sono opzionali — se non forniti, li imposto a zero.
        // I costi accessori rappresentano spese extra (spedizione, commissioni piattaforma, ecc.)
        var costiAccessori = (request.costiAccessori() != null && !request.costiAccessori().isBlank())
                ? new BigDecimal(request.costiAccessori())
                : BigDecimal.ZERO;
        item.setCostiAccessori(costiAccessori);

        // Calcolo il prezzo netto: prezzo di vendita meno i costi accessori.
        // Questo rappresenta il guadagno reale dalla vendita.
        item.setPrezzoNetto(prezzoVendita.subtract(costiAccessori));

        // Passo 6: aggiorno lo stato dell'acquisto a "Venduto"
        item.setStatoAcquisto(StatoAcq.Venduto);

        // Salvo le modifiche — JPA fa UPDATE perché l'entity è già managed (ha un ID)
        inventarioRepository.save(item);

        log.info("AggiungiVendita service completato con successo per item id: {}", request.itemId());
        return new BaseResponse("Vendita registrata con successo");
    }
}
