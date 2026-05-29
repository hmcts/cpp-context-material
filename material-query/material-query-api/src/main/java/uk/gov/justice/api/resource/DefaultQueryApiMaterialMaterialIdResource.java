package uk.gov.justice.api.resource;

import static java.lang.String.format;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.newInputStream;
import static java.time.LocalDateTime.now;
import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LOCATION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;

import uk.gov.justice.services.core.accesscontrol.AccessControlFailureMessageGenerator;
import uk.gov.justice.services.core.accesscontrol.AccessControlService;
import uk.gov.justice.services.core.accesscontrol.AccessControlViolation;
import uk.gov.justice.services.core.audit.AuditService;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.query.service.AlfrescoReadService;
import uk.gov.moj.cpp.material.query.service.AzureBlobClientService;
import uk.gov.moj.cpp.material.query.service.exception.AzureBlobClientException;
import uk.gov.moj.cpp.material.query.view.MaterialView;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S00116")
@Stateless
public class DefaultQueryApiMaterialMaterialIdResource implements MaterialMaterialIdResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultQueryApiMaterialMaterialIdResource.class);

    private static final String RESPONSE_ERROR_MSG_KEY = "error";
    private static final String DATE_TIME_FORMAT = "yyyyMMdd-HHmmss";
    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

    @Inject
    private AlfrescoReadService alfrescoReadService;

    @Inject
    private AccessControlService accessControlService;

    @Inject
    private AccessControlFailureMessageGenerator accessControlFailureMessageGenerator;

    @Inject
    private MaterialEnvelopeFactory accessControlEnvelopeFactory;

    @Inject
    private AuditService auditService;

    @Inject
    private AzureBlobClientService azureBlobClientService;

    @Override
    public Response getMaterialByMaterialId(final String materialId, final String filename, final String userId) {
        return getMaterialByMaterialId(materialId, userId, false);
    }

    @Override
    public Response getMaterialByMaterialId(String materialId, final boolean requestPdf, final String userId) {
        return getMaterialByMaterialId(materialId, userId, requestPdf);
    }

    private Response getMaterialByMaterialId(final String materialId,
                                             final String userId,
                                             final boolean requestPdf) {
        final JsonEnvelope envelope = accessControlEnvelopeFactory.buildEnvelope(
                userId,
                materialId);

        auditService.audit(envelope, QUERY_API);

        final Optional<AccessControlViolation> violation = accessControlService.checkAccessControl(QUERY_API, envelope);

        if (violation.isPresent()) {
            final String errorMessage = accessControlFailureMessageGenerator.errorMessageFrom(
                    envelope,
                    violation.get());

            final JsonObject responseErrorMsg = createObjectBuilder()
                    .add(RESPONSE_ERROR_MSG_KEY, errorMessage)
                    .build();

            return status(FORBIDDEN).entity(responseErrorMsg.toString()).build();
        }

        final Optional<MaterialView> materialView = requestPdf ? alfrescoReadService.getDataAsPdfById(fromString(materialId)) : alfrescoReadService.getDataById(fromString(materialId));

        if (materialView.isPresent()) {
            final MaterialView materialViewValue = materialView.get();
            final InputStream documentInputStream = materialViewValue.getDocumentInputStream();
            final Response.ResponseBuilder responseBuilder = status(OK);
            final String documentContentType = requestPdf ? "application/pdf" : materialViewValue.getContentType();
            generateSASUrlAndSetResponse(materialId, materialViewValue, documentInputStream, "text/uri-list", responseBuilder, documentContentType);
            return responseBuilder.build();
        }
        return status(NOT_FOUND).build();
    }

    private void generateSASUrlAndSetResponse(final String materialId, final MaterialView materialViewValue,
                                              final InputStream documentInputStream, final String contentType,
                                              final Response.ResponseBuilder responseBuilder, final String documentContentType) {

        Path tempFilePath = null;
        final LocalDateTime startTime = LocalDateTime.now();
        final String destinationFileName = format("%s-%s-%s/%s", now().format(fmt), randomAlphanumeric(10), materialId, materialViewValue.getFileName());

        try {
            tempFilePath = createTempFile("mat_", null);
            copyInputStreamToFile(documentInputStream, tempFilePath.toFile());

            final InputStream fileInputStream = newInputStream(tempFilePath);
            final String sasUri = azureBlobClientService.upload(fileInputStream, destinationFileName, documentContentType);

            responseBuilder.entity(sasUri).location(new URI(sasUri));
            responseBuilder.header(CONTENT_LOCATION, "filename=" + materialViewValue.getFileName());
            responseBuilder.header(CONTENT_TYPE, contentType);
        } catch (URISyntaxException ex) {
            throw new AzureBlobClientException("SAS URI parse error", ex);
        } catch (IOException ex) {
            throw new AzureBlobClientException("Error while downloading file to local temp location", ex);
        } finally {
            Optional.ofNullable(tempFilePath).ifPresent(this::deleteTempFile);
            LOGGER.info("Total time taken={} ms to read stream from Alfresco - store to temp file and upload destinationFileName={} to azure blob storage", Duration.between(startTime, LocalDateTime.now()).toMillis(), destinationFileName);
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("FileName passed as HTTP Header Content location {}", materialViewValue.getFileName());
        }
    }

    private void deleteTempFile(final Path filePath) {
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            LOGGER.warn("Failed to delete tempFile: {}", filePath, e);
        }
    }
}