package model;

import java.util.ArrayList;

public class Trust {
    private static final int D = 10;

    private float trust;
    private ArrayList<Float> gains;
    private ArrayList<Float> pastCapital;

    public Trust(){
        this.trust = 0;
        this.gains = new ArrayList<>();
        this.pastCapital = new ArrayList<>();
    }

    public float getTrust(){
        return trust;
    }

    public ArrayList<Float> getGains(){
        return gains;
    }

    public ArrayList<Float> getPastCapital() {
        return pastCapital;
    }

    public  void setTrust(float trust){
        this.trust = trust;
    }

    // Add element to pastCapital and calculate gain and trust
    public void addPastCapital(float capital){
        pastCapital.add(capital);
        if (pastCapital.size() != 1) {
            float previousCapital = pastCapital.get(pastCapital.size()-2);
            gains.add((capital-previousCapital)/previousCapital);
        }
        if (gains.size() > 0) {
            float num = 0, denom = 0;
            for (int i = 0; i < gains.size(); i++) {
                int delta = gains.size()-i-1;
                double lambda = -D/Math.log(0.5);
                double weight = Math.pow(Math.E, -delta/lambda);
                float value = gains.get(i);

                num += weight*value;
                denom += weight;
            }
            trust = num/denom;
        }
    }
}
