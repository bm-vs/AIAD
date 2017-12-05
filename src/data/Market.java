package data;

import utils.StockPrice;

import java.util.ArrayList;
import java.util.Calendar;

import static utils.MarketSettings.BASIC_SECTOR_COUNT;
import static utils.Utils.calendarBuilder;
import static utils.MarketSettings.getStockBasicList;

public class Market {
    private int openTime;
    private int closeTime;
    private ArrayList<Stock> stocks;
    private Calendar startDate;
    private int nSectors;

    public Market(int openTime, int closeTime) {
        this.openTime = openTime;
        this.closeTime = closeTime;
        stocks = getStockBasicList();
        nSectors = BASIC_SECTOR_COUNT;
        setStartDate();
    }

    // Get
    public ArrayList<Stock> getStocks() {
        return stocks;
    }

    public int getCloseTime() {
        return closeTime;
    }

    public int getOpenTime() {
        return openTime;
    }

    public int getNSectors() {
        return nSectors;
    }

    public Stock getStock(String symbol) {
        for (Stock stock: stocks) {
            if (symbol.equals(stock.getSymbol())) {
                return stock;
            }
        }

        return null;
    }

    public Calendar getStartDate() {
        startDate.set(Calendar.HOUR_OF_DAY, openTime-1);
        return startDate;
    }

    // Get current and future prices of all stocks at a given time
    // @param: Calendar with year, month, day and hour
    // @returns: hashmap{stock symbol, stockprice}
    public ArrayList<StockPrice> getPrices(Calendar date) throws Exception {
        ArrayList<StockPrice> prices = new ArrayList<>();

        Calendar nextHour = (Calendar) date.clone();
        nextHour.add(Calendar.HOUR_OF_DAY, 1);
        Calendar nextDay = (Calendar) date.clone();
        nextDay.add(Calendar.DAY_OF_YEAR, 1);
        Calendar nextWeek = (Calendar) date.clone();
        nextWeek.add(Calendar.WEEK_OF_YEAR, 1);
        Calendar nextMonth = (Calendar) date.clone();
        nextMonth.add(Calendar.MONTH, 1);

        for (Stock s: stocks) {
            prices.add(new StockPrice(s.getSymbol(), s.getSector(), s.getPrice(date), getFuturePrice(nextHour, s), getFuturePrice(nextDay, s), getFuturePrice(nextWeek, s), getFuturePrice(nextMonth, s)));
        }

        return prices;
    }

    // Set start date to day of first stock info
    private void setStartDate() {
        startDate = calendarBuilder(3000, 0, 0, 0);
        for (Stock s : stocks) {
            if (s.getStartDate().compareTo(startDate) < 0) {
                startDate = s.getStartDate();
            }
        }
    }

    // Get future price (ignore market closes)
    private float getFuturePrice(Calendar date, Stock s) {
        Calendar nextDate = (Calendar) date.clone();
        boolean failed = true;
        int timeout = 0;
        float price = 0;
        while (failed && timeout < 5) {
            try {
                price = s.getPrice(nextDate);
                failed = false;
            }
            catch (Exception e) {
                nextDate.add(Calendar.DAY_OF_YEAR, 1);
                timeout++;
                failed = true;
            }
        }
        return price;
    }
}
