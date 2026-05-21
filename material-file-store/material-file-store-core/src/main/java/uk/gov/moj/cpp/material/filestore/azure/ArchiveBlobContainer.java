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
 * CDI qualifier for the archive Azure Blob container.
 *
 * <p>Selects the container used by the case archiving pipeline to store ZIP archives
 * (JNDI key prefix {@code material.archive.storage.*}).
 */
@Qualifier
@Retention(RUNTIME)
@Target({FIELD, METHOD, PARAMETER, TYPE})
public @interface ArchiveBlobContainer {
}
