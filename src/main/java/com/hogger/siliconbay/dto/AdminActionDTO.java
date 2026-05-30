package com.hogger.siliconbay.dto;

import java.io.Serializable;

public class AdminActionDTO implements Serializable {
    private String status;
    private String role;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}

