package uk.gov.moj.cpp.material.command.handler.azure.service.exception;

public class MaterialNotFoundException extends RuntimeException{

    public MaterialNotFoundException(String message) {
        super(message);
    }
    public MaterialNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
