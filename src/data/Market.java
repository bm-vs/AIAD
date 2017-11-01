package data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import static utils.Utils.calendarBuilder;

public class Market {
    private int openHour;
    private int closeHour;
    private ArrayList<Stock> stocks;
    private Calendar startDate;

    public Market() {
        openHour = 9;
        closeHour = 17;

        stocks = new ArrayList<>();
        stocks.add(new Stock("Apple",     "resources/aapl.csv"));
        stocks.add(new Stock("AMD",       "resources/amd.csv"));
        stocks.add(new Stock("Amazon",    "resources/amzn.csv"));
        stocks.add(new Stock("Facebook",  "resources/fb.csv"));
        stocks.add(new Stock("Google",    "resources/goog.csv"));
        stocks.add(new Stock("Microsoft", "resources/msft.csv"));
        stocks.add(new Stock("Nvidia",    "resources/nvda.csv"));
        stocks.add(new Stock("Tesla",     "resources/tsla.csv"));

        startDate = calendarBuilder(3000, 0, 0, 0);
        for (int i = 0; i < stocks.size(); i++) {
            if (stocks.get(i).getStartDate().compareTo(startDate) < 0) {
                startDate = stocks.get(i).getStartDate();
            }
        }
    }

    // Get
    public ArrayList<Stock> getStocks() { return stocks; }
    public int getCloseHour() { return closeHour; }
    public int getOpenHour() { return openHour; }
    public Calendar getStartDate() {
        startDate.set(Calendar.HOUR_OF_DAY, openHour-1);
        return startDate;
    }

    // Get prices of all stocks at a given time
    // @param: (year, month, day, hour)
    public HashMap<String, Float> getPrices(Calendar date) {
        HashMap<String, Float> prices = new HashMap<>();
        for (Stock s: stocks) {
            try {
                prices.put(s.getName(), s.getPrice(date));
            }
            catch (Exception e) {
                e.printStackTrace();
            }

        }
        return prices;
    }

    // Code to get progression of stock price during entire day
    public static void main(String[] args) {
        Market nasdaq = new Market();
        ArrayList<HashMap<String, Float>> day = new ArrayList<>();
        for (int i = 9; i < 17; i++) {
            day.add(nasdaq.getPrices(calendarBuilder(2016, 11, 10, i)));
        }
        for (String s: day.get(0).keySet()) {
            System.out.print(s + " - ");
            for (int i = 0; i < day.size(); i++) {
                System.out.print(day.get(i).get(s) + ", ");
            }
            System.out.println();
        }
    }

}
