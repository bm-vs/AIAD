package data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import static utils.Utils.calendarBuilder;

public class Market {
    private int openTime;
    private int closeTime;
    private ArrayList<Stock> stocks;
    private Calendar startDate;

    public Market(int openTime, int closeTime) {
        this.openTime = openTime;
        this.closeTime = closeTime;

        stocks = new ArrayList<>();
        stocks.add(new Stock("Apple", "aapl", "resources/aapl.csv"));
        stocks.add(new Stock("AMD", "amd", "resources/amd.csv"));
        stocks.add(new Stock("Amazon", "amzn", "resources/amzn.csv"));
        stocks.add(new Stock("Facebook", "fb", "resources/fb.csv"));
        stocks.add(new Stock("Google", "goog", "resources/goog.csv"));
        stocks.add(new Stock("Microsoft", "msft", "resources/msft.csv"));
        stocks.add(new Stock("Nvidia", "nvda", "resources/nvda.csv"));
        stocks.add(new Stock("Tesla", "tsla", "resources/tsla.csv"));

        setStartDate();
    }

    // Get
    public ArrayList<Stock> getStocks() { return stocks; }
    public int getCloseTime() { return closeTime; }
    public int getOpenTime() { return openTime; }
    public Calendar getStartDate() {
        startDate.set(Calendar.HOUR_OF_DAY, openTime-1);
        return startDate;
    }

    // Get prices of all stocks at a given time
    // @param: Calendar with year, month, day and hour
    public HashMap<String, Float> getPrices(Calendar date) throws Exception {
        HashMap<String, Float> prices = new HashMap<>();
        for (Stock s: stocks) {
            prices.put(s.getSymbol(), s.getPrice(date));
        }
        return prices;
    }

    // Set start date to day of first stock info
    private void setStartDate() {
        startDate = calendarBuilder(3000, 0, 0, 0);
        for (int i = 0; i < stocks.size(); i++) {
            if (stocks.get(i).getStartDate().compareTo(startDate) < 0) {
                startDate = stocks.get(i).getStartDate();
            }
        }
    }
}
