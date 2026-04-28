package com.tsm_db_sql.db.wiam.service;

import com.tsm_db_sql.db.wiam.entity.Utente;
import com.tsm_db_sql.db.wiam.entity.UtenteInventario;
import com.tsm_db_sql.db.wiam.exception.InventarioException;
import com.tsm_db_sql.db.wiam.model.request.AggiungiAcquistoRequest;
import com.tsm_db_sql.db.wiam.model.request.AggiungiVenditaRequest;
import com.tsm_db_sql.db.wiam.model.request.CancellaAcquistoRequest;
import com.tsm_db_sql.db.wiam.model.request.CancellaVenditaRequest;
import com.tsm_db_sql.db.wiam.model.request.InventarioInquiryRequest;
import com.tsm_db_sql.db.wiam.repository.InventarioRepository;
import com.tsm_db_sql.db.wiam.repository.UtenteRepository;
import com.tsm_db_sql.db.wiam.service.inventario.AggiungiAcquistoService;
import com.tsm_db_sql.db.wiam.service.inventario.AggiungiVenditaService;
import com.tsm_db_sql.db.wiam.service.inventario.CancellaAcquistoService;
import com.tsm_db_sql.db.wiam.service.inventario.CancellaVenditaService;
import com.tsm_db_sql.db.wiam.service.inventario.InventarioInquiryService;
import com.tsm_db_sql.db.wiam.service.inventario.InventarioInquiryV2Service;
import com.tsm_db_sql.db.wiam.utils.BrandAcquisti;
import com.tsm_db_sql.db.wiam.utils.StatoAcq;
import com.tsm_db_sql.db.wiam.utils.StatoProdotto;
import com.tsm_db_sql.db.wiam.utils.TipoProdotto;
import com.tsm_db_sql.db.wiam.utils.UtenteRoles;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@SpringBootTest
@ActiveProfiles("test")
public class InventarioServiceTest {

    @Autowired
    AggiungiAcquistoService aggiungiAcquistoService;
    @Autowired
    AggiungiVenditaService aggiungiVenditaService;
    @Autowired
    CancellaVenditaService cancellaVenditaService;
    @Autowired
    CancellaAcquistoService cancellaAcquistoService;
    @Autowired
    InventarioInquiryService inventarioInquiryService;
    @Autowired
    InventarioInquiryV2Service inventarioInquiryV2Service;
    @Autowired
    UtenteRepository utenteRepository;
    @Autowired
    InventarioRepository inventarioRepository;
    @Autowired
    PasswordEncoder passwordEncoder;


    @BeforeEach
    void cleanUp() {
        inventarioRepository.deleteAll();
        utenteRepository.deleteAll();
    }

    /**
     * Helper — crea e salva un utente di test nel DB.
     */
    private Utente creaUtenteDiTest() {
        var utente = new Utente();
        utente.setUsername("testuser");
        utente.setNome("Test");
        utente.setCognome("User");
        utente.setEmail("test@test.com");
        utente.setPassword(passwordEncoder.encode("password123"));
        utente.setDataRegistrazione(LocalDateTime.now());
        utente.setRuolo(UtenteRoles.User);
        return utenteRepository.save(utente);
    }

    // ==================== AggiungiAcquistoService ====================

    @Test
    void aggiungiAcquistoOK() {
        creaUtenteDiTest();

        var request = new AggiungiAcquistoRequest(
                "testuser", "Booster Box Pokemon", "BB-001",
                "2024-06-15", "Box sigillato Scarlet & Violet",
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, StatoProdotto.Sealed, new BigDecimal("89.90"));

        var response = aggiungiAcquistoService.aggiungiAcquisto(request);

        Assertions.assertEquals("Acquisto aggiunto con successo all'inventario", response.messaggio());

        var items = inventarioRepository.findByUtente(utenteRepository.findByUsername("testuser").get());
        Assertions.assertEquals(1, items.size());

        var item = items.getFirst();
        Assertions.assertEquals("Booster Box Pokemon", item.getNomeAcquisto());
        Assertions.assertEquals("BB-001", item.getCodiceAcquisto());
        Assertions.assertEquals(LocalDate.of(2024, 6, 15), item.getDataAcquisto());
        Assertions.assertEquals(BrandAcquisti.Pokemon, item.getBrandProdotto());
        Assertions.assertEquals(TipoProdotto.BoxSealed, item.getTipoProdotto());
        Assertions.assertEquals(StatoProdotto.Sealed, item.getStatoProdotto());
        Assertions.assertEquals(StatoAcq.Acquistato, item.getStatoAcquisto());
        Assertions.assertEquals(new BigDecimal("89.90"), item.getPrezzoAcquisto());
    }

