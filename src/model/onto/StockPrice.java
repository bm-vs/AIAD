package model.onto;

import jade.content.Predicate;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Random;

import static utils.Settings.INVESTOR_MAX_SKILL;

// Class used to pass stock price info from informer to investor
public class StockPrice implements Predicate {
    private String symbol;
    private int sector;
    private float currPrice;  // price at the current time
    private float hourPrice;  // price next hour
    private float estimatedPrice; // price prediction to be used by investorAgents

    public StockPrice(String symbol, int sector, float currPrice, float hourPrice) {
        this.symbol = symbol;
        this.sector = sector;
        this.currPrice = currPrice;
        this.hourPrice = hourPrice;
        this.estimatedPrice = 0;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getSector() {
        return sector;
    }

    public void setSector(int sector) {
        this.sector = sector;
    }

    public float getCurrPrice() {
        return currPrice;
    }

    public void setCurrPrice(float currPrice) {
        this.currPrice = currPrice;
    }

    public float getHourPrice() {
        return hourPrice;
    }

    public void setHourPrice(float hourPrice) {
        this.hourPrice = hourPrice;
    }

    public float getEstimatedPrice() {
        return estimatedPrice;
    }

    public void setEstimatedPrice(float estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
    }

    // Introduces error into hour price according to skill
    // The higher the skill the more accurate the return value is
    public void addError(int skill) {
        Random r = new Random();
        if (skill < r.nextInt(INVESTOR_MAX_SKILL)) {
            // Error is x% of price with x being lower the higher the skill of the investor
            float error = (INVESTOR_MAX_SKILL - skill - 1) * (float) r.nextGaussian() / 100f;
            hourPrice = hourPrice+hourPrice*error;
        }
    }

    @Override
    public String toString() {
        Float growth = (estimatedPrice-currPrice)/currPrice;
        return growth.toString();
    }

    // Growth from lowest to highest
    public static class StockPriceComparator implements Comparator {
        @Override
        public int compare(Object o1, Object o2) {
            if (!(o1 instanceof StockPrice) || !(o2 instanceof StockPrice)) {
                throw new ClassCastException();
            }

            StockPrice s1 = (StockPrice) o1;
            StockPrice s2 = (StockPrice) o2;

            float s1Growth = (s1.getEstimatedPrice()-s1.getCurrPrice())/s1.getCurrPrice();
            float s2Growth = (s2.getEstimatedPrice()-s2.getCurrPrice())/s2.getCurrPrice();

            return Float.compare(s1Growth, s2Growth);
        }
    }
}
