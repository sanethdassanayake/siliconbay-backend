package com.hogger.siliconbay.dto;

import java.io.Serializable;

public class ReviewDTO implements Serializable {
    private int stars;
    private String message;
    private String by;
    private String date;

    public ReviewDTO() {
    }

    public ReviewDTO(int stars, String message, String by, String date) {
        this.stars = stars;
        this.message = message;
        this.by = by;
        this.date = date;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getBy() {
        return by;
    }

    public void setBy(String by) {
        this.by = by;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
