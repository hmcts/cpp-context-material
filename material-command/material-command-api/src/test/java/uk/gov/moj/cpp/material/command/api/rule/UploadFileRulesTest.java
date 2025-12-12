package uk.gov.moj.cpp.material.command.api.rule;


import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class UploadFileRulesTest extends BaseDroolsAccessControlTest {

    private static final String[] ALLOWED_USER_GROUPS = new String[]{"System Users", "SJP Prosecutors", "Court Administrators", "Legal Advisers", "Crown Court Admin", "Listing Officers", "Judiciary", "Court Clerks", "Defence Lawyers", "District Judge", "Court Associate", "Second Line Support", "Deputies", "DJMC", "Advocates", "Probation Admin", "Youth Offending Service Admin", "Judge", "Recorders", "Magistrates", "Auditors", "Police Admin","Non Police Prosecutors"};
    private Action action;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public UploadFileRulesTest() {
        super("COMMAND_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @BeforeEach
    public void setUp() {
        action = createActionFor("material.command.upload-file");
    }

    @Test
    public void shouldAllowFileUploadWhenCallerBelongsToAnyAllowedGroup() {
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, ALLOWED_USER_GROUPS)).thenReturn(true);

        final ExecutionResults executionResults = executeRulesWith(action);

        assertSuccessfulOutcome(executionResults);

        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(action, ALLOWED_USER_GROUPS);
    }

    @Test
    public void shouldNotAllowFileUploadWhenCallerDoesNotBelongToAnyAllowedGroup() throws Exception {
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, ALLOWED_USER_GROUPS)).thenReturn(false);

        final ExecutionResults executionResults = executeRulesWith(action);

        assertFailureOutcome(executionResults);

        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(action, ALLOWED_USER_GROUPS);
    }
}
