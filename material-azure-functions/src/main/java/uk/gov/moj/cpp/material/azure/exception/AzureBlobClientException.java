package uk.gov.moj.cpp.material.azure.exception;

public class AzureBlobClientException extends RuntimeException {

    public AzureBlobClientException(String message) {
        super(message);
    }

    public AzureBlobClientException(String message, Exception e) {
        super(message, e);
    }
}