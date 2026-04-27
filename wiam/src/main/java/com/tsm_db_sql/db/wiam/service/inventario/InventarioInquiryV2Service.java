package com.tsm_db_sql.db.wiam.service.inventario;

import com.tsm_db_sql.db.wiam.entity.Utente;
import com.tsm_db_sql.db.wiam.exception.InventarioException;
import com.tsm_db_sql.db.wiam.model.request.InventarioInquiryRequest;
import com.tsm_db_sql.db.wiam.model.response.InventarioInquiryResponse;
import com.tsm_db_sql.db.wiam.repository.InventarioRepository;
import com.tsm_db_sql.db.wiam.repository.UtenteRepository;
import com.tsm_db_sql.db.wiam.utils.InquiryFilterType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servizio "gemello" per la ricerca multi-criterio nell'inventario — versione V2.
 *
 * A differenza di InventarioInquiryService (V1) che usa JPA Specifications (Criteria API),
 * questa versione usa un approccio basato su:
 * - Switch-case per determinare quale scenario di filtro è attivo
 * - Query JPQL personalizzate (@Query) nel repository per ogni scenario
 *
 * Vantaggi di questo approccio:
 * - Le query sono scritte esplicitamente in JPQL → più leggibili e facili da debuggare
 * - Lo switch-case rende evidente il flusso decisionale
 * - Ogni query è ottimizzata per il suo scenario specifico
 *
 * Svantaggi rispetto a Specifications:
 * - Più codice da mantenere (una query per ogni combinazione)
 * - Aggiungere un nuovo filtro richiede nuovi case e nuove query
 * - Per combinazioni di 3+ filtri serve una query "universale" con parametri opzionali
 *
 * Flusso:
 * 1. Valida la request (username obbligatorio)
 * 2. Cerca l'utente per username
 * 3. Analizza la request per determinare quale filtro è attivo (determinaFiltro)
 * 4. Switch-case sul tipo di filtro → chiama la query JPQL corretta
 * 5. Mappa i risultati da entity a response DTO
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventarioInquiryV2Service {

    private final UtenteRepository utenteRepository;
    private final InventarioRepository inventarioRepository;

    public Page<InventarioInquiryResponse> inquiry(InventarioInquiryRequest request) {

        log.info("InventarioInquiry V2 (switch-case) avviato con request: {}", request);

        // Passo 1: validazione — solo username è obbligatorio
        request.validaRequest();

        // Passo 2: cerco l'utente nel DB — se non esiste, errore 404
        var utente = utenteRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.error("Utente non trovato con username: {}", request.username());
                    return new InventarioException(
                            "Utente non trovato con username: " + request.username(), "ERR-INV-404");
                });

        // Passo 3: determino quale scenario di filtro è attivo analizzando la request.
        // Il metodo restituisce un enum InquiryFilterType che rappresenta la combinazione di filtri.
        var filtroAttivo = determinaFiltro(request);
        log.info("Filtro determinato: {}", filtroAttivo);

        // Passo 4: costruisco il Pageable per la paginazione.
        // Default: pagina 0, 20 elementi, ordinati per data acquisto decrescente.
        var page = (request.page() != null) ? request.page() : 0;
        var size = (request.size() != null) ? request.size() : 20;
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dataAcquisto"));

        // Passo 5: switch-case sul tipo di filtro.
        // Ogni case chiama una query JPQL specifica nel repository, ottimizzata per quel filtro.
        // Il case MULTIPLO usa la query universale con parametri opzionali (:param IS NULL OR ...).
        var risultati = switch (filtroAttivo) {

            // Nessun filtro — ritorna tutti gli item dell'utente
            case NESSUNO -> inventarioRepository.findByUtente(utente, pageable);

            // Filtri singoli — una sola condizione WHERE oltre all'utente
            case SOLO_STATO -> inventarioRepository.findByUtenteAndStato(
                    utente, request.statoAcquisto(), pageable);

            case SOLO_BRAND -> inventarioRepository.findByUtenteAndBrand(
                    utente, request.brandProdotto(), pageable);

            case SOLO_TIPO -> inventarioRepository.findByUtenteAndTipo(
                    utente, request.tipoProdotto(), pageable);

            // Filtri range — usano >= e <= per delimitare un intervallo.
            // Se solo un estremo è presente, l'altro viene impostato a un valore "aperto":
            // - prezzoMin default = 0 (nessun limite inferiore)
            // - prezzoMax default = 999999999 (nessun limite superiore pratico)
            // - dataDa default = 1900-01-01 (inizio storico)
            // - dataA default = 2999-12-31 (futuro lontano)
            case RANGE_PREZZO -> {
                var min = request.prezzoAcquistoDa() != null
                        ? request.prezzoAcquistoDa()
                        : java.math.BigDecimal.ZERO;
                var max = request.prezzoAcquistoA() != null
                        ? request.prezzoAcquistoA()
                        : new java.math.BigDecimal("999999999");
                yield inventarioRepository.findByUtenteAndRangePrezzo(utente, min, max, pageable);
            }

            case RANGE_DATA -> {
                var dataDa = request.dataAcquistoDa() != null
                        ? request.dataAcquistoDa()
                        : java.time.LocalDate.of(1900, 1, 1);
                var dataA = request.dataAcquistoA() != null
                        ? request.dataAcquistoA()
                        : java.time.LocalDate.of(2999, 12, 31);
                yield inventarioRepository.findByUtenteAndRangeData(utente, dataDa, dataA, pageable);
            }

            // Filtri combinati (2 filtri) — query dedicate per le combinazioni più comuni
            case BRAND_E_STATO -> inventarioRepository.findByUtenteAndBrandAndStato(
                    utente, request.brandProdotto(), request.statoAcquisto(), pageable);

            case BRAND_E_TIPO -> inventarioRepository.findByUtenteAndBrandAndTipo(
                    utente, request.brandProdotto(), request.tipoProdotto(), pageable);

            // Caso MULTIPLO — 3 o più filtri attivi, o combinazioni non coperte sopra.
            // Usa la query universale che accetta TUTTI i parametri come opzionali.
            // Tecnica (:param IS NULL OR colonna = :param): se il parametro è null,
            // la condizione è sempre vera e non filtra. Altrimenti filtra normalmente.
            case MULTIPLO -> inventarioRepository.findByUtenteWithFiltriOpzionali(
                    utente,
                    request.statoAcquisto(),
                    request.brandProdotto(),
                    request.tipoProdotto(),
                    request.prezzoAcquistoDa(),
                    request.prezzoAcquistoA(),
                    request.dataAcquistoDa(),
                    request.dataAcquistoA(),
                    pageable);
        };

        log.info("InventarioInquiry V2 completato: {} risultati trovati (pagina {}/{})",
                risultati.getTotalElements(), page, risultati.getTotalPages());

        // Passo 6: mappo le entity a response DTO usando il factory method
        return risultati.map(InventarioInquiryResponse::fromEntity);
    }

    /**
     * Determina quale tipo di filtro è attivo analizzando i campi della request.
     *
     * Logica:
     * 1. Conta quante "dimensioni" di filtro sono attive (prezzo, data, stato, brand, tipo)
     * 2. Se 0 → NESSUNO
     * 3. Se 1 → identifica quale singolo filtro e ritorna il caso corrispondente
     * 4. Se 2 → verifica se la combinazione ha una query dedicata (BRAND_E_STATO, BRAND_E_TIPO)
     *          altrimenti → MULTIPLO
     * 5. Se 3+ → MULTIPLO (usa la query universale)
     */
    private InquiryFilterType determinaFiltro(InventarioInquiryRequest request) {

        // Determino quali dimensioni di filtro sono attive.
        // "Dimensione" = un gruppo logico di filtri (es. prezzoAcquistoDa/A sono la stessa dimensione "prezzo")
        boolean haPrezzo = request.prezzoAcquistoDa() != null || request.prezzoAcquistoA() != null;
        boolean haData = request.dataAcquistoDa() != null || request.dataAcquistoA() != null;
        boolean haStato = request.statoAcquisto() != null;
        boolean haBrand = request.brandProdotto() != null;
        boolean haTipo = request.tipoProdotto() != null;

        // Conto il numero totale di dimensioni attive
        int contatore = 0;
        if (haPrezzo) contatore++;
        if (haData) contatore++;
        if (haStato) contatore++;
        if (haBrand) contatore++;
        if (haTipo) contatore++;

        // Switch sul numero di dimensioni attive
        return switch (contatore) {

            // Nessun filtro → ritorna tutti gli item dell'utente
            case 0 -> InquiryFilterType.NESSUNO;

            // Un solo filtro → identifico quale
            case 1 -> {
                if (haStato) yield InquiryFilterType.SOLO_STATO;
                if (haBrand) yield InquiryFilterType.SOLO_BRAND;
                if (haTipo) yield InquiryFilterType.SOLO_TIPO;
                if (haPrezzo) yield InquiryFilterType.RANGE_PREZZO;
                // se arriviamo qui, l'unico rimasto è haData
                yield InquiryFilterType.RANGE_DATA;
            }

            // Due filtri → verifico se esiste una query dedicata per la combinazione
            case 2 -> {
                // Brand + Stato ha una query dedicata
                if (haBrand && haStato) yield InquiryFilterType.BRAND_E_STATO;
                // Brand + Tipo ha una query dedicata
                if (haBrand && haTipo) yield InquiryFilterType.BRAND_E_TIPO;
                // Altre combinazioni di 2 filtri → fallback alla query universale
                yield InquiryFilterType.MULTIPLO;
            }

            // 3 o più filtri → sempre query universale
            default -> InquiryFilterType.MULTIPLO;
        };
    }
}
