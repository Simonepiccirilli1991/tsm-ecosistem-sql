package com.tsm_db_sql.db.wiam.service.inventario;

import com.tsm_db_sql.db.wiam.entity.UtenteInventario;
import com.tsm_db_sql.db.wiam.exception.InventarioException;
import com.tsm_db_sql.db.wiam.model.request.AggiungiAcquistoRequest;
import com.tsm_db_sql.db.wiam.model.response.BaseResponse;
import com.tsm_db_sql.db.wiam.repository.InventarioRepository;
import com.tsm_db_sql.db.wiam.repository.UtenteRepository;
import com.tsm_db_sql.db.wiam.utils.StatoAcq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Servizio per aggiungere un nuovo acquisto all'inventario di un utente.
 *
 * Flusso:
 * 1. Valida i campi obbligatori della request
 * 2. Cerca l'utente per username (se non esiste → errore 404)
 * 3. Converte i valori stringa della request negli enum corrispondenti
 * 4. Crea l'entity UtenteInventario con statoAcquisto = Acquistato
 * 5. Sincronizza la relazione bidirezionale con l'utente
 * 6. Salva l'item nel database
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AggiungiAcquistoService {

    private final UtenteRepository utenteRepository;
    private final InventarioRepository inventarioRepository;

    public BaseResponse aggiungiAcquisto(AggiungiAcquistoRequest request) {

        log.info("AggiungiAcquisto service avviato con request: {}", request);

        // Passo 1: validazione dei campi obbligatori della request
        request.validaRequest();

        // Passo 2: cerco l'utente nel DB tramite username — se non esiste, lancio eccezione 404
        var utente = utenteRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.error("Utente non trovato con username: {}", request.username());
                    return new InventarioException(
                            "Utente non trovato con username: " + request.username(), "ERR-INV-404");
                });

        // Passo 4: creo la nuova entity UtenteInventario con i dati dell'acquisto.
        // Lo statoAcquisto viene impostato a "Acquistato" — è lo stato iniziale di ogni item.
        var item = new UtenteInventario();
        item.setNomeAcquisto(request.nomeAcquisto());
        item.setCodiceAcquisto(request.codiceAcquisto());
        item.setDataAcquisto(LocalDate.parse(request.dataAcquisto()));
        item.setDescrizione(request.descrizione());
        item.setBrandProdotto(request.brandProdotto());
        item.setTipoProdotto(request.tipoProdotto());
        item.setStatoProdotto(request.statoProdotto());
        // Stato acquisto iniziale: ogni nuovo item parte come "Acquistato"
        item.setStatoAcquisto(StatoAcq.Acquistato);

        // Passo 5: uso il metodo helper dell'entity Utente per sincronizzare
        // entrambi i lati della relazione bidirezionale (utente → item e item → utente).
        // Senza questa sincronizzazione, il grafo oggetti JPA in memoria sarebbe inconsistente.
        utente.aggiungiInventarioItem(item);

        // Passo 6: salvo l'item tramite il repository.
        // Grazie al CascadeType.ALL sulla relazione, si potrebbe anche salvare tramite
        // utenteRepository.save(utente), ma salvare direttamente l'item è più esplicito.
        inventarioRepository.save(item);

        log.info("AggiungiAcquisto service completato con successo per utente: {}", request.username());
        return new BaseResponse("Acquisto aggiunto con successo all'inventario");
    }
}
