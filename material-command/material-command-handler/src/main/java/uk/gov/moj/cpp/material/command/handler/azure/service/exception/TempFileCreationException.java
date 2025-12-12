package uk.gov.moj.cpp.material.command.handler.azure.service.exception;

public class TempFileCreationException extends RuntimeException{

    public TempFileCreationException(String message) {
        super(message);
    }
    public TempFileCreationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
