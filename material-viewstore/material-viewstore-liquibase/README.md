# Instructions for creating the database

1. Install Postgres 9.4 or later
2. Create a user called postgres
3. Create a database called cppmaterial
4. Run with the following command (Note: the 'update' command at the end may not be required):
    mvn resources:resources liquibase:update  -Dliquibase.url=jdbc:postgresql://localhost:5432/postgres -DdefaultSchemaName=material -Dliquibase.username=postgres -Dliquibase.password=postgres -Dliquibase.logLevel=info
   Or
    java -jar material-liquibase-<version>.jar --url=jdbc:postgresql://localhost:5432/postgres --defaultSchemaName=material --username=postgres --password=postgres --logLevel=info update

All tables can be dropped by running:

java -jar material-liquibase-<version>.jar --url=jjdbc:postgresql://localhost:5432/postgres --defaultSchemaName=material --username=postgres --password=postgres --logLevel=info dropAll    