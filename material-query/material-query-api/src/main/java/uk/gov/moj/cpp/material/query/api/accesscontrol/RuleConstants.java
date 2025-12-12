package uk.gov.moj.cpp.material.query.api.accesscontrol;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.accesscontrol.drools.ExpectedPermission.builder;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.accesscontrol.drools.ExpectedPermission;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RuleConstants {
    private static final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    static final String GROUP_CROWN_COURT_ADMIN = "Crown Court Admin";
    static final String GROUP_LISTING_OFFICERS = "Listing Officers";
    static final String GROUP_LEGAL_ADVISERS = "Legal Advisers";
    static final String GROUP_COURT_ADMINISTRATORS = "Court Administrators";
    static final String GROUP_SYSTEM_USERS = "System Users";
    static final String GROUP_COURT_ASSOCIATE = "Court Associate";
    static final String GROUP_COURT_CLERKS = "Court Clerks";
    static final String GRANT_ACCESS = "GrantAccess";
    static final String ACTION = "action";
    static final String OBJECT = "object";
    static final String PET_FORM = "PetForm";
    static final String NON_CPS_DOCUMENT = "NonCpsDocument";
    static final String VIEW = "View";
    static final String UPLOAD = "Upload";
    static final String DOWNLOAD = "Download";

    private RuleConstants() {
    }

    public static List<String> getQueryCaseActionGroups() {
        return singletonList(GROUP_SYSTEM_USERS);
    }

    public static List<String> getQueryCaseByProsecutionReferenceActionGroups() {
        return singletonList(GROUP_SYSTEM_USERS);
    }

    public static List<String> getQueryCaseErrorsActionGroups() {
        return asList(GROUP_SYSTEM_USERS, GROUP_CROWN_COURT_ADMIN, GROUP_LISTING_OFFICERS, GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS);
    }

    public static List<String> getQueryCountsCasesErrorsActionGroups() {
        return asList(GROUP_CROWN_COURT_ADMIN, GROUP_LEGAL_ADVISERS, GROUP_COURT_ASSOCIATE, GROUP_COURT_CLERKS);
    }

    public static String relatedCasesGrantAccessPermission() {
        return createObjectBuilder().add(OBJECT, "RelatedCases").add(ACTION, GRANT_ACCESS).build().toString();
    }

    public static String structuredFormGrantAccessPermission() {
        return createObjectBuilder().add(OBJECT, PET_FORM).add(ACTION, GRANT_ACCESS).build().toString();
    }

    public static String structuredFormGrantAccessPermissionForPET() {
        return createObjectBuilder().add(OBJECT, PET_FORM).add(ACTION, GRANT_ACCESS).build().toString();
    }

    public static String getBCMViewPermission() throws JsonProcessingException {
        final ExpectedPermission expectedPermission = builder()
                .withObject("BCM")
                .withAction(VIEW)
                .build();
        return objectMapper.writeValueAsString(expectedPermission);
    }

    public static String getPTPHViewPermission() throws JsonProcessingException {
        final ExpectedPermission expectedPermission = builder()
                .withObject("PTPH")
                .withAction(VIEW)
                .build();
        return objectMapper.writeValueAsString(expectedPermission);
    }

    public static String[] getNonCpsDocumentReadPermission() {
        return new String[]{
                createObjectBuilder().add(OBJECT, NON_CPS_DOCUMENT).add(ACTION, VIEW).build().toString(),
                createObjectBuilder().add(OBJECT, NON_CPS_DOCUMENT).add(ACTION, DOWNLOAD).build().toString()};
    }

}
