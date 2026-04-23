package com.tsm_db_sql.db.wiam.repository;

import com.tsm_db_sql.db.wiam.entity.UtenteSecurety;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UtenteSecRepository extends JpaRepository<UtenteSecurety, Long> {

    Optional<UtenteSecurety> findByEmail(String email);
    Optional<UtenteSecurety> findByUsername(String username);
}
