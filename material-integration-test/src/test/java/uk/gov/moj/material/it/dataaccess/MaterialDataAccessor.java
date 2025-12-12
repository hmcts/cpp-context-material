package uk.gov.moj.material.it.dataaccess;

import static java.util.Optional.empty;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.fromSqlTimestamp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

public class MaterialDataAccessor {

    private static final String SQL =
            "SELECT alfresco_id, filename, mime_type, external_link, date_material_added FROM material where material_id = ?";

    private final TestJdbcConnectionProvider jdbcConnectionProvider = TestJdbcConnectionProvider.getInstance();

    public Optional<MaterialReference> get(final UUID materialId) {

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = jdbcConnectionProvider.getViewStoreConnection("material");
            preparedStatement = connection.prepareStatement(SQL);
            preparedStatement.setObject(1, materialId);

            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                final String alfrescoId = resultSet.getString("alfresco_id");
                final String filename = resultSet.getString("filename");
                final String mimeType = resultSet.getString("mime_type");
                final String externalLink = resultSet.getString("external_link");
                final Timestamp dateMaterialAdded = resultSet.getTimestamp("date_material_added");

                return Optional.of(new MaterialReference(
                        materialId,
                        alfrescoId,
                        filename,
                        mimeType,
                        externalLink,
                        fromSqlTimestamp(dateMaterialAdded)
                ));
            }
        } catch (final SQLException e) {
            throw new AssertionError("Failed to get material details using material id " + materialId, e);
        } finally {
            close(connection);
            close(preparedStatement);
            close(resultSet);
        }

        return empty();
    }

    private void close(final AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final Exception ignored) {
            }
        }
    }
}
