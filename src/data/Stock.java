package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;

import utils.DateNotFoundException;
import static utils.Utils.calendarBuilder;
import static utils.Utils.stringToDate;

public class Stock {
    private String name;
    private String symbol;
    private ArrayList<Day> days;
    private Calendar startDate;
    private int sector;

    public Stock(String name, String symbol, String filename, int sector) {
        this.name = name;
        this.symbol = symbol;
        this.days = new ArrayList<>();
        this.startDate = calendarBuilder(3000, 0, 0, 0);
        this.sector = sector;
        readDaysFromCSV(filename);
    }

    // Get
    public String getName() {
        return name;
    }

    public ArrayList<Day> getDays() {
        return days;
    }

    public Calendar getStartDate() {
        return startDate;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getSector() {
        return sector;
    }

    public Float getPrice(Calendar date) throws DateNotFoundException {
        for (Day d: days) {
            if (d.getDate().get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)) {
                return d.getPrice(date);
            }
        }

        throw new DateNotFoundException();
    }

    // Read each day from CSV file, update start date
    // CSV structure - Date, Open, High, Low, Close, Volume
    private void readDaysFromCSV(String filename) {
        File file = new File(filename);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] numbers = line.split(",");
                try {
                    Day day = new Day(stringToDate(numbers[0]), Float.parseFloat(numbers[1]), Float.parseFloat(numbers[4]), Float.parseFloat(numbers[3]), Float.parseFloat(numbers[2]), Integer.parseInt(numbers[5]));
                    days.add(day);
                    if (day.getDate().compareTo(startDate) < 0) {
                        startDate = day.getDate();
                    }
                }
                catch (Exception e) {}
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
