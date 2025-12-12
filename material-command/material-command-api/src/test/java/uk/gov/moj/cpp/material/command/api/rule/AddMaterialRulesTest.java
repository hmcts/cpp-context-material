package uk.gov.moj.cpp.material.command.api.rule;


import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;
import uk.gov.moj.cpp.material.command.api.accesscontrol.RuleConstants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class AddMaterialRulesTest extends BaseDroolsAccessControlTest {

    private static final String[] ALLOWED_USER_GROUPS = new String[] {
            "System Users",
            "CMS",
            "Charging Lawyers",
            "SJP Prosecutors",
            "Court Administrators",
            "Crown Court Admin",
            "Listing Officers",
            "Judiciary","Court Clerks", "Second Line Support"};

    private static final String MATERIAL_COMMAND_ADD_MATERIAL = "material.command.add-material";
    private Action action;

    public AddMaterialRulesTest() {
        super("COMMAND_API_SESSION");
    }

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @BeforeEach
    public void setUp() throws Exception {
        action = createActionFor(MATERIAL_COMMAND_ADD_MATERIAL);
    }

    @AfterEach
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(userAndGroupProvider);
    }

    @Test
    public void whenUserIsAMemberOfAllowedUserGroups_thenSuccessfull() throws Exception {
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, ALLOWED_USER_GROUPS)).thenReturn(true);

        final ExecutionResults executionResults = executeRulesWith(action);

        assertSuccessfulOutcome(executionResults);

        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(action, ALLOWED_USER_GROUPS);
    }

    @Test
    public void whenUserIsNotAMemberOfAllowedUserGroups_thenFailure() throws Exception {
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, ALLOWED_USER_GROUPS)).thenReturn(false);

        final ExecutionResults executionResults = executeRulesWith(action);

        assertFailureOutcome(executionResults);

        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(action, ALLOWED_USER_GROUPS);
    }

    @Test
    public void shouldNotAllowAuthorisedUserAddMaterialWithoutNonCpsDocuments() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "material.add-material");
        action = createActionFor(metadata);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "CMS", "Charging Lawyers", "SJP Prosecutors", "Court Administrators", "Legal Advisers", "Crown Court Admin", "Listing Officers", "Judiciary", "Court Clerks", "Defence Users", "District Judge", "Court Associate", "Second Line Support", "Magistrates", "Auditors")).willReturn(false);
        given(userAndGroupProvider.hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission())).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "CMS", "Charging Lawyers", "SJP Prosecutors", "Court Administrators", "Legal Advisers", "Crown Court Admin", "Listing Officers", "Judiciary", "Court Clerks", "Defence Users", "District Judge", "Court Associate", "Second Line Support", "Magistrates", "Auditors");
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission());
    }

    @Test
    public void shouldAllowAuthorisedUserAddMaterialWithPermissionNonCpsDocuments() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "material.add-material");
        action = createActionFor(metadata);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "CMS", "Charging Lawyers", "SJP Prosecutors", "Court Administrators", "Legal Advisers", "Crown Court Admin", "Listing Officers", "Judiciary", "Court Clerks", "Defence Users", "District Judge", "Court Associate", "Second Line Support", "Magistrates", "Auditors")).willReturn(false);
        given(userAndGroupProvider.hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission())).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "CMS", "Charging Lawyers", "SJP Prosecutors", "Court Administrators", "Legal Advisers", "Crown Court Admin", "Listing Officers", "Judiciary", "Court Clerks", "Defence Users", "District Judge", "Court Associate", "Second Line Support", "Magistrates", "Auditors");
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission());
    }

    @Test
    public void shouldAllowAuthorisedUserAddMaterialWithNonCpsDocuments() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "material.add-material");
        action = createActionFor(metadata);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "CMS", "Charging Lawyers", "SJP Prosecutors", "Court Administrators", "Legal Advisers", "Crown Court Admin", "Listing Officers", "Judiciary", "Court Clerks", "Defence Users", "District Judge", "Court Associate", "Second Line Support", "Magistrates", "Auditors")).willReturn(true);
        given(userAndGroupProvider.hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission())).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "CMS", "Charging Lawyers", "SJP Prosecutors", "Court Administrators", "Legal Advisers", "Crown Court Admin", "Listing Officers", "Judiciary", "Court Clerks", "Defence Users", "District Judge", "Court Associate", "Second Line Support", "Magistrates", "Auditors");
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission());
    }

    @Test
    public void shouldNotAllowAuthorisedUserUploadMaterialWithoutNonCpsDocuments() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "material.command.upload-file");
        action = createActionFor(metadata);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "SJP Prosecutors", "Court Administrators", "Legal Advisers","Crown Court Admin", "Listing Officers", "Judiciary","Court Clerks", "Defence Lawyers", "District Judge", "Court Associate", "Second Line Support", "Deputies", "DJMC", "Advocates", "Probation Admin", "Youth Offending Service Admin", "Judge", "Recorders", "Magistrates", "Auditors", "Police Admin","Non Police Prosecutors")).willReturn(false);
        given(userAndGroupProvider.hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission())).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "SJP Prosecutors", "Court Administrators", "Legal Advisers","Crown Court Admin", "Listing Officers", "Judiciary","Court Clerks", "Defence Lawyers", "District Judge", "Court Associate", "Second Line Support", "Deputies", "DJMC", "Advocates", "Probation Admin", "Youth Offending Service Admin", "Judge", "Recorders", "Magistrates", "Auditors", "Police Admin","Non Police Prosecutors");
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission());
    }

    @Test
    public void shouldAllowAuthorisedUserAddUploadWithPermissionNonCpsDocuments() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "material.command.upload-file");
        action = createActionFor(metadata);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "SJP Prosecutors", "Court Administrators", "Legal Advisers","Crown Court Admin", "Listing Officers", "Judiciary","Court Clerks", "Defence Lawyers", "District Judge", "Court Associate", "Second Line Support", "Deputies", "DJMC", "Advocates", "Probation Admin", "Youth Offending Service Admin", "Judge", "Recorders", "Magistrates", "Auditors", "Police Admin","Non Police Prosecutors")).willReturn(false);
        given(userAndGroupProvider.hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission())).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "SJP Prosecutors", "Court Administrators", "Legal Advisers","Crown Court Admin", "Listing Officers", "Judiciary","Court Clerks", "Defence Lawyers", "District Judge", "Court Associate", "Second Line Support", "Deputies", "DJMC", "Advocates", "Probation Admin", "Youth Offending Service Admin", "Judge", "Recorders", "Magistrates", "Auditors", "Police Admin","Non Police Prosecutors");
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission());
    }

    @Test
    public void shouldAllowAuthorisedUserUploadMaterialWithNonCpsDocuments() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "material.command.upload-file");
        action = createActionFor(metadata);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "SJP Prosecutors", "Court Administrators", "Legal Advisers","Crown Court Admin", "Listing Officers", "Judiciary","Court Clerks", "Defence Lawyers", "District Judge", "Court Associate", "Second Line Support", "Deputies", "DJMC", "Advocates", "Probation Admin", "Youth Offending Service Admin", "Judge", "Recorders", "Magistrates", "Auditors", "Police Admin","Non Police Prosecutors")).willReturn(true);
        given(userAndGroupProvider.hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission())).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "SJP Prosecutors", "Court Administrators", "Legal Advisers","Crown Court Admin", "Listing Officers", "Judiciary","Court Clerks", "Defence Lawyers", "District Judge", "Court Associate", "Second Line Support", "Deputies", "DJMC", "Advocates", "Probation Admin", "Youth Offending Service Admin", "Judge", "Recorders", "Magistrates", "Auditors", "Police Admin","Non Police Prosecutors");
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission());
    }

    @Test
    public void shouldNotAllowAuthorisedUserUploadPdfMaterialWithoutNonCpsDocuments() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "material.command.upload-file-as-pdf");
        action = createActionFor(metadata);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "SJP Prosecutors", "Court Administrators", "Legal Advisers","Crown Court Admin", "Listing Officers", "Judiciary","Court Clerks", "Defence Lawyers", "District Judge", "Court Associate", "Second Line Support", "Deputies", "DJMC", "Advocates", "Probation Admin", "Youth Offending Service Admin", "Judge", "Recorders", "Auditors", "Police Admin","Non Police Prosecutors")).willReturn(false);
        given(userAndGroupProvider.hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission())).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "SJP Prosecutors", "Court Administrators", "Legal Advisers","Crown Court Admin", "Listing Officers", "Judiciary","Court Clerks", "Defence Lawyers", "District Judge", "Court Associate", "Second Line Support", "Deputies", "DJMC", "Advocates", "Probation Admin", "Youth Offending Service Admin", "Judge", "Recorders", "Auditors", "Police Admin","Non Police Prosecutors");
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission());
    }

    @Test
    public void shouldAllowAuthorisedUserAddUploadPdfWithPermissionNonCpsDocuments() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "material.command.upload-file-as-pdf");
        action = createActionFor(metadata);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "SJP Prosecutors", "Court Administrators", "Legal Advisers","Crown Court Admin", "Listing Officers", "Judiciary","Court Clerks", "Defence Lawyers", "District Judge", "Court Associate", "Second Line Support", "Deputies", "DJMC", "Advocates", "Probation Admin", "Youth Offending Service Admin", "Judge", "Recorders", "Auditors", "Police Admin","Non Police Prosecutors")).willReturn(false);
        given(userAndGroupProvider.hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission())).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "SJP Prosecutors", "Court Administrators", "Legal Advisers","Crown Court Admin", "Listing Officers", "Judiciary","Court Clerks", "Defence Lawyers", "District Judge", "Court Associate", "Second Line Support", "Deputies", "DJMC", "Advocates", "Probation Admin", "Youth Offending Service Admin", "Judge", "Recorders", "Auditors", "Police Admin","Non Police Prosecutors");
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission());
    }

    @Test
    public void shouldAllowAuthorisedUserUploadPdfMaterialWithNonCpsDocuments() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "material.command.upload-file-as-pdf");
        action = createActionFor(metadata);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "SJP Prosecutors", "Court Administrators", "Legal Advisers","Crown Court Admin", "Listing Officers", "Judiciary","Court Clerks", "Defence Lawyers", "District Judge", "Court Associate", "Second Line Support", "Deputies", "DJMC", "Advocates", "Probation Admin", "Youth Offending Service Admin", "Judge", "Recorders", "Auditors", "Police Admin","Non Police Prosecutors")).willReturn(true);
        given(userAndGroupProvider.hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission())).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "SJP Prosecutors", "Court Administrators", "Legal Advisers","Crown Court Admin", "Listing Officers", "Judiciary","Court Clerks", "Defence Lawyers", "District Judge", "Court Associate", "Second Line Support", "Deputies", "DJMC", "Advocates", "Probation Admin", "Youth Offending Service Admin", "Judge", "Recorders", "Auditors", "Police Admin","Non Police Prosecutors");
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission());
    }

    @Test
    public void shouldNotAllowAuthorisedUserPublishMaterialWithoutNonCpsDocuments() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "material.command.publish-is-downloadable-materials");
        action = createActionFor(metadata);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "System Users","Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers",
                "Prison Admin", "Court Admin", "Probation Admin", "Police Admin", "Victims & Witness Care Admin", "Youth Offending Service Admin",
                "Magistrates", "Court Associate", "District Judge", "Probation Admin", "Judiciary", "Court Associate", "Deputies", "DJMC", "Judge",
                "Second Line Support", "NCES", "Recorders", "Charging Lawyers", "Defence Users", "Defence Lawyers", "Advocates", "Auditors","Non Police Prosecutors", "Non CPS Prosecutors")).willReturn(false);
        given(userAndGroupProvider.hasPermission(action, RuleConstants.getNonCpsDocumentReadPermission())).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, "System Users","Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers",
                "Prison Admin", "Court Admin", "Probation Admin", "Police Admin", "Victims & Witness Care Admin", "Youth Offending Service Admin",
                "Magistrates", "Court Associate", "District Judge", "Probation Admin", "Judiciary", "Court Associate", "Deputies", "DJMC", "Judge",
                "Second Line Support", "NCES", "Recorders", "Charging Lawyers", "Defence Users", "Defence Lawyers", "Advocates", "Auditors","Non Police Prosecutors", "Non CPS Prosecutors");
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getNonCpsDocumentReadPermission());
    }

    @Test
    public void shouldAllowAuthorisedUserPublishWithPermissionNonCpsDocuments() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "material.command.publish-is-downloadable-materials");
        action = createActionFor(metadata);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "System Users","Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers",
                "Prison Admin", "Court Admin", "Probation Admin", "Police Admin", "Victims & Witness Care Admin", "Youth Offending Service Admin",
                "Magistrates", "Court Associate", "District Judge", "Probation Admin", "Judiciary", "Court Associate", "Deputies", "DJMC", "Judge",
                "Second Line Support", "NCES", "Recorders", "Charging Lawyers", "Defence Users", "Defence Lawyers", "Advocates", "Auditors","Non Police Prosecutors", "Non CPS Prosecutors")).willReturn(false);
        given(userAndGroupProvider.hasPermission(action, RuleConstants.getNonCpsDocumentReadPermission())).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, "System Users","Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers",
                "Prison Admin", "Court Admin", "Probation Admin", "Police Admin", "Victims & Witness Care Admin", "Youth Offending Service Admin",
                "Magistrates", "Court Associate", "District Judge", "Probation Admin", "Judiciary", "Court Associate", "Deputies", "DJMC", "Judge",
                "Second Line Support", "NCES", "Recorders", "Charging Lawyers", "Defence Users", "Defence Lawyers", "Advocates", "Auditors","Non Police Prosecutors", "Non CPS Prosecutors");
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getNonCpsDocumentReadPermission());
    }

    @Test
    public void shouldAllowAuthorisedUserPublishMaterialWithNonCpsDocuments() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "material.command.publish-is-downloadable-materials");
        action = createActionFor(metadata);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "System Users","Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers",
                "Prison Admin", "Court Admin", "Probation Admin", "Police Admin", "Victims & Witness Care Admin", "Youth Offending Service Admin",
                "Magistrates", "Court Associate", "District Judge", "Probation Admin", "Judiciary", "Court Associate", "Deputies", "DJMC", "Judge",
                "Second Line Support", "NCES", "Recorders", "Charging Lawyers", "Defence Users", "Defence Lawyers", "Advocates", "Auditors","Non Police Prosecutors", "Non CPS Prosecutors")).willReturn(true);
        given(userAndGroupProvider.hasPermission(action, RuleConstants.getNonCpsDocumentReadPermission())).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, "System Users","Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers",
                "Prison Admin", "Court Admin", "Probation Admin", "Police Admin", "Victims & Witness Care Admin", "Youth Offending Service Admin",
                "Magistrates", "Court Associate", "District Judge", "Probation Admin", "Judiciary", "Court Associate", "Deputies", "DJMC", "Judge",
                "Second Line Support", "NCES", "Recorders", "Charging Lawyers", "Defence Users", "Defence Lawyers", "Advocates", "Auditors","Non Police Prosecutors", "Non CPS Prosecutors");
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getNonCpsDocumentReadPermission());
    }

    @Test
    public void shouldNotAllowAuthorisedUserAddBundleWithoutNonCpsDocuments() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "material.command.create-material-bundle");
        action = createActionFor(metadata);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "CMS", "Charging Lawyers", "SJP Prosecutors", "Court Administrators", "Legal Advisers", "Crown Court Admin", "Listing Officers", "Judiciary", "Court Clerks", "Defence Users", "District Judge", "Court Associate", "Second Line Support")).willReturn(false);
        given(userAndGroupProvider.hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission())).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "CMS", "Charging Lawyers", "SJP Prosecutors", "Court Administrators", "Legal Advisers", "Crown Court Admin", "Listing Officers", "Judiciary", "Court Clerks", "Defence Users", "District Judge", "Court Associate", "Second Line Support");
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission());
    }

    @Test
    public void shouldAllowAuthorisedUserAddBundleWithPermissionNonCpsDocuments() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "material.command.create-material-bundle");
        action = createActionFor(metadata);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "CMS", "Charging Lawyers", "SJP Prosecutors", "Court Administrators", "Legal Advisers", "Crown Court Admin", "Listing Officers", "Judiciary", "Court Clerks", "Defence Users", "District Judge", "Court Associate", "Second Line Support")).willReturn(false);
        given(userAndGroupProvider.hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission())).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "CMS", "Charging Lawyers", "SJP Prosecutors", "Court Administrators", "Legal Advisers", "Crown Court Admin", "Listing Officers", "Judiciary", "Court Clerks", "Defence Users", "District Judge", "Court Associate", "Second Line Support");
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission());
    }

    @Test
    public void shouldAllowAuthorisedUserAddBundleMaterialWithNonCpsDocuments() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "material.command.create-material-bundle");
        action = createActionFor(metadata);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "CMS", "Charging Lawyers", "SJP Prosecutors", "Court Administrators", "Legal Advisers", "Crown Court Admin", "Listing Officers", "Judiciary", "Court Clerks", "Defence Users", "District Judge", "Court Associate", "Second Line Support")).willReturn(true);
        given(userAndGroupProvider.hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission())).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, "System Users", "CMS", "Charging Lawyers", "SJP Prosecutors", "Court Administrators", "Legal Advisers", "Crown Court Admin", "Listing Officers", "Judiciary", "Court Clerks", "Defence Users", "District Judge", "Court Associate", "Second Line Support");
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission());
    }

    @Test
    public void shouldNotAllowAuthorisedUserAddZipWithoutNonCpsDocuments() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "material.command.zip-material");
        action = createActionFor(metadata);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "System Users")).willReturn(false);
        given(userAndGroupProvider.hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission())).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, "System Users");
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission());
    }

    @Test
    public void shouldAllowAuthorisedUserAddZipWithPermissionNonCpsDocuments() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "material.command.zip-material");
        action = createActionFor(metadata);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "System Users")).willReturn(false);
        given(userAndGroupProvider.hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission())).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, "System Users");
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission());
    }

    @Test
    public void shouldAllowAuthorisedUserAddZipMaterialWithNonCpsDocuments() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "material.command.zip-material");
        action = createActionFor(metadata);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "System Users")).willReturn(true);
        given(userAndGroupProvider.hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission())).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, "System Users");
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getNonCpsDocumentUploadPermission());
    }
}
