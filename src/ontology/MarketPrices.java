package ontology;

import jade.content.Predicate;
import java.util.ArrayList;

public class MarketPrices implements Predicate {
    private ArrayList<StockPrice> prices;

    public MarketPrices() {}

    public MarketPrices(ArrayList<StockPrice> prices) {
        this.prices = prices;
    }

    public ArrayList<StockPrice> getPrices() {
        return prices;
    }

    public void setPrices(ArrayList<StockPrice> prices) {
        this.prices = prices;
    }
}
