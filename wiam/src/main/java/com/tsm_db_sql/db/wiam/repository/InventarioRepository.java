package com.tsm_db_sql.db.wiam.repository;

import com.tsm_db_sql.db.wiam.entity.Utente;
import com.tsm_db_sql.db.wiam.entity.UtenteInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository per UtenteInventario.
 * Il campo "username" è stato rimosso dall'entity (ridondante con la FK).
 * Per ottenere l'inventario di un utente, usare la relazione:
 *   utente.getInventario()
 * oppure questo metodo che cerca tramite l'oggetto Utente (FK).
 */
public interface InventarioRepository extends JpaRepository<UtenteInventario, Long> {

    List<UtenteInventario> findByUtente(Utente utente);

    Page<UtenteInventario> findByUtente(Utente utente, Pageable pageable);
}