    @Test
    void aggiungiAcquistoKO_utenteNonTrovato() {
        var request = new AggiungiAcquistoRequest(
                "utente_inesistente", "Box", null,
                "2024-06-15", null,
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, StatoProdotto.Sealed, new BigDecimal("50.00"));

        var ex = Assertions.assertThrows(InventarioException.class,
                () -> aggiungiAcquistoService.aggiungiAcquisto(request));

        Assertions.assertEquals("ERR-INV-404", ex.getErrorCode());
    }

    @Test
    void aggiungiAcquistoKO_campiMancanti() {
        var request = new AggiungiAcquistoRequest(
                "testuser", null, null,
                null, null,
                null, null, null, null);

        var ex = Assertions.assertThrows(InventarioException.class,
                () -> aggiungiAcquistoService.aggiungiAcquisto(request));

        Assertions.assertEquals("ERR-INV-400", ex.getErrorCode());
    }

    @Test
    void aggiungiAcquistoKO_senzaPrezzoAcquisto() {
        creaUtenteDiTest();

        // prezzoAcquisto è ora obbligatorio — verifica che fallisca senza
        var request = new AggiungiAcquistoRequest(
                "testuser", "Box Lego", null,
                "2024-06-15", null,
                BrandAcquisti.Lego, TipoProdotto.BoxSealed, StatoProdotto.Sealed, null);

        var ex = Assertions.assertThrows(InventarioException.class,
                () -> aggiungiAcquistoService.aggiungiAcquisto(request));
        Assertions.assertEquals("ERR-INV-400", ex.getErrorCode());
    }

    // ==================== AggiungiVenditaService ====================

    @Test
    void aggiungiVenditaOK() {
        var utente = creaUtenteDiTest();
        var item = creaItemDiTest(utente);

        var request = new AggiungiVenditaRequest(
                "testuser", item.getId(),
                "150.00", "eBay", "2024-07-20", "10.50");

        var response = aggiungiVenditaService.aggiungiVendita(request);

        Assertions.assertEquals("Vendita registrata con successo", response.messaggio());

        var itemAggiornato = inventarioRepository.findById(item.getId()).get();
        Assertions.assertEquals(StatoAcq.Venduto, itemAggiornato.getStatoAcquisto());
        Assertions.assertEquals(new BigDecimal("150.00"), itemAggiornato.getPrezzoVendita());
        Assertions.assertEquals("eBay", itemAggiornato.getPiattaformaVendita());
        Assertions.assertEquals(new BigDecimal("10.50"), itemAggiornato.getCostiAccessori());
        // prezzoNetto = 150.00 - 10.50 = 139.50
        Assertions.assertEquals(new BigDecimal("139.50"), itemAggiornato.getPrezzoNetto());
    }

