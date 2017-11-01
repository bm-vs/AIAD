package model;

import data.Stock;

public class Transaction {
    private Stock stock;
    private double buyPrice;
    private int quantity;
    private double sellPrice;
    private boolean done;

    public Transaction(Stock stock) {
        this.stock = stock;
        this.done = false;
    }

    public Transaction(Stock stock, double buyPrice, int quantity) {
        this.stock = stock;
        this.buyPrice = buyPrice;
        this.quantity = quantity;
        this.done = false;
    }

    public Stock getStock() {
        return stock;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public void closeTransaction() {
        this.done = true;
    }
}
