package model;

public class Report {
    private String investorID;
    private float totalGains;

    public Report(String investorID, float totalGains){
        this.investorID = investorID;
        this.totalGains = totalGains;
    }

    public void setInvestorID(String investorID){
        this.investorID = investorID;
    }

    public String getInvestorID(){
        return investorID;
    }

    public void setTotalGains(float totalGains) {
        this.totalGains = totalGains;
    }

    public float getTotalGains() {
        return totalGains;
    }
}
