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
            prices.add(new StockPrice(s.getSymbol(), s.getPrice(date), s.getPrice(nextHour), s.getPrice(nextDay), s.getPrice(nextWeek), s.getPrice(nextMonth)));
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
}
