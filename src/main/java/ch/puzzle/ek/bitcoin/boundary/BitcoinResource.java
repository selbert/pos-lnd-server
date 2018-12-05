package ch.puzzle.ek.bitcoin.boundary;

import ch.puzzle.ek.bitcoin.control.BitcoinService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.metrics.annotation.Timed;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static ch.puzzle.ek.bitcoin.control.BitcoinService.FIELD_BUY;

@Health
@Path("/bitcoin")
public class BitcoinResource implements HealthCheck {

    @Inject
    @ConfigProperty(name = "CURRENCY_TICKER", defaultValue = "CHF")
    String currencyTicker;

    @Inject
    private BitcoinService bitcoinService;

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getInfo() {
        return bitcoinService.chainInfo();
    }

    @Timed
    @GET
    @Path("/price/{ticker}")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getPrice(@PathParam("ticker") String ticker) {
        return Json.createObjectBuilder()
                .add(FIELD_BUY, bitcoinService.buyPricePerBitcoinIn(ticker)).build();
    }


    @GET
    @Path("/prices/stale")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getStalePrices() {
        return bitcoinService.getStalePrices();
    }

    @GET
    @Path("/prices/age")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Long> getPricesAge() {
        return bitcoinService.getPricesAge();
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder =
                HealthCheckResponse.
                        named("bitcoin");
        try {
            JsonObject info = bitcoinService.chainInfo();
            BigDecimal price = bitcoinService.buyPricePerBitcoinIn(currencyTicker);
            List<String> stalePrices = bitcoinService.getStalePrices();
            Map<String, Long> pricesAges = bitcoinService.getPricesAge();

            if (info == null || !stalePrices.isEmpty()) {
                builder.down();
            }
            builder.withData("price", price.toString())
                    .withData("stalePrices", stalePrices.toString())
                    .withData("pricesAge", pricesAges.toString())
                    .withData("blockHeight", info.getJsonNumber("blocks").toString())
                    .withData("blockHash", info.getString("bestblockhash"))
                    .up();
        } catch (Exception e) {
            builder.down();
        }
        return builder.build();
    }
}
