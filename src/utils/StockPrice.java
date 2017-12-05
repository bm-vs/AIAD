package utils;

import java.io.Serializable;
import java.util.Comparator;

// Class used to pass stock price info from informer to investor
public class StockPrice implements Serializable {
    private String symbol;
    private int sector;
    private float currPrice;  // price at the current time
    private float hourPrice;  // price next hour
    private float dayPrice;   // price next day
    private float weekPrice;  // price next week
    private float monthPrice; // price next month
    private float estimatedPrice; // price prediction to be used by investorAgents

    public StockPrice(String symbol, int sector, float currPrice, float hourPrice, float dayPrice, float weekPrice, float monthPrice) {
        this.symbol = symbol;
        this.sector = sector;
        this.currPrice = currPrice;
        this.hourPrice = hourPrice;
        this.dayPrice = dayPrice;
        this.weekPrice = weekPrice;
        this.monthPrice = monthPrice;
        this.estimatedPrice = 0;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getSector() {
        return sector;
    }

    public float getCurrPrice() {
        return currPrice;
    }

    public float getHourPrice() {
        return hourPrice;
    }

    public float getDayPrice() {
        return dayPrice;
    }

    public float getWeekPrice() {
        return weekPrice;
    }

    public float getMonthPrice() {
        return monthPrice;
    }

    public float getEstimatedPrice() {
        return estimatedPrice;
    }

    public void setEstimatedPrice(float estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
    }

    @Override
    public String toString() {
        Float growth = (estimatedPrice-currPrice)/currPrice;
        return growth.toString();
    }

    // Growth from lowest to highest
    public static class StockPriceComparator implements Comparator {
        @Override
        public int compare(Object o1, Object o2) {
            if (!(o1 instanceof StockPrice) || !(o2 instanceof StockPrice)) {
                throw new ClassCastException();
            }

            StockPrice s1 = (StockPrice) o1;
            StockPrice s2 = (StockPrice) o2;

            float s1Growth = (s1.getEstimatedPrice()-s1.getCurrPrice())/s1.getCurrPrice();
            float s2Growth = (s2.getEstimatedPrice()-s2.getCurrPrice())/s2.getCurrPrice();

            return Float.compare(s1Growth, s2Growth);
        }
    }
}
