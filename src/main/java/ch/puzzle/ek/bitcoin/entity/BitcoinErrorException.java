package ch.puzzle.ek.bitcoin.entity;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class BitcoinErrorException extends WebApplicationException {
    public BitcoinErrorException(String message) {
        super(Response.serverError().header("reason", message).build());
    }
}