    @Test
    void aggiungiVenditaOK_senzaCostiAccessori() {
        var utente = creaUtenteDiTest();
        var item = creaItemDiTest(utente);

        var request = new AggiungiVenditaRequest(
                "testuser", item.getId(),
                "100.00", "Vinted", "2024-07-20", null);

        var response = aggiungiVenditaService.aggiungiVendita(request);

        Assertions.assertEquals("Vendita registrata con successo", response.messaggio());

        var itemAggiornato = inventarioRepository.findById(item.getId()).get();
        Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(itemAggiornato.getCostiAccessori()));
        Assertions.assertEquals(new BigDecimal("100.00"), itemAggiornato.getPrezzoNetto());
    }

    @Test
    void aggiungiVenditaKO_itemNonAppartiene() {
        var utente1 = creaUtenteDiTest();
        var item = creaItemDiTest(utente1);

        // creo un secondo utente
        var utente2 = new Utente();
        utente2.setUsername("altroutente");
        utente2.setNome("Altro");
        utente2.setCognome("Utente");
        utente2.setEmail("altro@test.com");
        utente2.setPassword(passwordEncoder.encode("pass"));
        utente2.setDataRegistrazione(LocalDateTime.now());
        utente2.setRuolo(UtenteRoles.User);
        utenteRepository.save(utente2);

        // provo a vendere l'item di utente1 usando username di utente2
        var request = new AggiungiVenditaRequest(
                "altroutente", item.getId(),
                "100.00", "eBay", "2024-07-20", null);

        var ex = Assertions.assertThrows(InventarioException.class,
                () -> aggiungiVenditaService.aggiungiVendita(request));

        Assertions.assertEquals("ERR-INV-403", ex.getErrorCode());
    }

    @Test
    void aggiungiVenditaKO_itemGiaVenduto() {
        var utente = creaUtenteDiTest();
        var item = creaItemDiTest(utente);

        // prima vendita OK
        aggiungiVenditaService.aggiungiVendita(new AggiungiVenditaRequest(
                "testuser", item.getId(), "100.00", "eBay", "2024-07-20", null));

        // seconda vendita KO — item già venduto
        var request = new AggiungiVenditaRequest(
                "testuser", item.getId(), "200.00", "Vinted", "2024-08-01", null);

        var ex = Assertions.assertThrows(InventarioException.class,
                () -> aggiungiVenditaService.aggiungiVendita(request));

        Assertions.assertEquals("ERR-INV-400", ex.getErrorCode());
    }

    // ==================== CancellaVenditaService ====================

    @Test
    void cancellaVenditaOK() {
        var utente = creaUtenteDiTest();
        var item = creaItemDiTest(utente);

        // prima vendo l'item
        aggiungiVenditaService.aggiungiVendita(new AggiungiVenditaRequest(
                "testuser", item.getId(), "150.00", "eBay", "2024-07-20", "10.00"));

        // poi annullo la vendita
        var response = cancellaVenditaService.cancellaVendita(
                new CancellaVenditaRequest("testuser", item.getId()));

        Assertions.assertEquals("Vendita annullata con successo, item riportato ad Acquistato", response.messaggio());

        var itemAggiornato = inventarioRepository.findById(item.getId()).get();
        Assertions.assertEquals(StatoAcq.Acquistato, itemAggiornato.getStatoAcquisto());
        // tutti i campi vendita devono essere null
        Assertions.assertNull(itemAggiornato.getPrezzoVendita());
        Assertions.assertNull(itemAggiornato.getPiattaformaVendita());
        Assertions.assertNull(itemAggiornato.getDataVendita());
        Assertions.assertNull(itemAggiornato.getCostiAccessori());
        Assertions.assertNull(itemAggiornato.getPrezzoNetto());
    }

    @Test
    void cancellaVenditaKO_itemNonVenduto() {
        var utente = creaUtenteDiTest();
        var item = creaItemDiTest(utente);

        // provo ad annullare vendita di un item non venduto
        var ex = Assertions.assertThrows(InventarioException.class,
                () -> cancellaVenditaService.cancellaVendita(
                        new CancellaVenditaRequest("testuser", item.getId())));

        Assertions.assertEquals("ERR-INV-400", ex.getErrorCode());
    }

    // ==================== CancellaAcquistoService ====================

    @Test
    void cancellaAcquistoOK() {
        var utente = creaUtenteDiTest();
        var item = creaItemDiTest(utente);

        var response = cancellaAcquistoService.cancellaAcquisto(
                new CancellaAcquistoRequest("testuser", item.getId()));

        Assertions.assertEquals("Acquisto cancellato con successo", response.messaggio());

        var itemAggiornato = inventarioRepository.findById(item.getId()).get();
        Assertions.assertEquals(StatoAcq.Cancellato, itemAggiornato.getStatoAcquisto());
    }

    @Test
    void cancellaAcquistoKO_itemVenduto() {
        var utente = creaUtenteDiTest();
        var item = creaItemDiTest(utente);

        // vendo l'item
        aggiungiVenditaService.aggiungiVendita(new AggiungiVenditaRequest(
                "testuser", item.getId(), "100.00", "eBay", "2024-07-20", null));

        // provo a cancellare un item venduto — deve fallire
        var ex = Assertions.assertThrows(InventarioException.class,
                () -> cancellaAcquistoService.cancellaAcquisto(
                        new CancellaAcquistoRequest("testuser", item.getId())));

        Assertions.assertEquals("ERR-INV-400", ex.getErrorCode());
    }

    @Test
    void cancellaAcquistoKO_itemGiaCancellato() {
        var utente = creaUtenteDiTest();
        var item = creaItemDiTest(utente);

        // prima cancellazione OK
        cancellaAcquistoService.cancellaAcquisto(
                new CancellaAcquistoRequest("testuser", item.getId()));

        // seconda cancellazione KO — già cancellato
        var ex = Assertions.assertThrows(InventarioException.class,
                () -> cancellaAcquistoService.cancellaAcquisto(
                        new CancellaAcquistoRequest("testuser", item.getId())));

        Assertions.assertEquals("ERR-INV-400", ex.getErrorCode());
    }

    // ==================== Flusso completo ====================

    @Test
    void flussoCompleto_acquisto_vendita_annulloVendita_cancellazione() {
        var utente = creaUtenteDiTest();

        // 1. Aggiungo acquisto
        var acquistoResp = aggiungiAcquistoService.aggiungiAcquisto(new AggiungiAcquistoRequest(
                "testuser", "Display Pokemon", "DP-001",
                "2024-06-01", "Display 36 buste",
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, StatoProdotto.Sealed, new BigDecimal("120.00")));
        Assertions.assertEquals("Acquisto aggiunto con successo all'inventario", acquistoResp.messaggio());

        var items = inventarioRepository.findByUtente(utente);
        var itemId = items.getFirst().getId();

        // 2. Registro vendita
        var venditaResp = aggiungiVenditaService.aggiungiVendita(new AggiungiVenditaRequest(
                "testuser", itemId, "200.00", "eBay", "2024-07-15", "15.00"));
        Assertions.assertEquals("Vendita registrata con successo", venditaResp.messaggio());
        Assertions.assertEquals(StatoAcq.Venduto, inventarioRepository.findById(itemId).get().getStatoAcquisto());

        // 3. Annullo vendita
        var annulloResp = cancellaVenditaService.cancellaVendita(
                new CancellaVenditaRequest("testuser", itemId));
        Assertions.assertEquals("Vendita annullata con successo, item riportato ad Acquistato", annulloResp.messaggio());
        Assertions.assertEquals(StatoAcq.Acquistato, inventarioRepository.findById(itemId).get().getStatoAcquisto());

        // 4. Cancello acquisto
        var cancellaResp = cancellaAcquistoService.cancellaAcquisto(
                new CancellaAcquistoRequest("testuser", itemId));
        Assertions.assertEquals("Acquisto cancellato con successo", cancellaResp.messaggio());
        Assertions.assertEquals(StatoAcq.Cancellato, inventarioRepository.findById(itemId).get().getStatoAcquisto());
    }

    // ==================== Helper ====================

    /**
     * Helper — crea e salva un item di inventario di test per un utente.
     */
    private UtenteInventario creaItemDiTest(Utente utente) {
        var item = new UtenteInventario();
        item.setNomeAcquisto("Test Item");
        item.setCodiceAcquisto("TEST-001");
        item.setDataAcquisto(LocalDate.of(2024, 6, 1));
        item.setDescrizione("Item di test");
        item.setBrandProdotto(BrandAcquisti.Pokemon);
        item.setTipoProdotto(TipoProdotto.BoxSealed);
        item.setStatoProdotto(StatoProdotto.Sealed);
        item.setStatoAcquisto(StatoAcq.Acquistato);
        item.setPrezzoAcquisto(new BigDecimal("50.00"));
        utente.aggiungiInventarioItem(item);
        return inventarioRepository.save(item);
    }

    /**
     * Helper — crea un item con parametri personalizzati per i test inquiry.
     */
    private UtenteInventario creaItemPersonalizzato(Utente utente, String nome, LocalDate data,
            BrandAcquisti brand, TipoProdotto tipo, BigDecimal prezzo) {
        var item = new UtenteInventario();
        item.setNomeAcquisto(nome);
        item.setDataAcquisto(data);
        item.setBrandProdotto(brand);
        item.setTipoProdotto(tipo);
        item.setStatoProdotto(StatoProdotto.Sealed);
        item.setStatoAcquisto(StatoAcq.Acquistato);
        item.setPrezzoAcquisto(prezzo);
        utente.aggiungiInventarioItem(item);
        return inventarioRepository.save(item);
    }

    // ==================== InventarioInquiryService ====================

    @Test
    void inquirySenzaFiltri_restituisceTuttiGliItem() {
        var utente = creaUtenteDiTest();
        creaItemPersonalizzato(utente, "Item 1", LocalDate.of(2024, 1, 15),
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal("50.00"));
        creaItemPersonalizzato(utente, "Item 2", LocalDate.of(2024, 3, 20),
                BrandAcquisti.Lego, TipoProdotto.BoxSealed, new BigDecimal("100.00"));
        creaItemPersonalizzato(utente, "Item 3", LocalDate.of(2024, 6, 10),
                BrandAcquisti.OnePiece, TipoProdotto.CartaSingola, new BigDecimal("25.00"));

        // inquiry senza filtri — solo username
        var request = new InventarioInquiryRequest(
                "testuser", null, null, null, null, null, null, null, null, null);

        var risultati = inventarioInquiryService.inquiry(request);

        Assertions.assertEquals(3, risultati.getTotalElements());
        // ordinati per data acquisto decrescente
        Assertions.assertEquals("Item 3", risultati.getContent().getFirst().nomeAcquisto());
    }

    @Test
    void inquiryFiltroBrand() {
        var utente = creaUtenteDiTest();
        creaItemPersonalizzato(utente, "Pokemon Box", LocalDate.of(2024, 1, 15),
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal("80.00"));
        creaItemPersonalizzato(utente, "Lego Set", LocalDate.of(2024, 3, 20),
                BrandAcquisti.Lego, TipoProdotto.BoxSealed, new BigDecimal("120.00"));
        creaItemPersonalizzato(utente, "Pokemon Card", LocalDate.of(2024, 5, 10),
                BrandAcquisti.Pokemon, TipoProdotto.CartaSingola, new BigDecimal("15.00"));

        // filtro solo Pokemon
        var request = new InventarioInquiryRequest(
                "testuser", null, null, null, null, null, BrandAcquisti.Pokemon, null, null, null);

        var risultati = inventarioInquiryService.inquiry(request);

        Assertions.assertEquals(2, risultati.getTotalElements());
        risultati.getContent().forEach(item ->
                Assertions.assertEquals(BrandAcquisti.Pokemon, item.brandProdotto()));
    }

    @Test
    void inquiryFiltroTipoProdotto() {
        var utente = creaUtenteDiTest();
        creaItemPersonalizzato(utente, "Box 1", LocalDate.of(2024, 1, 15),
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal("80.00"));
        creaItemPersonalizzato(utente, "Carta 1", LocalDate.of(2024, 3, 20),
                BrandAcquisti.Pokemon, TipoProdotto.CartaSingola, new BigDecimal("10.00"));

        var request = new InventarioInquiryRequest(
                "testuser", null, null, null, null, null, null, TipoProdotto.CartaSingola, null, null);

        var risultati = inventarioInquiryService.inquiry(request);

        Assertions.assertEquals(1, risultati.getTotalElements());
        Assertions.assertEquals("Carta 1", risultati.getContent().getFirst().nomeAcquisto());
    }

    @Test
    void inquiryFiltroStatoAcquisto() {
        var utente = creaUtenteDiTest();
        var item1 = creaItemPersonalizzato(utente, "Item Acquistato", LocalDate.of(2024, 1, 15),
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal("80.00"));
        var item2 = creaItemPersonalizzato(utente, "Item Venduto", LocalDate.of(2024, 3, 20),
                BrandAcquisti.Lego, TipoProdotto.BoxSealed, new BigDecimal("100.00"));

        // vendo il secondo item
        aggiungiVenditaService.aggiungiVendita(new AggiungiVenditaRequest(
                "testuser", item2.getId(), "150.00", "eBay", "2024-07-20", null));

        // filtro solo Venduto
        var request = new InventarioInquiryRequest(
                "testuser", null, null, null, null, StatoAcq.Venduto, null, null, null, null);

        var risultati = inventarioInquiryService.inquiry(request);

        Assertions.assertEquals(1, risultati.getTotalElements());
        Assertions.assertEquals("Item Venduto", risultati.getContent().getFirst().nomeAcquisto());
    }

    @Test
    void inquiryFiltroRangePrezzo() {
        var utente = creaUtenteDiTest();
        creaItemPersonalizzato(utente, "Economico", LocalDate.of(2024, 1, 15),
                BrandAcquisti.Pokemon, TipoProdotto.CartaSingola, new BigDecimal("10.00"));
        creaItemPersonalizzato(utente, "Medio", LocalDate.of(2024, 3, 20),
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal("80.00"));
        creaItemPersonalizzato(utente, "Costoso", LocalDate.of(2024, 5, 10),
                BrandAcquisti.Lego, TipoProdotto.BoxSealed, new BigDecimal("200.00"));

        // filtro prezzo tra 50 e 150
        var request = new InventarioInquiryRequest(
                "testuser", new BigDecimal("50.00"), new BigDecimal("150.00"), null, null, null, null, null, null, null);

        var risultati = inventarioInquiryService.inquiry(request);

        Assertions.assertEquals(1, risultati.getTotalElements());
        Assertions.assertEquals("Medio", risultati.getContent().getFirst().nomeAcquisto());
    }

    @Test
    void inquiryFiltroRangeDate() {
        var utente = creaUtenteDiTest();
        creaItemPersonalizzato(utente, "Gennaio", LocalDate.of(2024, 1, 15),
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal("50.00"));
        creaItemPersonalizzato(utente, "Marzo", LocalDate.of(2024, 3, 20),
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal("60.00"));
        creaItemPersonalizzato(utente, "Giugno", LocalDate.of(2024, 6, 10),
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal("70.00"));

        // filtro da febbraio ad aprile
        var request = new InventarioInquiryRequest(
                "testuser", null, null, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 4, 30), null, null, null, null, null);

        var risultati = inventarioInquiryService.inquiry(request);

        Assertions.assertEquals(1, risultati.getTotalElements());
        Assertions.assertEquals("Marzo", risultati.getContent().getFirst().nomeAcquisto());
    }

    @Test
    void inquiryFiltriCombinati() {
        var utente = creaUtenteDiTest();
        creaItemPersonalizzato(utente, "Pokemon Box Cheap", LocalDate.of(2024, 3, 15),
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal("30.00"));
        creaItemPersonalizzato(utente, "Pokemon Box Expensive", LocalDate.of(2024, 3, 20),
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal("150.00"));
        creaItemPersonalizzato(utente, "Lego Box", LocalDate.of(2024, 3, 25),
                BrandAcquisti.Lego, TipoProdotto.BoxSealed, new BigDecimal("100.00"));
        creaItemPersonalizzato(utente, "Pokemon Card", LocalDate.of(2024, 3, 28),
                BrandAcquisti.Pokemon, TipoProdotto.CartaSingola, new BigDecimal("5.00"));

        // filtro: brand=Pokemon AND tipo=BoxSealed AND prezzo tra 20 e 100
        var request = new InventarioInquiryRequest(
                "testuser", new BigDecimal("20.00"), new BigDecimal("100.00"), null, null, null,
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, null, null);

        var risultati = inventarioInquiryService.inquiry(request);

        Assertions.assertEquals(1, risultati.getTotalElements());
        Assertions.assertEquals("Pokemon Box Cheap", risultati.getContent().getFirst().nomeAcquisto());
    }

    @Test
    void inquiryPaginazione() {
        var utente = creaUtenteDiTest();
        // creo 5 item
        for (int i = 1; i <= 5; i++) {
            creaItemPersonalizzato(utente, "Item " + i, LocalDate.of(2024, i, 1),
                    BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal(i * 10));
        }

        // pagina 0, 2 elementi per pagina
        var request = new InventarioInquiryRequest(
                "testuser", null, null, null, null, null, null, null, 0, 2);

        var risultati = inventarioInquiryService.inquiry(request);

        Assertions.assertEquals(5, risultati.getTotalElements());
        Assertions.assertEquals(3, risultati.getTotalPages());
        Assertions.assertEquals(2, risultati.getContent().size());
        // ordinati per data decrescente, pagina 0 → Item 5 e Item 4
        Assertions.assertEquals("Item 5", risultati.getContent().get(0).nomeAcquisto());
        Assertions.assertEquals("Item 4", risultati.getContent().get(1).nomeAcquisto());
    }

    @Test
    void inquiryKO_usernameMancante() {
        var request = new InventarioInquiryRequest(
                null, null, null, null, null, null, null, null, null, null);

        var ex = Assertions.assertThrows(InventarioException.class,
                () -> inventarioInquiryService.inquiry(request));

        Assertions.assertEquals("ERR-INV-400", ex.getErrorCode());
    }

    @Test
    void inquiryKO_utenteNonTrovato() {
        var request = new InventarioInquiryRequest(
                "utente_inesistente", null, null, null, null, null, null, null, null, null);

        var ex = Assertions.assertThrows(InventarioException.class,
                () -> inventarioInquiryService.inquiry(request));

        Assertions.assertEquals("ERR-INV-404", ex.getErrorCode());
    }

    // ==================== InventarioInquiryV2Service (switch-case + custom query) ====================

    @Test
    void v2_inquirySenzaFiltri() {
        var utente = creaUtenteDiTest();
        creaItemPersonalizzato(utente, "Item 1", LocalDate.of(2024, 1, 15),
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal("50.00"));
        creaItemPersonalizzato(utente, "Item 2", LocalDate.of(2024, 3, 20),
                BrandAcquisti.Lego, TipoProdotto.BoxSealed, new BigDecimal("100.00"));

        var request = new InventarioInquiryRequest(
                "testuser", null, null, null, null, null, null, null, null, null);

        var risultati = inventarioInquiryV2Service.inquiry(request);

        Assertions.assertEquals(2, risultati.getTotalElements());
    }

    @Test
    void v2_inquiryFiltroBrand() {
        var utente = creaUtenteDiTest();
        creaItemPersonalizzato(utente, "Pokemon Box", LocalDate.of(2024, 1, 15),
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal("80.00"));
        creaItemPersonalizzato(utente, "Lego Set", LocalDate.of(2024, 3, 20),
                BrandAcquisti.Lego, TipoProdotto.BoxSealed, new BigDecimal("120.00"));

        var request = new InventarioInquiryRequest(
                "testuser", null, null, null, null, null, BrandAcquisti.Pokemon, null, null, null);

        var risultati = inventarioInquiryV2Service.inquiry(request);

        Assertions.assertEquals(1, risultati.getTotalElements());
        Assertions.assertEquals("Pokemon Box", risultati.getContent().getFirst().nomeAcquisto());
    }

    @Test
    void v2_inquiryFiltroStato() {
        var utente = creaUtenteDiTest();
        var item1 = creaItemPersonalizzato(utente, "Item Acquistato", LocalDate.of(2024, 1, 15),
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal("80.00"));
        var item2 = creaItemPersonalizzato(utente, "Item Venduto", LocalDate.of(2024, 3, 20),
                BrandAcquisti.Lego, TipoProdotto.BoxSealed, new BigDecimal("100.00"));

        aggiungiVenditaService.aggiungiVendita(new AggiungiVenditaRequest(
                "testuser", item2.getId(), "150.00", "eBay", "2024-07-20", null));

        var request = new InventarioInquiryRequest(
                "testuser", null, null, null, null, StatoAcq.Venduto, null, null, null, null);

        var risultati = inventarioInquiryV2Service.inquiry(request);

        Assertions.assertEquals(1, risultati.getTotalElements());
        Assertions.assertEquals("Item Venduto", risultati.getContent().getFirst().nomeAcquisto());
    }

    @Test
    void v2_inquiryFiltroTipo() {
        var utente = creaUtenteDiTest();
        creaItemPersonalizzato(utente, "Box", LocalDate.of(2024, 1, 15),
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal("80.00"));
        creaItemPersonalizzato(utente, "Carta", LocalDate.of(2024, 3, 20),
                BrandAcquisti.Pokemon, TipoProdotto.CartaSingola, new BigDecimal("10.00"));

        var request = new InventarioInquiryRequest(
                "testuser", null, null, null, null, null, null, TipoProdotto.CartaSingola, null, null);

        var risultati = inventarioInquiryV2Service.inquiry(request);

        Assertions.assertEquals(1, risultati.getTotalElements());
        Assertions.assertEquals("Carta", risultati.getContent().getFirst().nomeAcquisto());
    }

    @Test
    void v2_inquiryRangePrezzo() {
        var utente = creaUtenteDiTest();
        creaItemPersonalizzato(utente, "Economico", LocalDate.of(2024, 1, 15),
                BrandAcquisti.Pokemon, TipoProdotto.CartaSingola, new BigDecimal("10.00"));
        creaItemPersonalizzato(utente, "Medio", LocalDate.of(2024, 3, 20),
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal("80.00"));
        creaItemPersonalizzato(utente, "Costoso", LocalDate.of(2024, 5, 10),
                BrandAcquisti.Lego, TipoProdotto.BoxSealed, new BigDecimal("200.00"));

        var request = new InventarioInquiryRequest(
                "testuser", new BigDecimal("50.00"), new BigDecimal("150.00"),
                null, null, null, null, null, null, null);

        var risultati = inventarioInquiryV2Service.inquiry(request);

        Assertions.assertEquals(1, risultati.getTotalElements());
        Assertions.assertEquals("Medio", risultati.getContent().getFirst().nomeAcquisto());
    }

    @Test
    void v2_inquiryRangeData() {
        var utente = creaUtenteDiTest();
        creaItemPersonalizzato(utente, "Gennaio", LocalDate.of(2024, 1, 15),
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal("50.00"));
        creaItemPersonalizzato(utente, "Marzo", LocalDate.of(2024, 3, 20),
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal("60.00"));
        creaItemPersonalizzato(utente, "Giugno", LocalDate.of(2024, 6, 10),
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal("70.00"));

        var request = new InventarioInquiryRequest(
                "testuser", null, null,
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 4, 30),
                null, null, null, null, null);

        var risultati = inventarioInquiryV2Service.inquiry(request);

        Assertions.assertEquals(1, risultati.getTotalElements());
        Assertions.assertEquals("Marzo", risultati.getContent().getFirst().nomeAcquisto());
    }

    @Test
    void v2_inquiryBrandEStato() {
        var utente = creaUtenteDiTest();
        var item1 = creaItemPersonalizzato(utente, "Pokemon Acquistato", LocalDate.of(2024, 1, 15),
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal("80.00"));
        var item2 = creaItemPersonalizzato(utente, "Pokemon Venduto", LocalDate.of(2024, 2, 20),
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal("90.00"));
        creaItemPersonalizzato(utente, "Lego Acquistato", LocalDate.of(2024, 3, 10),
                BrandAcquisti.Lego, TipoProdotto.BoxSealed, new BigDecimal("100.00"));

        aggiungiVenditaService.aggiungiVendita(new AggiungiVenditaRequest(
                "testuser", item2.getId(), "150.00", "eBay", "2024-07-20", null));

        // filtro: brand=Pokemon AND stato=Acquistato
        var request = new InventarioInquiryRequest(
                "testuser", null, null, null, null,
                StatoAcq.Acquistato, BrandAcquisti.Pokemon, null, null, null);

        var risultati = inventarioInquiryV2Service.inquiry(request);

        Assertions.assertEquals(1, risultati.getTotalElements());
        Assertions.assertEquals("Pokemon Acquistato", risultati.getContent().getFirst().nomeAcquisto());
    }

    @Test
    void v2_inquiryBrandETipo() {
        var utente = creaUtenteDiTest();
        creaItemPersonalizzato(utente, "Pokemon Box", LocalDate.of(2024, 1, 15),
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal("80.00"));
        creaItemPersonalizzato(utente, "Pokemon Carta", LocalDate.of(2024, 3, 20),
                BrandAcquisti.Pokemon, TipoProdotto.CartaSingola, new BigDecimal("5.00"));
        creaItemPersonalizzato(utente, "Lego Box", LocalDate.of(2024, 5, 10),
                BrandAcquisti.Lego, TipoProdotto.BoxSealed, new BigDecimal("100.00"));

        var request = new InventarioInquiryRequest(
                "testuser", null, null, null, null, null,
                BrandAcquisti.Pokemon, TipoProdotto.CartaSingola, null, null);

        var risultati = inventarioInquiryV2Service.inquiry(request);

        Assertions.assertEquals(1, risultati.getTotalElements());
        Assertions.assertEquals("Pokemon Carta", risultati.getContent().getFirst().nomeAcquisto());
    }

    @Test
    void v2_inquiryMultiplo_treFiltri() {
        var utente = creaUtenteDiTest();
        creaItemPersonalizzato(utente, "Match", LocalDate.of(2024, 3, 15),
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal("80.00"));
        creaItemPersonalizzato(utente, "No Brand", LocalDate.of(2024, 3, 20),
                BrandAcquisti.Lego, TipoProdotto.BoxSealed, new BigDecimal("80.00"));
        creaItemPersonalizzato(utente, "No Tipo", LocalDate.of(2024, 3, 25),
                BrandAcquisti.Pokemon, TipoProdotto.CartaSingola, new BigDecimal("5.00"));

        // filtro: brand=Pokemon AND tipo=BoxSealed AND prezzo tra 50 e 150 (3 filtri → MULTIPLO)
        var request = new InventarioInquiryRequest(
                "testuser", new BigDecimal("50.00"), new BigDecimal("150.00"),
                null, null, null,
                BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, null, null);

        var risultati = inventarioInquiryV2Service.inquiry(request);

        Assertions.assertEquals(1, risultati.getTotalElements());
        Assertions.assertEquals("Match", risultati.getContent().getFirst().nomeAcquisto());
    }

    @Test
    void v2_inquiryPaginazione() {
        var utente = creaUtenteDiTest();
        for (int i = 1; i <= 5; i++) {
            creaItemPersonalizzato(utente, "Item " + i, LocalDate.of(2024, i, 1),
                    BrandAcquisti.Pokemon, TipoProdotto.BoxSealed, new BigDecimal(i * 10));
        }

        var request = new InventarioInquiryRequest(
                "testuser", null, null, null, null, null, null, null, 0, 2);

        var risultati = inventarioInquiryV2Service.inquiry(request);

        Assertions.assertEquals(5, risultati.getTotalElements());
        Assertions.assertEquals(3, risultati.getTotalPages());
        Assertions.assertEquals(2, risultati.getContent().size());
    }

    @Test
    void v2_inquiryKO_usernameMancante() {
        var request = new InventarioInquiryRequest(
                null, null, null, null, null, null, null, null, null, null);

        var ex = Assertions.assertThrows(InventarioException.class,
                () -> inventarioInquiryV2Service.inquiry(request));

        Assertions.assertEquals("ERR-INV-400", ex.getErrorCode());
    }
}
