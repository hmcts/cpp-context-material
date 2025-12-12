package uk.gov.moj.cpp.material.domain;


public class UploadedMaterial {

    //Id of the material in the external system (e.g. Alfresco)
    private final String externalId;
    private final String mimeType;

    public UploadedMaterial(final String externalId, final String mimeType) {
        this.externalId = externalId;
        this.mimeType = mimeType;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getMimeType() {
        return mimeType;
    }
}
