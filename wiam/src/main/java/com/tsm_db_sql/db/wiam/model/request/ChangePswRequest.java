package com.tsm_db_sql.db.wiam.model.request;

public record ChangePswRequest(

        String username,
        String oldPassword,
        String newPassword
) {
}
