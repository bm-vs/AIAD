package data;

import java.io.Serializable;

public class Transaction implements Serializable {
    private String stockSymbol;
    private float buyPrice;
    private int quantity;
    private float sellPrice;
    private boolean done;

    public Transaction(String stockSymbol) {
        this.stockSymbol = stockSymbol;
        this.done = false;
    }

    public Transaction(String stockSymbol, float buyPrice, int quantity) {
        this.stockSymbol = stockSymbol;
        this.buyPrice = buyPrice;
        this.quantity = quantity;
        this.done = false;
    }

    public Transaction(String stockSymbol, float buyPrice, float askingPrice) {
        this.stockSymbol = stockSymbol;
        this.buyPrice = buyPrice;
        this.quantity = 0;
        this.done = false;
    }

    // Get
    public String getStock() {
        return stockSymbol;
    }

    public float getBuyPrice() {
        return buyPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public float getSellPrice() {
        return sellPrice;
    }

    // Set
    public void setBuyPrice(float buyPrice) {
        this.buyPrice = buyPrice;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setSellPrice(float sellPrice) {
        this.sellPrice = sellPrice;
    }

    public void closeTransaction() {
        this.done = true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof String) {
            return this.stockSymbol.equals(obj);
        }
        else if (obj instanceof Transaction){
            return this.stockSymbol.equals(((Transaction) obj).getStock());
        }

        return false;
    }
}
