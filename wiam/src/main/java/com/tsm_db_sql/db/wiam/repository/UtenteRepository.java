package com.tsm_db_sql.db.wiam.repository;

import com.tsm_db_sql.db.wiam.entity.Utente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UtenteRepository extends JpaRepository<Utente, Long> {


    Optional<Utente> findByUsername(String username);
    Optional<Utente> findByEmail(String email);
}
