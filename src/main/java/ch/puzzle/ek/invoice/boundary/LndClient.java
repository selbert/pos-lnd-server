package ch.puzzle.ek.invoice.boundary;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.ejb.Stateless;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEventSource;

@Stateless
public class LndClient {

    @ConfigProperty(name = "lnd.api.hostname", defaultValue = "lnd-api")
    String lndHostname;
    @ConfigProperty(name = "lnd.api.port", defaultValue = "8080")
    String lndPort;

    @ConfigProperty(name = "lnd.api.sse.path", defaultValue = "/resources/sse/invoices")
    String lndPath;

    public void listenForInvoice(String invoiceHash) {
        WebTarget target = ClientBuilder.newClient()
                .target(lndHostname + ":" + lndPort + lndPath + "/{invoiceHash}}")
                .resolveTemplate("invoiceHash", invoiceHash)
                .queryParam("verbose", true);

        SseEventSource eventSource =
                SseEventSource.target(target)
                        .build();

        eventSource.register(object -> {
            object.readData();
            eventSource.close();
        } );
    }
}
