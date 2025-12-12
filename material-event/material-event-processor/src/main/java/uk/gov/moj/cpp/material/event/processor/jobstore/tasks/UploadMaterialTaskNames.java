package uk.gov.moj.cpp.material.event.processor.jobstore.tasks;

@SuppressWarnings("squid:S1214")
public final class UploadMaterialTaskNames {

    public static final String MERGE_FILE_TASK = "material.merge-file";
    public static final String FAILED_MERGE_FILE_TASK = "material.failed-merge-file";
    public static final String UPLOAD_BUNDLE_TO_ALFRESCO_TASK = "material.upload-bundle-to-alfresco";
    public static final String SUCCESSFUL_BUNDLE_UPLOAD_COMMAND_TASK = "material.successful-bundle-upload-to-alfresco";
    public static final String FAILED_BUNDLE_UPLOAD_COMMAND_TASK = "material.failed-bundle-upload-to-alfresco";

    public static final String UPLOAD_FILE_TO_ALFRESCO_TASK = "material.upload-file-to-alfresco";
    public static final String SUCCESSFUL_MATERIAL_UPLOAD_COMMAND_TASK = "material.successful-upload-to-alfresco";
    public static final String FAILED_MATERIAL_UPLOAD_COMMAND_TASK = "material.failed-upload-to-alfresco";

    private UploadMaterialTaskNames() {}
}
