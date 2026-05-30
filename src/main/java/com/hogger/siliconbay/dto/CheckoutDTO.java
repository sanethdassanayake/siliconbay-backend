package com.hogger.siliconbay.dto;

import java.io.Serializable;

public class CheckoutDTO implements Serializable {
    private int deliveryTypeId;
    private Long paymentInstrumentId;

    public int getDeliveryTypeId() {
        return deliveryTypeId;
    }

    public void setDeliveryTypeId(int deliveryTypeId) {
        this.deliveryTypeId = deliveryTypeId;
    }

    public Long getPaymentInstrumentId() {
        return paymentInstrumentId;
    }

    public void setPaymentInstrumentId(Long paymentInstrumentId) {
        this.paymentInstrumentId = paymentInstrumentId;
    }
}

