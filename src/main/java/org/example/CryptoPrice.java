package org.example;

public class CryptoPrice {
    private String cryptoName;
    private double price;

    public CryptoPrice(String cryptoName, double price) {
        this.cryptoName = cryptoName;
        this.price = price;
    }

    public String getCryptoName() {
        return cryptoName;
    }

    public double getPrice() {
        return price;
    }
}