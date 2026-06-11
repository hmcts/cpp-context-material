package uk.gov.moj.cpp.material.command.api.accesscontrol;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.accesscontrol.drools.ExpectedPermission.builder;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.accesscontrol.drools.ExpectedPermission;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RuleConstants {
    private static final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private static final String GRANT_ACCESS = "GrantAccess";
    private static final String NON_CPS_DOCUMENT = "NonCpsDocument";
    private static final String VIEW = "View";
    private static final String UPLOAD = "Upload";
    private static final String DOWNLOAD = "Download";
    private static final String ACTION = "action";
    private static final String OBJECT = "object";


    private RuleConstants() {
    }

    public static String structuredFormGrantAccessPermission() {
        return createObjectBuilder().add(OBJECT, "PetForm").add(ACTION, GRANT_ACCESS).build().toString();
    }

    public static String structuredFormGrantAccessPermissionForPET() {
        return createObjectBuilder().add(OBJECT, "PetForm").add(ACTION, GRANT_ACCESS).build().toString();
    }

    public static String getBCMCreatePermission() throws JsonProcessingException {
        final ExpectedPermission expectedPermission = builder()
                .withObject("BCM")
                .withAction("Create")
                .build();
        return objectMapper.writeValueAsString(expectedPermission);
    }

    public static String getPTPHCreatePermission() throws JsonProcessingException {
        final ExpectedPermission expectedPermission = builder()
                .withObject("PTPH")
                .withAction("Create")
                .build();
        return objectMapper.writeValueAsString(expectedPermission);
    }

    public static String getBCMEditPermission() throws JsonProcessingException {
        final ExpectedPermission expectedPermission = builder()
                .withObject("BCM")
                .withAction("Edit")
                .build();
        return objectMapper.writeValueAsString(expectedPermission);
    }

    public static String getPTPHEditPermission() throws JsonProcessingException {
        final ExpectedPermission expectedPermission = builder()
                .withObject("PTPH")
                .withAction("Edit")
                .build();
        return objectMapper.writeValueAsString(expectedPermission);
    }

    public static String[] getNonCpsDocumentUploadPermission() {
        return new String[]{
                createObjectBuilder().add(OBJECT, NON_CPS_DOCUMENT).add(ACTION, UPLOAD).build().toString()};
    }

    public static String[] getNonCpsDocumentReadPermission() {
        return new String[]{
                createObjectBuilder().add(OBJECT, NON_CPS_DOCUMENT).add(ACTION, VIEW).build().toString(),
                createObjectBuilder().add(OBJECT, NON_CPS_DOCUMENT).add(ACTION, DOWNLOAD).build().toString()};
    }


}
