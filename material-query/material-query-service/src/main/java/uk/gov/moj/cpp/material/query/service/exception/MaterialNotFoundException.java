package uk.gov.moj.cpp.material.query.service.exception;

public class MaterialNotFoundException extends RuntimeException {

    public MaterialNotFoundException(String message) {
        super(message);
    }
}
