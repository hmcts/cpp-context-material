package uk.gov.moj.cpp.material.filestore.azure;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * CDI qualifier for the Alfresco-facing Azure Blob container.
 *
 * <p>material owns three Azure Blob containers. Without a qualifier, Weld would find multiple
 * {@code @Produces BlobContainerClient} methods and throw {@code WELD-001409: Ambiguous
 * dependencies}. This annotation selects the Alfresco upload and SAS-generation container
 * (JNDI key prefix {@code material.alfresco.storage.*}).
 */
@Qualifier
@Retention(RUNTIME)
@Target({FIELD, METHOD, PARAMETER, TYPE})
public @interface AlfrescoBlobContainer {
}
