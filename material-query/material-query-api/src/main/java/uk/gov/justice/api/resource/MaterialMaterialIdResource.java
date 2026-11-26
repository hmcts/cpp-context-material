
package uk.gov.justice.api.resource;

import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

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
