package uk.gov.moj.cpp.material.event.processor.jobstore.util;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class FileExtensionResolver {

    private static final Map<String, String> mediaTypeMap = new HashMap<>();

    private FileExtensionResolver() {
    }

    static {

        // Supported media types as specified in "Common Platform Prosecutor Interface (CPPI) External API Specification" document.

        mediaTypeMap.put("application/pdf", "pdf");
        mediaTypeMap.put("text/plain", "txt");
        mediaTypeMap.put("application/msword", "doc");
        mediaTypeMap.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");
        mediaTypeMap.put("application/vnd.ms-excel", "xls");
        mediaTypeMap.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");
        mediaTypeMap.put("application/xml", "xml");
        mediaTypeMap.put("text/html", "html");
        mediaTypeMap.put("application/vnd.ms-word.document.macroEnabled.12", "docm");
        mediaTypeMap.put("message/rfc822", "mht");
        mediaTypeMap.put("image/jpeg", "jpeg");
        mediaTypeMap.put("image/tiff", "tif");
        mediaTypeMap.put("application/vnd.ms-outlook", "msg");
        mediaTypeMap.put("application/rtf", "rtf");

    }

    public static String getFileExtension(final String mimeType) {
        return mediaTypeMap.get(mimeType);
    }

    public static String getMimeType(String fileExtension){
        BiMap<String,String> biMap = HashBiMap.create(mediaTypeMap);
        return  biMap.inverse().get(fileExtension);
    }
}
