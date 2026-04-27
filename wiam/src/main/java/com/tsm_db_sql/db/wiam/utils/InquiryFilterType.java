package com.tsm_db_sql.db.wiam.utils;

/**
 * Enum che rappresenta i possibili scenari di filtro per l'inquiry V2.
 *
 * Ogni valore identifica una combinazione specifica di filtri attivi nella request.
 * Il service V2 usa questo enum con uno switch-case per instradare la request
 * verso la query JPQL corretta nel repository.
 *
 * Scenari coperti:
 * - NESSUNO:         nessun filtro, ritorna tutti gli item dell'utente
 * - SOLO_STATO:      filtra solo per stato acquisto (Acquistato/Venduto/Cancellato)
 * - SOLO_BRAND:      filtra solo per brand prodotto (Pokemon/OnePiece/Lego/Generico)
 * - SOLO_TIPO:       filtra solo per tipo prodotto (BoxSealed/CartaSingola/ecc.)
 * - RANGE_PREZZO:    filtra per range prezzo acquisto (da/a)
 * - RANGE_DATA:      filtra per range data acquisto (da/a)
 * - BRAND_E_STATO:   combinazione brand + stato
 * - BRAND_E_TIPO:    combinazione brand + tipo prodotto
 * - MULTIPLO:        3 o più filtri attivi, o combinazioni non coperte dai casi sopra.
 *                    Usa una query universale con parametri opzionali.
 */
public enum InquiryFilterType {

    NESSUNO,
    SOLO_STATO,
    SOLO_BRAND,
    SOLO_TIPO,
    RANGE_PREZZO,
    RANGE_DATA,
    BRAND_E_STATO,
    BRAND_E_TIPO,
    MULTIPLO
}
