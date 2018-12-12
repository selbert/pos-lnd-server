package ch.puzzle.ek.invoice.entity;

import java.math.BigDecimal;
import java.math.BigInteger;

public class Product {
    public String name;
    public BigDecimal price;
    public String category;

    public Product(String name, String category, BigDecimal price) {
        this.name = name;
        this.category = category;
        this.price = price;
    }
}
