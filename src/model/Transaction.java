package model;

public class Transaction {
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

    public String getStock() { return stockSymbol; }
    public double getBuyPrice() {
        return buyPrice;
    }
    public int getQuantity() {
        return quantity;
    }
    public double getSellPrice() {
        return sellPrice;
    }

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
}
