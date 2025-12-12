package uk.gov.moj.cpp.material.query.view.response;

import java.util.UUID;

public class UpdatedBy {

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

    public String getName() {
        return name;
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

    public static Builder builder(){
        return new Builder();
    }

    public static final class Builder {
        private UUID id;
        private String firstName;
        private String lastName;
        private String name;

        private Builder() {
        }

        public Builder withId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder withFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder withLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder withName(String name){
            this.name = name;
            return this;
        }

        public UpdatedBy build() {
            return new UpdatedBy(id, firstName, lastName, name);
        }
    }
}
