package data;

import utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

public class Day {
    private Calendar date;
    private float open;
    private float close;
    private float low;
    private float high;
    private int volume;
    private ArrayList<Float> hourly_stock;

    public Day(Calendar date, float open, float high, float low, float close, int volume) {
        this.date = date;
        this.open = open;
        this.close = close;
        this.low = low;
        this.high = high;
        this.volume = volume;
        this.hourly_stock = new ArrayList<>();
        this.createHourlyStock();
    }

    // Get
    public Calendar getDate() { return date; }
    public float getOpen() { return open; }
    public float getClose() { return close; }
    public float getLow() { return low; }
    public float getHigh() { return high; }
    public int getVolume() { return volume; }
    public float getPrice(Calendar date) throws Exception {
        if (date.get(Calendar.HOUR_OF_DAY) < Utils.OPEN_TIME || date.get(Calendar.HOUR_OF_DAY) > Utils.CLOSE_TIME) {
            throw new Exception("Market closed");
        }
        else {
            return hourly_stock.get(date.get(Calendar.HOUR_OF_DAY)-Utils.OPEN_TIME);
        }
    }

    // Set
    public void setDate(Calendar date) { this.date = date; }
    public void setOpen(float open) { this.open = open; }
    public void setClose(float close) { this.close = close; }
    public void setLow(float low) { this.low = low; }
    public void setHigh(float high) { this.high = high; }
    public void setVolume(int volume) { this.volume = volume; }

    // Randomly create hourly stock prices
    public void createHourlyStock() {
        Random r = new Random();
        for (int hour = Utils.OPEN_TIME; hour <= Utils.CLOSE_TIME; hour++) {
            float linear_price = (hour-Utils.OPEN_TIME)*(this.close-this.open)/(Utils.CLOSE_TIME-Utils.OPEN_TIME) + this.open;
            float variance = (float) r.nextGaussian()/3;

            if (hour == Utils.OPEN_TIME || hour == Utils.CLOSE_TIME) {
                variance = 0;
            }
            else if (this.high-linear_price > linear_price-this.low) {
                variance *= linear_price-this.low;
            }
            else {
                variance *= this.high-linear_price;
            }

            this.hourly_stock.add(linear_price+variance);
        }
    }
}
