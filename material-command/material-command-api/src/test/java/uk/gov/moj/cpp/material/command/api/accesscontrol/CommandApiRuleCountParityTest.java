package uk.gov.moj.cpp.material.command.api.accesscontrol;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;

/**
 * J17/J25 parity characterisation (J17 golden master), MA-M01 / BC-20.
 *
 * Deny assertions in access-control tests also pass when the kbase loads no rules at all, so pin exactly which
 * rules the COMMAND_API kbase loads.
 */
class CommandApiRuleCountParityTest {

    @Test
    void maM01_commandApiKbaseShouldLoadTheJ17AccessControlRules() {
        final KieBase kieBase = KieServices.Factory.get().getKieClasspathContainer().getKieBase("COMMAND_API");

        final List<String> ruleNames = kieBase.getKiePackages().stream()
                .map(KiePackage::getRules)
                .flatMap(rules -> rules.stream().map(Rule::getName))
                .collect(toList());

        assertThat(ruleNames, containsInAnyOrder(
                "Command - add material - Allowed user groups",
                "Command - add material reference",
                "Command - upload file",
                "Command - upload file as pdf",
                "Command - delete material",
                "Command - find downloadable materials  - Allowed user groups",
                "Command - create material bundle",
                "Command - zip material"));
    }
}
