package com.tsm_db_sql.db.wiam.model.response;

import com.tsm_db_sql.db.wiam.utils.UtenteRoles;

public record LoginUtenteResponse(

    String nome,
    String cognome,
    UtenteRoles ruolo,
    String email
) {
}
