package com.tsm_db_sql.db.wiam.model.request;

public record RetrivePswStep2Request(
        String username,
        String otp
) {
}
