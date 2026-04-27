package com.tsm_db_sql.db.wiam.model.response;

import com.tsm_db_sql.db.wiam.entity.UtenteInventario;
import com.tsm_db_sql.db.wiam.utils.BrandAcquisti;
import com.tsm_db_sql.db.wiam.utils.StatoAcq;
import com.tsm_db_sql.db.wiam.utils.StatoProdotto;
import com.tsm_db_sql.db.wiam.utils.TipoProdotto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response che rappresenta un singolo item dell'inventario nei risultati dell'inquiry.
 *
 * Mappa i campi dell'entity UtenteInventario senza includere la relazione Utente,
 * per evitare riferimenti circolari nella serializzazione JSON e per esporre
 * solo i dati rilevanti al client.
 */
public record InventarioInquiryResponse(
        Long id,
        String nomeAcquisto,
        String codiceAcquisto,
        LocalDate dataAcquisto,
        String descrizione,
        BrandAcquisti brandProdotto,
        TipoProdotto tipoProdotto,
        StatoProdotto statoProdotto,
        StatoAcq statoAcquisto,
        BigDecimal prezzoAcquisto,
        BigDecimal prezzoVendita,
        String piattaformaVendita,
        LocalDate dataVendita,
        BigDecimal costiAccessori,
        BigDecimal prezzoNetto
) {

    /**
     * Factory method — converte un'entity UtenteInventario in un InventarioInquiryResponse.
     * Centralizza la logica di mapping in un unico punto per evitare duplicazioni.
     */
    public static InventarioInquiryResponse fromEntity(UtenteInventario item) {
        return new InventarioInquiryResponse(
                item.getId(),
                item.getNomeAcquisto(),
                item.getCodiceAcquisto(),
                item.getDataAcquisto(),
                item.getDescrizione(),
                item.getBrandProdotto(),
                item.getTipoProdotto(),
                item.getStatoProdotto(),
                item.getStatoAcquisto(),
                item.getPrezzoAcquisto(),
                item.getPrezzoVendita(),
                item.getPiattaformaVendita(),
                item.getDataVendita(),
                item.getCostiAccessori(),
                item.getPrezzoNetto()
        );
    }
}
