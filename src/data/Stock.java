package data;

import utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;

import static utils.Utils.calendarBuilder;
import static utils.Utils.stringToDate;

public class Stock {
    private String name;
    private String symbol;
    private ArrayList<Day> days;
    private Calendar startDate;

    public Stock(String name, String symbol, String filename) {
        this.name = name;
        this.symbol = symbol;
        this.days = new ArrayList<>();
        startDate = calendarBuilder(3000, 0, 0, 0);

        // CSV structure - Date, Open, High, Low, Close, Volume
        File file = new File(filename);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] numbers = line.split(",");
                Day day;
                try {
                    day = new Day(stringToDate(numbers[0]), Float.parseFloat(numbers[1]), Float.parseFloat(numbers[4]), Float.parseFloat(numbers[3]), Float.parseFloat(numbers[2]), Integer.parseInt(numbers[5]));
                }
                catch (Exception e) {
                    continue;
                }
                days.add(day);
                if (day.getDate().compareTo(startDate) < 0) {
                    startDate = day.getDate();
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    // Get
    public String getName() { return name; }
    public ArrayList<Day> getDays() { return days; }
    public Calendar getStartDate() { return startDate; }
    public String getSymbol() { return symbol; }

    public Float getPrice(Calendar date) throws Exception {
        for (Day d: days) {
            if (d.getDate().get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)) {
                return d.getPrice(date);
            }
        }

        throw new Exception("Date not found");
    }
}
