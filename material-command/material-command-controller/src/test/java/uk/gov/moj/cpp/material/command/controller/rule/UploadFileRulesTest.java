package uk.gov.moj.cpp.material.command.controller.rule;


import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class UploadFileRulesTest extends BaseDroolsAccessControlTest {

    private Action action;

    public UploadFileRulesTest() {
        super("COMMAND_CONTROLLER_SESSION");
    }

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @BeforeEach
    public void setUp() {
        action = createActionFor("material.command.upload-file");
    }

    @Test
    public void shouldAllowFileUpload() {
        final ExecutionResults executionResults = executeRulesWith(action);
        assertSuccessfulOutcome(executionResults);

        verify(userAndGroupProvider, never()).isMemberOfAnyOfTheSuppliedGroups(any());
    }

}
