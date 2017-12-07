package model.onto;

import jade.content.AgentAction;

public class InvestorSubscribe implements AgentAction {
    private static final long serialVersionUID = 1L;
    private InvestorInfo investor;

    public InvestorSubscribe() {
    }

    public InvestorSubscribe(InvestorInfo investor) {
        this.investor = investor;
    }

    public InvestorInfo getInvestor() {
        return investor;
    }

    public void setInvestor(InvestorInfo investor) {
        this.investor = investor;
    }
}
