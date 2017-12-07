package model.onto;

import jade.content.Concept;

import static utils.Settings.*;
import static utils.Settings.HEALTHCARE;
import static utils.Settings.TECH;

public class InvestorInfo implements Concept {
    private static final long serialVersionUID = 1L;
    private String id;
    private int[] skill;

    public InvestorInfo() {
    }

    public InvestorInfo(String id, int[] skill) {
        this.id = id;
        this.skill = skill;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int[] getSkill() {
        return skill;
    }

    public void setSkill(int[] skill) {
        this.skill = skill;
    }

    @Override
    public boolean equals(Object obj) {
        return ((InvestorInfo) obj).getId().equals(this.id);
    }

    @Override
    public String toString() {
        String s = id + " - "
                + skill[TELECOM] + " "
                + skill[FINANCIAL] + " "
                + skill[INDUSTRIAL] + " "
                + skill[ENERGY] + " "
                + skill[HEALTHCARE] + " "
                + skill[TECH] + " ";
        return s;
    }
}
