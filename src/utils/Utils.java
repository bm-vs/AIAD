package utils;

import data.Stock;

import java.util.ArrayList;
import java.util.Calendar;

public final class Utils {
    // Creates a Calendar object from the given {year, month, day, hour}
    public static Calendar calendarBuilder(int year, int month, int day, int hour) {
        return new Calendar.Builder().setDate(year, month, day).setTimeOfDay(hour, 0, 0).build();
    }

    // Creates a Calendar object from the given string
    public static Calendar stringToDate(String s) throws Exception{
        String[] date = s.replaceAll("\uFEFF", "").split("-");
        int month;
        switch(date[1]) {
            case "Jan": month = 0;  break;
            case "Feb": month = 1;  break;
            case "Mar": month = 2;  break;
            case "Apr": month = 3;  break;
            case "May": month = 4;  break;
            case "Jun": month = 5;  break;
            case "Jul": month = 6;  break;
            case "Aug": month = 7;  break;
            case "Sep": month = 8;  break;
            case "Oct": month = 9; break;
            case "Nov": month = 10; break;
            case "Dec": month = 11; break;
            default: throw new Exception("Invalid month");
        }

        return new Calendar.Builder().setDate(2000+Integer.parseInt(date[2]), month, Integer.parseInt(date[0])).build();
    }

    // Market times
    public static final int OPEN_TIME = 9;
    public static final int CLOSE_TIME = 16;

    // Market sectors
    public static final int TELECOM = 1;
    public static final int FINANCIAL = 2;
    public static final int INDUSTRIAL = 3;
    public static final int ENERGY = 4;
    public static final int HEALTHCARE = 5;
    public static final int TECH = 6;

    // Stock lists to be used for a market

    // Basic - top 5 market cap for each sector
    public static ArrayList<Stock> getStockBasicList() {
        ArrayList<Stock> stocks = new ArrayList<>();

        // Telecom
        stocks.add(new Stock("AT&T",     "T",    "resources/telecom/aapl.csv"));
        stocks.add(new Stock("Verizon",  "VZ",   "resources/telecom/amd.csv"));
        stocks.add(new Stock("Cisco",    "CSCO", "resources/telecom/amzn.csv"));
        stocks.add(new Stock("T-Mobile", "TMUS", "resources/telecom/fb.csv"));
        stocks.add(new Stock("Sprint",   "S",    "resources/telecom/goog.csv"));

        // Financial
        stocks.add(new Stock("Berkshire",       "BRK.B", "resources/financial/brk.b.csv"));
        stocks.add(new Stock("JP Morgan",       "JPM",   "resources/financial/jpm.csv"));
        stocks.add(new Stock("Bank of America", "BAC",   "resources/financial/bac.csv"));
        stocks.add(new Stock("Wells Fargo",     "WFC",   "resources/financial/wfc.csv"));
        stocks.add(new Stock("Citigroup",       "C",     "resources/financial/c.csv"));

        // Industrial
        stocks.add(new Stock("Boeing",           "BA",  "resources/industrial/ba.csv"));
        stocks.add(new Stock("General Electric", "GE",  "resources/industrial/ge.csv"));
        stocks.add(new Stock("3M Company",       "MMM", "resources/industrial/mmm.csv"));
        stocks.add(new Stock("Honeywell",        "HON", "resources/industrial/hon.csv"));
        stocks.add(new Stock("UPS",              "UPS", "resources/industrial/ups.csv"));

        // Energy



        // Healthcare



        // Tech

        stocks.add(new Stock("Apple",     "aapl", "resources/aapl.csv"));
        stocks.add(new Stock("AMD",       "amd",  "resources/amd.csv"));
        stocks.add(new Stock("Amazon",    "amzn", "resources/amzn.csv"));
        stocks.add(new Stock("Facebook",  "fb",   "resources/fb.csv"));
        stocks.add(new Stock("Google",    "goog", "resources/goog.csv"));
        stocks.add(new Stock("Microsoft", "msft", "resources/msft.csv"));
        stocks.add(new Stock("Nvidia",    "nvda", "resources/nvda.csv"));
        stocks.add(new Stock("Tesla",     "tsla", "resources/tsla.csv"));



    }
}
