package uk.gov.moj.cpp.material.domain;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@SuppressWarnings("pmd:BeanMembersShouldSerialize")
public class UpdatedBy implements Serializable {

    private static final long serialVersionUID = 826060005554657240L;
    private UUID id;
    private String firstName;
    private String lastName;
    private String name;

    public UpdatedBy(final UUID id, final String firstName, final String lastName, final String name) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.name = name;
    }

    public UpdatedBy(final UUID id, final String firstName, final String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @JsonCreator
    public UpdatedBy(final UUID id) {
        this.id = id;
    }

    public UpdatedBy(final String name) {
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getName() {
        return name;
    }
}
