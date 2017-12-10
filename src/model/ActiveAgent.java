package model;

import data.Transaction;
import jade.content.lang.Codec;
import jade.content.onto.Ontology;
import jade.core.AID;
import ontology.StockPrice;
import sajas.core.Agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import static utils.Settings.PORTFOLIO_SIZE;
import static utils.Settings.TRANSACTION_TAX;

public abstract class ActiveAgent extends Agent {
    protected Codec codec;
    protected Ontology stockMarketOntology;

    protected String id;
    protected float capital;
    protected float portfolioValue;

    protected ArrayList<Transaction> active;
    protected ArrayList<Transaction> closed;

    protected AID informer;

    protected ActiveAgent(String id, float initialCapital) {
        this.id = id;
        this.capital = initialCapital;
        this.active = new ArrayList<>();
        this.closed = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public float getCapital() {
        return capital;
    }

    public float getPortfolioValue() {
        return portfolioValue;
    }

    public float getTotalCapital() {
        return capital + portfolioValue;
    }

    // Updates the sum of values of every stock
    protected void updatePortfolioValue(ArrayList<StockPrice> prices) {
        portfolioValue = 0;
        for (Transaction t: active) {
            for (StockPrice stock: prices) {
                if (stock.getSymbol().equals(t.getStock())) {
                    portfolioValue += stock.getCurrPrice()*t.getQuantity();
                    break;
                }
            }
        }
    }

    // Buys/sells stock according to current prices and predicted prices
    protected void moveStock(ArrayList<StockPrice> prices) {
        // Get top growth stock
        ArrayList<StockPrice> predictedGrowth = new ArrayList<>(prices);
        Collections.sort(predictedGrowth, new StockPrice.StockPriceComparator());
        Collections.reverse(predictedGrowth);

        // Get lowest growth stock owned
        ArrayList<StockPrice> currentOwned = new ArrayList<>();
        for (Transaction t: active) {
            for (StockPrice stock: predictedGrowth) {
                if (t.getStock().equals(stock.getSymbol())) {
                    currentOwned.add(stock);
                }
            }
        }
        Collections.sort(currentOwned, new StockPrice.StockPriceComparator());

        // Decide stock to buy and sell
        ArrayList<StockPrice> union = new ArrayList<>(predictedGrowth.subList(0, PORTFOLIO_SIZE));
        union.addAll(currentOwned);
        ArrayList<StockPrice> intersection = new ArrayList<>(predictedGrowth.subList(0, PORTFOLIO_SIZE));
        intersection.retainAll(currentOwned);

        // Get top growth stock not currently owned
        ArrayList<StockPrice> toBuy = new ArrayList<>(predictedGrowth.subList(0, PORTFOLIO_SIZE));
        toBuy.removeAll(intersection);

        // Get worse growth stock currently owned
        ArrayList<StockPrice> toSell = new ArrayList<>(currentOwned);
        toSell.removeAll(intersection);

        int size = toBuy.size() < toSell.size() ? toBuy.size(): toSell.size();
        // Sell stock owned with lowest growth potential
        for (int i = 0; i < size; i++) {
            sellStock(toSell.get(i));
        }

        // Buy stock with highest growth potential to replace the ones sold
        for (int i = 0; i < size; i++) {
            buyStock(toBuy.get(i), capital/size);
        }

        // Buy stock with biggest growth with the remaining capital
        float amountPerStock = capital/PORTFOLIO_SIZE;
        for (int i = 0; i < PORTFOLIO_SIZE; i++) {
            buyStock(predictedGrowth.get(i), amountPerStock);
        }
    }

    // Buy stock
    protected void buyStock(StockPrice stock, float amountPerStock) {
        int quantity = Math.round(amountPerStock/stock.getCurrPrice()-0.5f);
        if (quantity > 0) {
            boolean transactionFound = false;
            for (Transaction t: active) {
                if (t.getStock().equals(stock.getSymbol())) {
                    t.setQuantity(t.getQuantity() + quantity);
                    capital -= stock.getCurrPrice() * quantity;
                    transactionFound = true;
                }
            }

            if (!transactionFound) {
                Transaction t = new Transaction(stock.getSymbol(), stock.getCurrPrice(), quantity);
                active.add(t);
                capital -= stock.getCurrPrice() * quantity + TRANSACTION_TAX;
            }
        }
    }

    // Sell stock
    protected void sellStock(StockPrice stock) {
        for (Iterator<Transaction> it = active.iterator(); it.hasNext(); ) {
            Transaction t = it.next();
            if (t.getStock().equals(stock.getSymbol())) {
                float currentPrice = stock.getCurrPrice();
                t.setSellPrice(currentPrice);
                t.closeTransaction();
                closed.add(t);
                it.remove();
                capital += t.getQuantity()*currentPrice;
                break;
            }
        }
    }
}
