package model.onto;

import jade.content.Concept;
import java.util.ArrayList;

import static utils.Settings.*;
import static utils.Settings.HEALTHCARE;
import static utils.Settings.TECH;

public class InvestorInfo implements Concept {
    private static final long serialVersionUID = 1L;
    private String id;
    private ArrayList<Integer> skill;

    public InvestorInfo() {
    }

    public InvestorInfo(String id, ArrayList<Integer> skill) {
        this.id = id;
        this.skill = skill;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ArrayList<Integer> getSkill() {
        return skill;
    }

    public void setSkill(ArrayList<Integer> skill) {
        this.skill = skill;
    }

    @Override
    public boolean equals(Object obj) {
        return ((InvestorInfo) obj).getId().equals(this.id);
    }

    @Override
    public String toString() {
        String s = id + " - "
                + skill.get(TELECOM) + " "
                + skill.get(FINANCIAL) + " "
                + skill.get(INDUSTRIAL) + " "
                + skill.get(ENERGY) + " "
                + skill.get(HEALTHCARE) + " "
                + skill.get(TECH) + " ";
        return s;
    }
}
