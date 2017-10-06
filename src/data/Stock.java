package data;

import utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;

import static utils.Utils.stringToDate;

public class Stock {
    private String name;
    private String symbol;
    private ArrayList<Day> days;

    public Stock(String filename) {
        this.days = new ArrayList<>();
        this.readStockInfo(filename);
    }

    // Get
    public String getName() { return name; }
    public String getSymbol() { return symbol; }
    public ArrayList<Day> getDays() { return days; }
    public Float getPrice(Calendar date) throws Exception{
        for (Day d: days) {
            if (d.getDate().get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)) {
                return d.getPrice(date);
            }
        }

        throw new Exception("Date not found");
    }

    // Reads stock info from file
    // CSV structure:
    // - 1st line - Name, Symbol
    // - Date, Open, High, Low, Close, Volume
    private void readStockInfo(String filename) {
        File file = new File(filename);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));

            // 1st line (Name, Symbol)
            String line = reader.readLine();
            String[] id = line.split(",");
            this.name = id[0].trim();
            this.symbol = id[1].trim();

            // Rest of file (Date, Open, High, Low, Close, Volume)
            while ((line = reader.readLine()) != null) {
                String[] numbers = line.split(",");
                Day day;
                try {
                    Calendar date = stringToDate(numbers[0].trim());
                    float open = Float.parseFloat(numbers[1].trim());
                    float high = Float.parseFloat(numbers[2].trim());
                    float low = Float.parseFloat(numbers[3].trim());
                    float close = Float.parseFloat(numbers[4].trim());
                    int volume = Integer.parseInt(numbers[5].trim());
                    day = new Day(date, open, high, low, close, volume);
                }
                catch (Exception e) {
                    continue;
                }
                days.add(day);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
