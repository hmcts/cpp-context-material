package uk.gov.moj.cpp.people.healthchecks;

import static java.util.Collections.emptyList;

import uk.gov.justice.services.healthcheck.api.DefaultIgnoredHealthcheckNamesProvider;

import java.util.List;

import javax.enterprise.inject.Specializes;

@Specializes
public class MaterialIgnoredHealthcheckNamesProvider extends DefaultIgnoredHealthcheckNamesProvider {

    public MaterialIgnoredHealthcheckNamesProvider() {
        // This constructor is required by CDI.
    }

    @Override
    public List<String> getNamesOfIgnoredHealthChecks() {
        return emptyList();
    }
}