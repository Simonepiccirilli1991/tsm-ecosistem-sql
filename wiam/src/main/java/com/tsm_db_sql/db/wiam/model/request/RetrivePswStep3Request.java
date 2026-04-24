package com.tsm_db_sql.db.wiam.model.request;

public record RetrivePswStep3Request(
        String username,
        String chiaveStep,
        String password
) {
}
