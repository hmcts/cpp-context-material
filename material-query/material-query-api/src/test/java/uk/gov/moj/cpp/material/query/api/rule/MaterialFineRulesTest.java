package uk.gov.moj.cpp.material.query.api.rule;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.progression.providers.ProgressionProvider;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;


//TODO {CRC-3399} to be moved to controller layer
public class MaterialFineRulesTest extends BaseDroolsAccessControlTest {

    private static final List<String> SYSTEM_USER_GROUPS = Arrays.asList("System Users");

    private static final List<String> CPS_PROGRESSION_CASE_ALLOWED_USER_GROUPS = Arrays.asList(
            "Court Clerks",
            "Crown Court Admin",
            "Listing Officers",
            "Judiciary");

    private static final String MATERIAL_ID = UUID.randomUUID().toString();
    private static final String MATERIAL_QUERY_MATERIAL = "material.query.material";
    private Action action;

    public MaterialFineRulesTest() {
        super("QUERY_API_SESSION");
    }

    @Mock
    private ProgressionProvider progressionProvider;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Captor
    private ArgumentCaptor<List<String>> listCaptor;

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return ImmutableMap.<Class<?>, Object>builder()
                .put(UserAndGroupProvider.class, userAndGroupProvider)
                .put(ProgressionProvider.class, progressionProvider)
                .build();
    }

    @BeforeEach
    public void setUp() {
        final JsonObject inputPayload = JsonObjects.createObjectBuilder().add("materialId", MATERIAL_ID).build();
        final JsonEnvelope envelope1 = envelopeFrom(metadataWithRandomUUID(MATERIAL_QUERY_MATERIAL).build(), inputPayload);
        action = new Action(envelope1);
    }

    @AfterEach
    public void tearDown() {
        verifyNoMoreInteractions(progressionProvider);
    }

    @Test
    public void whenUserIsCPSProgressionCaseAllowedAndCaseIsCPSProgression_thenSuccessful() {
        when(progressionProvider.isMaterialFromCPSProsecutedCase(action)).thenReturn(true);
        whenUserIsCPSProgressionAllowed();

        final ExecutionResults executionResults = executeRulesWith(action);

        assertSuccessfulOutcome(executionResults);
        verifyListOfUserGroups();
        verify(progressionProvider).isMaterialFromCPSProsecutedCase(action);
        verifyNoMoreInteractions(progressionProvider);
    }

    @Test
    public void whenUserIsCPSProgressionCaseAllowedAndCaseIsNotCPSProgression_thenFailure() {
        when(progressionProvider.isMaterialFromCPSProsecutedCase(action)).thenReturn(false);
        whenUserIsCPSProgressionAllowed();

        final ExecutionResults executionResults = executeRulesWith(action);

        assertFailureOutcome(executionResults);
        verifyListOfUserGroups();
        verify(progressionProvider).isMaterialFromCPSProsecutedCase(action);
    }


    @Test
    public void whenUserIsListingOfficerAndMaterialIsNotAccessibleByGroup_thenSuccessfull() {
        whenUserIsNotAllowedToAny();
        final ExecutionResults executionResults = executeRulesWith(action);
        assertFailureOutcome(executionResults);
        verifyListOfUserGroups();
    }

    @Test
    public void whenUserIsNotCPS_TFL_ProgressioAllowedFailure() {
        whenUserIsNotAllowedToAny();
        ExecutionResults executionResults = executeRulesWith(action);
        assertFailureOutcome(executionResults);
    }

    @Test
    public void whenUserIsSystemUser_thenSuccessful() {
        whenUserIsSystemUser();

        final ExecutionResults executionResults = executeRulesWith(action);

        assertSuccessfulOutcome(executionResults);
        verifyListOfUserGroups();
        verifyNoMoreInteractions(progressionProvider);
    }

    private void whenUserIsSystemUser() {
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(any(Action.class), eq(SYSTEM_USER_GROUPS))).thenReturn(true);
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(any(Action.class), eq(CPS_PROGRESSION_CASE_ALLOWED_USER_GROUPS))).thenReturn(false);
    }

    private void whenUserIsCPSAllowed() {
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(any(Action.class), eq(SYSTEM_USER_GROUPS))).thenReturn(false);
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(any(Action.class), eq(CPS_PROGRESSION_CASE_ALLOWED_USER_GROUPS))).thenReturn(false);
    }

    private void whenUserIsCPSProgressionAllowed() {
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(any(Action.class), eq(SYSTEM_USER_GROUPS))).thenReturn(false);
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(any(Action.class), eq(CPS_PROGRESSION_CASE_ALLOWED_USER_GROUPS))).thenReturn(true);
    }


    private void whenUserIsNotAllowedToAny() {
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(any(Action.class), eq(SYSTEM_USER_GROUPS))).thenReturn(false);
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(any(Action.class), eq(CPS_PROGRESSION_CASE_ALLOWED_USER_GROUPS))).thenReturn(false);
    }

    private void verifyListOfUserGroups() {
        verify(userAndGroupProvider, times(2)).isMemberOfAnyOfTheSuppliedGroups(eq(action), listCaptor.capture());
        assertThat(listCaptor.getValue(), anyOf(
                containsInAnyOrder(SYSTEM_USER_GROUPS.toArray()),
                containsInAnyOrder(CPS_PROGRESSION_CASE_ALLOWED_USER_GROUPS.toArray())
        ));
    }
}
