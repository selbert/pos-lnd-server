package ch.puzzle.ek.invoice.boundary;

import ch.puzzle.ek.invoice.entity.Product;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

import javax.annotation.PostConstruct;
import javax.websocket.server.PathParam;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("products")
public class ProductsResource {

    List<Product> products;

    @PostConstruct
    public void init() {
        products = Stream.of(
                new Product("Apple", "Fruits", BigDecimal.valueOf(1.2D)),
                new Product("Pear", "Fruits", BigDecimal.valueOf(2.3D)),
                new Product("Coffee", "Drugs", BigDecimal.valueOf(1.2D))
        ).collect(Collectors.toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(@RequestBody Product product, @Context UriInfo uriInfo) {
        products.add(product);

        UriBuilder builder = uriInfo.getAbsolutePathBuilder();
        builder.path(Integer.toString(products.size()-1));
        return Response.created(builder.build()).build();
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Product> getAll() {
        return products;
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Product> getByCategory(@QueryParam("category") String category) {
        return products.stream()
                .filter(p -> Objects.equals(p.category, category))
                .collect(Collectors.toList());
    }

    @GET
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Product getById(@PathParam("id") int id) {
        return products.get(id);
    }
}
