package uk.gov.moj.cpp.material.query.service.exception;

public class AlfrescoReadException extends RuntimeException {

    public AlfrescoReadException(final String message) {
        super(message);
    }

    public AlfrescoReadException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
