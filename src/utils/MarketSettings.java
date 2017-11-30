package utils;

import data.Stock;

import java.util.ArrayList;

public final class MarketSettings {
    // Market times
    public static final int OPEN_TIME = 9;
    public static final int CLOSE_TIME = 16;

    // Market sectors
    public static final int TELECOM = 0;
    public static final int FINANCIAL = 1;
    public static final int INDUSTRIAL = 2;
    public static final int ENERGY = 3;
    public static final int HEALTHCARE = 4;
    public static final int TECH = 5;

    // Stock lists to be used for a market
    // Basic - top 5 market cap for each sector from S&P 500
    public static final int BASIC_SECTOR_COUNT = 6;
    public static ArrayList<Stock> getStockBasicList() {
        ArrayList<Stock> stocks = new ArrayList<>(6);

        // Telecom
        stocks.add(new Stock("AT&T",     "T",    "resources/telecom/t.csv", TELECOM));
        stocks.add(new Stock("Verizon",  "VZ",   "resources/telecom/vz.csv",  TELECOM));
        stocks.add(new Stock("Cisco",    "CSCO", "resources/telecom/csco.csv", TELECOM));
        stocks.add(new Stock("T-Mobile", "TMUS", "resources/telecom/tmus.csv",   TELECOM));
        stocks.add(new Stock("Sprint",   "S",    "resources/telecom/s.csv", TELECOM));

        // Financial
        stocks.add(new Stock("Berkshire",       "BRK.B", "resources/financial/brk.b.csv", FINANCIAL));
        stocks.add(new Stock("JP Morgan",       "JPM",   "resources/financial/jpm.csv",   FINANCIAL));
        stocks.add(new Stock("Bank of America", "BAC",   "resources/financial/bac.csv",   FINANCIAL));
        stocks.add(new Stock("Wells Fargo",     "WFC",   "resources/financial/wfc.csv",   FINANCIAL));
        stocks.add(new Stock("Citigroup",       "C",     "resources/financial/c.csv",     FINANCIAL));

        // Industrial
        stocks.add(new Stock("Boeing",           "BA",  "resources/industrial/ba.csv",  INDUSTRIAL));
        stocks.add(new Stock("General Electric", "GE",  "resources/industrial/ge.csv",  INDUSTRIAL));
        stocks.add(new Stock("3M Company",       "MMM", "resources/industrial/mmm.csv", INDUSTRIAL));
        stocks.add(new Stock("Honeywell",        "HON", "resources/industrial/hon.csv", INDUSTRIAL));
        stocks.add(new Stock("UPS",              "UPS", "resources/industrial/ups.csv", INDUSTRIAL));

        // Energy
        stocks.add(new Stock("Exxon",          "XOM", "resources/energy/xom.csv", ENERGY));
        stocks.add(new Stock("Chevron",        "CVX", "resources/energy/cvx.csv", ENERGY));
        stocks.add(new Stock("Schlumberger",   "SLB", "resources/energy/slb.csv", ENERGY));
        stocks.add(new Stock("Conocophillips", "COP", "resources/energy/cop.csv", ENERGY));
        stocks.add(new Stock("Eog",            "EOG", "resources/energy/eog.csv", ENERGY));

        // Healthcare
        stocks.add(new Stock("Johnson & Johnson", "JNJ",  "resources/healthcare/jnj.csv",  HEALTHCARE));
        stocks.add(new Stock("Unitedhealth",      "UNH",  "resources/healthcare/unh.csv",  HEALTHCARE));
        stocks.add(new Stock("Pfizer",            "PFE",  "resources/healthcare/pfe.csv",  HEALTHCARE));
        stocks.add(new Stock("Abbvie",            "ABBV", "resources/healthcare/abbv.csv", HEALTHCARE));
        stocks.add(new Stock("Merck",             "MRK",  "resources/healthcare/mrk.csv",  HEALTHCARE));

        // Tech
        stocks.add(new Stock("Apple",     "AAPL", "resources/tech/aapl.csv", TECH));
        stocks.add(new Stock("Alphabet",  "GOOG", "resources/tech/goog.csv", TECH));
        stocks.add(new Stock("Microsoft", "MSFT", "resources/tech/msft.csv", TECH));
        stocks.add(new Stock("Facebook",  "FB",   "resources/tech/fb.csv",   TECH));
        stocks.add(new Stock("Visa",      "V",    "resources/tech/v.csv",    TECH));

        return stocks;
    }
}
