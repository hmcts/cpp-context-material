package uk.gov.moj.cpp.material.event.processor.error;

public class MaterialNotFoundException extends RuntimeException {

    public MaterialNotFoundException(String message) {
        super(message);
    }
}
