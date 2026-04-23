package com.tsm_db_sql.db.wiam.repository;

import com.tsm_db_sql.db.wiam.entity.UtenteInventario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventarioRepository extends JpaRepository<UtenteInventario, Long> {

    Optional<UtenteInventario> findByUsername(String username);
}
