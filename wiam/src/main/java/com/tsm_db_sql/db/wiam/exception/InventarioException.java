package com.tsm_db_sql.db.wiam.exception;

import lombok.Data;

@Data
public class InventarioException extends RuntimeException {

    private String messaggio;
    private String errorCode;

    public InventarioException(String messaggio, String errorCode) {
        this.messaggio = messaggio;
        this.errorCode = errorCode;
    }
}
