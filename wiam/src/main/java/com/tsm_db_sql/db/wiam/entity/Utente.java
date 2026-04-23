package com.tsm_db_sql.db.wiam.entity;


import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity(name = "utente")
public class Utente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String cognome;

    @Column(unique = true, nullable = false)
    private String email;
    @Column(unique = true, nullable = false)
    private String username;
    @Column( nullable = false)
    private String password;
    @Column( nullable = false)
    private String dataRegistrazione;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "utente_securety_username", referencedColumnName = "id")
    private UtenteSecurety utenteSecurety;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "utente_inventario_username", referencedColumnName = "id")
    private List<UtenteInventario> inventario;
}

