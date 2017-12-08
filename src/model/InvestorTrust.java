package model;

import java.util.ArrayList;

public class InvestorTrust {
    float trust;
    ArrayList<Float> historicGains;

    public InvestorTrust(){
        this.trust = 0;
        this.historicGains = new ArrayList<>();
    }

    public float getTrust(){
        return trust;
    }

    public  void setTrust(float trust){
        this.trust = trust;
    }

    public ArrayList<Float> getHistoricGains(){
        return historicGains;
    }

    public void addGaint(float gain){
        this.historicGains.add(gain);
    }
}
