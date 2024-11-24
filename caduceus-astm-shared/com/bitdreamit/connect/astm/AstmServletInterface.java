package com.bitdreamit.connect.astm;

import com.mirth.connect.client.core.ClientException;
import com.mirth.connect.client.core.api.BaseServletInterface;
import com.mirth.connect.client.core.api.MirthOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/extensions/astm")
@Tag(name = "Extension Services", description = "APIs for ASTM plugin-related operations")
@Consumes({"application/xml"})
@Produces({"application/xml"})
public interface AstmServletInterface extends BaseServletInterface {
    String PLUGINPOINT = "ASTM Settings";
    String PERMISSION_STATUS = "Driver Status";
    String PERMISSION_LICENSE = "Change License";

    @GET
    @Path("/status")
    @Operation(summary = "Retrieves the ASTM driver status", description = "Returns the current status of the ASTM driver")
    @MirthOperation(
            name = "getStatusMap",
            display = "Get status",
            permission = "Driver Status"
    )
    Map<String, Object> getStatusMap() throws ClientException;

    @POST
    @Path("/license")
    @Operation(summary = "Sets a new license", description = "Sets or updates the license for the ASTM plugin")
    @MirthOperation(
            name = "setLicense",
            display = "Set license",
            permission = "Change License"
    )
    void setLicense(byte[] var1) throws ClientException;

    @DELETE
    @Path("/license")
    @Operation(summary = "Removes existing license", description = "Deletes the current license for the ASTM plugin")
    @MirthOperation(
            name = "setLicense",
            display = "Remove license",
            permission = "Change License"
    )
    void removeLicense() throws ClientException;
}
