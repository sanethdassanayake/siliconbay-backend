package com.hogger.siliconbay.dto;

import java.io.Serializable;

public class RatingDTO implements Serializable {
    private double average;
    private int count;

    public RatingDTO() {
    }

    public RatingDTO(double average, int count) {
        this.average = average;
        this.count = count;
    }

    public double getAverage() {
        return average;
    }

    public void setAverage(double average) {
        this.average = average;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
