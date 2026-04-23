package com.tsm_db_sql.db.wiam.repository;

import com.tsm_db_sql.db.wiam.entity.UtenteSecurety;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository per UtenteSecurety.
 * Dopo aver reso la relazione bidirezionale, il campo "username" è stato rimosso
 * dall'entity (era ridondante con la FK verso Utente).
 * Per cercare i dati di sicurezza di un utente, passare dalla relazione:
 *   utente.getUtenteSecurety()
 * oppure fare query tramite UtenteRepository.findByUsername() e poi navigare.
 */
public interface UtenteSecRepository extends JpaRepository<UtenteSecurety, Long> {

}
