package com.tsm_db_sql.db.wiam.service;

import com.tsm_db_sql.db.wiam.entity.Utente;
import com.tsm_db_sql.db.wiam.entity.UtenteInventario;
import com.tsm_db_sql.db.wiam.exception.InventarioException;
import com.tsm_db_sql.db.wiam.model.request.AggiungiAcquistoRequest;
import com.tsm_db_sql.db.wiam.model.request.AggiungiVenditaRequest;
import com.tsm_db_sql.db.wiam.model.request.CancellaAcquistoRequest;
import com.tsm_db_sql.db.wiam.model.request.CancellaVenditaRequest;
import com.tsm_db_sql.db.wiam.repository.InventarioRepository;
import com.tsm_db_sql.db.wiam.repository.UtenteRepository;
import com.tsm_db_sql.db.wiam.service.inventario.AggiungiAcquistoService;
import com.tsm_db_sql.db.wiam.service.inventario.AggiungiVenditaService;
import com.tsm_db_sql.db.wiam.service.inventario.CancellaAcquistoService;
import com.tsm_db_sql.db.wiam.service.inventario.CancellaVenditaService;
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
    UtenteRepository utenteRepository;
    @Autowired
    InventarioRepository inventarioRepository;


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
        utente.setPassword("password123");
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
                "Pokemon", "BoxSealed", "Sealed");

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
    }

    @Test
    void aggiungiAcquistoKO_utenteNonTrovato() {
        var request = new AggiungiAcquistoRequest(
                "utente_inesistente", "Box", null,
                "2024-06-15", null,
                "Pokemon", "BoxSealed", "Sealed");

        var ex = Assertions.assertThrows(InventarioException.class,
                () -> aggiungiAcquistoService.aggiungiAcquisto(request));

        Assertions.assertEquals("ERR-INV-404", ex.getErrorCode());
    }

    @Test
    void aggiungiAcquistoKO_campiMancanti() {
        var request = new AggiungiAcquistoRequest(
                "testuser", null, null,
                null, null,
                null, null, null);

        var ex = Assertions.assertThrows(InventarioException.class,
                () -> aggiungiAcquistoService.aggiungiAcquisto(request));

        Assertions.assertEquals("ERR-INV-400", ex.getErrorCode());
    }

    @Test
    void aggiungiAcquistoKO_enumNonValido() {
        creaUtenteDiTest();

        var request = new AggiungiAcquistoRequest(
                "testuser", "Box", null,
                "2024-06-15", null,
                "BrandInesistente", "BoxSealed", "Sealed");

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
        utente2.setPassword("pass");
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
                "Pokemon", "BoxSealed", "Sealed"));
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
        utente.aggiungiInventarioItem(item);
        return inventarioRepository.save(item);
    }
}
