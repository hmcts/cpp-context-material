
package uk.gov.justice.api.resource;

import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("material/{materialId}")
public interface MaterialMaterialIdResource {

    // the last part of the url will be used as the filename when downloading from the browser
    @Path("{filename}")
    @GET
    @Produces({
            "application/vnd.material.query.material+json"
    })
    Response getMaterialByMaterialId(
            @PathParam("materialId") String materialId,
            @PathParam("filename") String filename,
            @HeaderParam(USER_ID) String userId);

    @GET
    @Produces({
            "application/vnd.material.query.material+json"
    })
    Response getMaterialByMaterialId(
            @PathParam("materialId") String materialId,
            @DefaultValue("false") @QueryParam("requestPdf") boolean requestPdf,
            @HeaderParam(USER_ID) String userId);
}
