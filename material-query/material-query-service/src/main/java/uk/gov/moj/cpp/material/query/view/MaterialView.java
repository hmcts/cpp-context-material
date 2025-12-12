package uk.gov.moj.cpp.material.query.view;

import java.io.InputStream;

public class MaterialView {

    private final InputStream documentInputStream;
    private final String fileName;
    private final String contentType;

    public MaterialView(final String fileName, final InputStream documentInputStream, final String contentType) {
        this.fileName = fileName;
        this.documentInputStream = documentInputStream;
        this.contentType = contentType;
    }

    public InputStream getDocumentInputStream() {
        return documentInputStream;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }
}
