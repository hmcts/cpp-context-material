package uk.gov.moj.cpp.material.event.processor.azure.service.exception;

public class CloudException extends RuntimeException{

    public CloudException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public CloudException(final String message) {
        super(message);
    }
}
