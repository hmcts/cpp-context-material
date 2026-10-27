package uk.gov.moj.cpp.material.queryapi.accesscontrol;

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
 * J17/J25 parity characterisation (J17 golden master), MA-M02 / BC-20.
 *
 * Deny assertions in access-control tests also pass when the kbase loads no rules at all, so pin exactly which
 * rules the QUERY_API kbase loads.
 */
class QueryApiRuleCountParityTest {

    @Test
    void maM02_queryApiKbaseShouldLoadAllEightAccessControlRules() {
        final KieBase kieBase = KieServices.Factory.get().getKieClasspathContainer().getKieBase("QUERY_API");

        final List<String> ruleNames = kieBase.getKiePackages().stream()
                .map(KiePackage::getRules)
                .flatMap(rules -> rules.stream().map(Rule::getName))
                .collect(toList());

        assertThat(ruleNames, containsInAnyOrder(
                "Query - material and material metadata - Allowed user groups",
                "Query - material and material metadata detail - Allowed user groups",
                "Query - find downloadable materials  - Allowed user groups",
                "Query - material - System user",
                "Query - material - CPS Prosecuted  Progression",
                "Command - Rule for query structured form",
                "Command - Rule for query structured-form-change-history",
                "Command - Rule for query structured form defendant user"));
    }
}
