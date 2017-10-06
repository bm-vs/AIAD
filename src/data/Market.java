package data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;

import static utils.Utils.calendarBuilder;

public class Market {
    private ArrayList<Stock> stocks;

    public Market() {
        stocks = new ArrayList<>();
        ArrayList<String> stock_list = new ArrayList<String>(Arrays.asList("aapl", "amd", "amzn", "fb", "goog", "msft", "nvda", "tsla"));
        for (String n: stock_list) {
            stocks.add(new Stock("resources/" + n + ".csv"));
        }
    }

    // Get
    public ArrayList<Stock> getStocks() { return stocks; }

    // Get prices of all stocks at a given time
    // @param: date (year, month, day, hour)
    public HashMap<String, Float> getAllPrices(Calendar date) {
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

    // Get price of a single stock at a given time
    // @param: symbol/name, date (year, month, day, hour)
    public Float getSinglePrice(String stock, Calendar date) throws Exception{
        for (Stock s: this.stocks) {
            if (s.getName().equals(stock) || s.getSymbol().equals(stock)) {
                return s.getPrice(date);
            }
        }

        throw new Exception("Stock not found");
    }



    public static void main(String[] args) {

        // Get progression of all stock prices during a day
        Market nasdaq = new Market();
        ArrayList<HashMap<String, Float>> day = new ArrayList<>();
        for (int i = 9; i < 17; i++) {
            day.add(nasdaq.getAllPrices(calendarBuilder(2016, 11, 10, i)));
        }
        for (String s: day.get(0).keySet()) {
            System.out.print(s + " - ");
            for (int i = 0; i < day.size(); i++) {
                System.out.print(day.get(i).get(s) + ", ");
            }
            System.out.println();
        }

        // Get price of a single stock at a given time
        Market nyse = new Market();
        try {
            Float price = nyse.getSinglePrice("aapl", calendarBuilder(2016, 12, 30, 13));
            System.out.print(price);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

}
