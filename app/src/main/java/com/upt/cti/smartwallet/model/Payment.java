package com.upt.cti.smartwallet.model;

import java.io.Serializable;

public class Payment implements Serializable {
    public String timestamp;
    private double cost;
    private String name;
    private String type;

    public Payment() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public Payment(String name, double cost, String typeOfPayment){
        this.name = name;
        this.cost = cost;
        this.type = typeOfPayment;
    }

    public String getName() {
        return name;
    }

    public double getCost() {
        return cost;
    }

    public String getType() {
        return type;
    }

    public void setTimestamp(String t) {
        this.timestamp = t;
    }
}
