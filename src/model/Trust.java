package model;

import java.util.ArrayList;

public class Trust {
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

    public void addPastCapital(float capital){
        pastCapital.add(capital);
        if (pastCapital.size() != 1) {
            float previousCapital = pastCapital.get(pastCapital.size()-2);
            gains.add((capital-previousCapital)/previousCapital);
        }
        if (gains.size() > 0) {
            // TODO formula for investor trust
            trust = gains.get(gains.size()-1);
        }
    }
}
