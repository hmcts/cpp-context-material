package uk.gov.moj.cpp.material.command.handler.azure.service.exception;

public class FileRetrieveException extends RuntimeException {

    public FileRetrieveException(final String message) {
        super(message);
    }

    public FileRetrieveException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
