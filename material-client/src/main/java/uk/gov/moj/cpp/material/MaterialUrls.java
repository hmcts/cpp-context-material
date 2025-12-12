package uk.gov.moj.cpp.material;

@SuppressWarnings("squid:S1075")
public class MaterialUrls {

    public static final String BASE_URI = "http://localhost:8080/material-query-api/query/api/rest/material";
    public static final String COMMAND_BASE_URI = "http://localhost:8080/material-command-api/command/api/rest/material";
    public static final String MATERIAL_REQUEST_PATH = "/material/";
    public static final String MATERIAL_METADATA_REQUEST_PATH = "/material/%s/metadata";
    public static final String BUNDLE_MATERIAL_REQUEST_PATH = "/create-material-bundle/";
    public static final String MATERIAL_STREAM_PDF_PARAMETERS = "?stream=true&requestPdf=true";

    private MaterialUrls() {
    }

}
