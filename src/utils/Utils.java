package utils;

import java.util.Calendar;

public final class Utils {
    public static final int OPEN_TIME = 9;
    public static final int CLOSE_TIME = 16;

    public static Calendar calendarBuilder(int year, int month, int day, int hour) {
        return new Calendar.Builder().setDate(year, month, day).setTimeOfDay(hour, 0, 0).build();
    }

    public static Calendar stringToDate(String s) throws Exception{
        String[] date = s.replaceAll("\uFEFF", "").split("-");
        int month;
        switch(date[1]) {
            case "Jan": month = 1;  break;
            case "Feb": month = 2;  break;
            case "Mar": month = 3;  break;
            case "Apr": month = 4;  break;
            case "May": month = 5;  break;
            case "Jun": month = 6;  break;
            case "Jul": month = 7;  break;
            case "Aug": month = 8;  break;
            case "Sep": month = 9;  break;
            case "Oct": month = 10; break;
            case "Nov": month = 11; break;
            case "Dec": month = 12; break;
            default: throw new Exception("Invalid month");
        }

        return new Calendar.Builder().setDate(Integer.parseInt(date[2]), month, Integer.parseInt(date[0])).build();
    }
}
