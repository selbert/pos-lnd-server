package ch.puzzle.ek.bitcoin.control;

import ch.puzzle.ek.bitcoin.entity.BitcoinErrorException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

import javax.annotation.PostConstruct;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Stateless
public class BitcoinService {


    @Inject
    @ConfigProperty(name = "CURRENCY_TICKER", defaultValue = "CHF")
    String currencyTicker;


    @Inject
    @ConfigProperty(name = "BITCOIN_REST_URL", defaultValue = "https://rest.lightning-test.puzzle.ch/rest/chaininfo.json")
    String restUrl;


    @Inject
    @ConfigProperty(name = "BITCOIN_PRICE_API_PRIMARY", defaultValue = "https://blockchain.info/ticker")
    String primaryBitcoinPriceUrl;

    @Inject
    @ConfigProperty(name = "BITCOIN_PRICE_API_SECONDARY", defaultValue = "https://public-api.lykke.com/api/AssetPairs/rate/BTCCHF")
    String secondaryBitcoinPriceUrl;

    private static final Long SATOSHIS_IN_BTC = 100000000L;
    public static final String FIELD_BUY = "buy";
    private static final String FIELD_BID = "bid";

    private final Map<String, AtomicReference<BigDecimal>> lastBitcoinBuyPriceMap = new ConcurrentHashMap<String, AtomicReference<BigDecimal>>();
    private final Map<String, LocalDateTime> lastBitcoinPriceUpdateMap = new ConcurrentHashMap<>();


    @PostConstruct
    public void init() {
            updateBitcoinPrice(currencyTicker);
    }


    public JsonObject chainInfo() {
        return getJsonFrom(restUrl);
    }

    public BigDecimal buyPricePerBitcoinIn(String ticker) {
        return Optional.ofNullable(lastBitcoinBuyPriceMap.get(ticker))
            .map(AtomicReference::get)
            .orElseGet(() -> updateBitcoinPrice(ticker));
    }

    public List<String> getStalePrices() {
        return lastBitcoinPriceUpdateMap.entrySet().stream()
            .filter(lastUpdate -> lastUpdate.getValue().isBefore(LocalDateTime.now().minusMinutes(10L)))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }


    public Map<String, Long> getPricesAge() {
        return lastBitcoinPriceUpdateMap.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> Duration.between(e.getValue(), LocalDateTime.now()).toMillis()));
    }

    private JsonObject pricePerBitcoinIn() {
        return primaryTickerPrice();
    }

    @Retry
    @Fallback(fallbackMethod = "secondaryTickerPrice")
    public JsonObject primaryTickerPrice() {
        return getJsonFrom(primaryBitcoinPriceUrl).getJsonObject(currencyTicker);
    }

    public JsonObject secondaryTickerPrice()  {
        JsonObject responseObject =  getJsonFrom(secondaryBitcoinPriceUrl);

        return Json.createObjectBuilder(responseObject)
                .add(FIELD_BUY, responseObject.getJsonNumber(FIELD_BID))
                .build();
    }

    private JsonObject getJsonFrom(String bitcoinPriceApiUrl) {
        System.out.println("Looking up price at " + bitcoinPriceApiUrl);
        Client httpClient = ClientBuilder.newBuilder()
                .build();
        WebTarget target = httpClient.target(bitcoinPriceApiUrl);
        Response response = target
                .request("")
                .accept(MediaType.APPLICATION_JSON)
                .get();
        return response.readEntity(JsonObject.class);
    }

    @Schedule(hour = "*", minute = "*", persistent = false)
    private void updateBitcoinPrices() {
        lastBitcoinBuyPriceMap.forEach((ticker, price) -> updateBitcoinPrice(ticker));
    }

    private BigDecimal updateBitcoinPrice(String currencyTicker) {
        Optional<BigDecimal> priceOptional = Optional.empty();
        try {
            BigDecimal price = fetchBuyPricePerBitcoinIn();
            updateBitcoinPriceCache(currencyTicker, price);
            priceOptional = Optional.of(price);
        } catch (IOException | InterruptedException e) {
            System.err.println("unable to update price");
            e.printStackTrace();
        }
        return priceOptional.orElseThrow(() -> new BitcoinErrorException("Unable to retrieve bitcoin price"));
    }

    private BigDecimal fetchBuyPricePerBitcoinIn() throws IOException, InterruptedException {
        JsonObject price = pricePerBitcoinIn();
        if (price == null || !price.containsKey(FIELD_BUY)) {
            throw new BitcoinErrorException("could not read bitcoin price from ticker API");
        }
        try {
            return price.getJsonNumber(FIELD_BUY).bigDecimalValue();
        } catch (NumberFormatException e) {
            throw new BitcoinErrorException("could not parse bitcoin price from ticker API");
        }
    }

    private void updateBitcoinPriceCache(String currencyTicker, BigDecimal price) {
        lastBitcoinBuyPriceMap.putIfAbsent(currencyTicker, new AtomicReference<>());
        lastBitcoinBuyPriceMap.get(currencyTicker)
            .set(price);
        lastBitcoinPriceUpdateMap
            .put(currencyTicker, LocalDateTime.now());
    }
}
