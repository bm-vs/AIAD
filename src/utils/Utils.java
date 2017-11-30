package utils;

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
}
