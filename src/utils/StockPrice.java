package utils;

import java.io.Serializable;

// Class used to pass stock price info from informer to investor
public class StockPrice implements Serializable {
    private String symbol;
    private float currPrice;  // price at the current time
    private float hourPrice;  // price next hour
    private float dayPrice;   // price next day
    private float weekPrice;  // price next week
    private float monthPrice; // price next month

    public StockPrice(String symbol, float currPrice, float hourPrice, float dayPrice, float weekPrice, float monthPrice) {
        this.symbol = symbol;
        this.currPrice = currPrice;
        this.hourPrice = hourPrice;
        this.dayPrice = dayPrice;
        this.weekPrice = weekPrice;
        this.monthPrice = monthPrice;
    }

    public String getSymbol() {
        return symbol;
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
}
