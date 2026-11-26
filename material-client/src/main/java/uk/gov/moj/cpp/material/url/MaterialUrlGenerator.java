package uk.gov.moj.cpp.material.url;

import static uk.gov.moj.cpp.material.MaterialUrls.BASE_URI;
import static uk.gov.moj.cpp.material.MaterialUrls.MATERIAL_REQUEST_PATH;
import static uk.gov.moj.cpp.material.MaterialUrls.MATERIAL_STREAM_PDF_PARAMETERS;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MaterialUrlGenerator {

    private String baseFileStreamUrl(final UUID materialId) {
        return BASE_URI + MATERIAL_REQUEST_PATH + materialId;
    }

    public String pdfFileStreamUrlFor(final UUID materialId) {
        return baseFileStreamUrl(materialId) + MATERIAL_STREAM_PDF_PARAMETERS;
    }

    public String fileStreamUrlFor(final UUID materialId, final boolean pdfStream) {
        return pdfStream ? pdfFileStreamUrlFor(materialId) : baseFileStreamUrl(materialId);
    }

    public String fileStreamUrlFor(final UUID materialId) {
        return fileStreamUrlFor(materialId, false);
    }

}
