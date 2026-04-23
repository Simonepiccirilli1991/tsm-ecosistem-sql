package com.tsm_db_sql.db.wiam.entity;


import com.tsm_db_sql.db.wiam.utils.BrandAcquisti;
import com.tsm_db_sql.db.wiam.utils.StatoAcq;
import com.tsm_db_sql.db.wiam.utils.StatoProdotto;
import com.tsm_db_sql.db.wiam.utils.TipoProdotto;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity(name = "utente_inventario")
public class UtenteInventario {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String nomeAcquisto;
    private String codiceAcquisto;
    private String dataAcquisto;
    private String descrizione;
    private BrandAcquisti brandProdotto;
    private TipoProdotto tipoProdotto;
    private StatoProdotto statoProdotto;
    private StatoAcq statoAcquisto;
    // sezione vendita
    private Double prezzoVendita;
    private String piattaformaVendita;
    private String dataVendita;
    private Double costiAccessori;
    private Double prezzoNetto;

}
