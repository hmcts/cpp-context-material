package uk.gov.moj.cpp.material.query.api.rule;


import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

public class MaterialMetadataRulesTest extends BaseDroolsAccessControlTest {

    private static final List<String> ALLOWED_USER_GROUPS = Arrays.asList(
            "System Users",
            "Court Clerks",
            "Charging Lawyers",
            "Crown Court Admin",
            "Listing Officers",
            "Judiciary",
            "Legal Advisers");

    private static final List<String> RANDOM_USER_GROUPS = Arrays.asList(
            "Ramdom Users",
            "Anonymous");

    private static final String MATERIAL_QUERY_MATERIAL_METADATA = "material.query.material-metadata";
    private Action action;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Captor
    private ArgumentCaptor<List<String>> listCaptor;

    public MaterialMetadataRulesTest() {
        super("QUERY_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @BeforeEach
    public void setUp() throws Exception {
        action = createActionFor(MATERIAL_QUERY_MATERIAL_METADATA);

    }

    @AfterEach
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(userAndGroupProvider);
    }

    @Test
    public void whenUserIsAMemberOfAllowedUserGroups_thenSuccessfull() throws Exception {
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(any(Action.class), eq(ALLOWED_USER_GROUPS))).thenReturn(true);

        ExecutionResults executionResults = executeRulesWith(action);

        assertSuccessfulOutcome(executionResults);
        verifyListOfUserGroups();
    }


    @Test
    public void whenUserIsNotAMemberOfAllowedUserGroups_thenFailure() throws Exception {
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(any(Action.class), eq(ALLOWED_USER_GROUPS))).thenReturn(false);

        ExecutionResults executionResults = executeRulesWith(action);

        assertFailureOutcome(executionResults);
        verifyListOfUserGroups();
    }

    private void verifyListOfUserGroups() {
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(eq(action), listCaptor.capture());

        assertThat(listCaptor.getValue(), anyOf(containsInAnyOrder(ALLOWED_USER_GROUPS.toArray()), containsInAnyOrder(RANDOM_USER_GROUPS.toArray())));
    }
}
