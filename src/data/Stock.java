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
    private ArrayList<Day> days;

    public Stock(String name, String filename) {
        this.name = name;
        this.days = new ArrayList<>();

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
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    // Get
    public String getName() { return name; }
    public ArrayList<Day> getDays() { return days; }

    public Float getPrice(Calendar date) throws Exception{
        for (Day d: days) {
            if (d.getDate().get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)) {
                return d.getPrice(date);
            }
        }

        throw new Exception("Date not found");
    }
}
