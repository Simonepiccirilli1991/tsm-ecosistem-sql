package com.tsm_db_sql.db.wiam.exception;

public class UtenteSecuretyException extends RuntimeException {

    private String messaggio;
    private String errorCode;

    public UtenteSecuretyException(String messaggio, String errorCode) {
        this.messaggio = messaggio;
        this.errorCode = errorCode;
    }
}
