package com.nttdata.account.msaccount.exception;

public class RemoteServiceUnavailableException extends RuntimeException {
    public RemoteServiceUnavailableException(String message) {
        super(message);
    }

    public RemoteServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
