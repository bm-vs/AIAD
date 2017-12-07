package model.onto;

import jade.content.Predicate;

import java.util.HashMap;

public class MarketPrices implements Predicate {
    private HashMap<String, StockPrice> prices;

    public MarketPrices(HashMap<String, StockPrice> prices) {
        this.prices = prices;
    }

    public HashMap<String, StockPrice> getPrices() {
        return prices;
    }

    public void setPrices(HashMap<String, StockPrice> prices) {
        this.prices = prices;
    }
}
