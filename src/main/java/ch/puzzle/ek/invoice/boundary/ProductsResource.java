package ch.puzzle.ek.invoice.boundary;

import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

@Path("/product")
public class ProductsResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public JsonObject buy(@RequestBody JsonArray products) {
        return null;
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    public JsonArray get() {
        return null;
    }
}
