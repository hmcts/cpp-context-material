package uk.gov.moj.cpp.material.query.api.rule;


import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

public class MaterialRulesTest extends BaseDroolsAccessControlTest {

    private static final List<String> ALLOWED_USER_GROUPS = Arrays.asList(
            "System Users",
            "Charging Lawyers",
            "SJP Prosecutors",
            "Court Administrators",
            "Court Clerks",
            "Legal Advisers",
            "Crown Court Admin",
            "Listing Officers",
            "Judiciary");

    private static final String MATERIAL_QUERY_MATERIAL = "material.query.material";
    private Action action;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Captor
    private ArgumentCaptor<List<String>> listCaptor;

    public MaterialRulesTest() {
        super("QUERY_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @BeforeEach
    public void setUp() throws Exception {
        action = createActionFor(MATERIAL_QUERY_MATERIAL);
    }

    @AfterEach
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(userAndGroupProvider);
    }

    //TODO {CRC-3399} remove @Ignored
    @Disabled
    @Test
    public void whenUserIsAMemberOfAllowedUserGroups_thenSuccessfull() throws Exception {
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(any(Action.class), anyList())).thenReturn(true);

        ExecutionResults executionResults = executeRulesWith(action);

        assertSuccessfulOutcome(executionResults);
        verifyListOfUserGroups();
    }

    //TODO {CRC-3399} remove @Ignored
    @Disabled
    @Test
    public void whenUserIsNotAMemberOfAllowedUserGroups_thenFailure() throws Exception {
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(any(Action.class), anyList())).thenReturn(false);

        ExecutionResults executionResults = executeRulesWith(action);

        assertFailureOutcome(executionResults);
        verifyListOfUserGroups();
    }

    private void verifyListOfUserGroups() {
        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(eq(action), listCaptor.capture());
        assertThat(listCaptor.getValue(), containsInAnyOrder(ALLOWED_USER_GROUPS.toArray()));
    }
}
