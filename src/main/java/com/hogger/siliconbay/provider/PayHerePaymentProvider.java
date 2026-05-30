package com.hogger.siliconbay.provider;

import com.hogger.siliconbay.dto.PayHereRequestDTO;
import com.hogger.siliconbay.util.PayHereUtil;

public class PayHerePaymentProvider {
    private static final String PAYHERE_LIVE = "https://www.payhere.lk/pay/checkout";
    private static final String PAYHERE_SANDBOX = "https://sandbox.payhere.lk/pay/checkout";

    private String endpoint() {
        String sandbox = PayHereUtil.env("PAYHERE_SANDBOX", "true");
        if ("true".equalsIgnoreCase(sandbox) || "1".equals(sandbox)) {
            return PAYHERE_SANDBOX;
        }
        return PAYHERE_LIVE;
    }

    public String buildAutoSubmitForm(PayHereRequestDTO req) {
        String merchantId = PayHereUtil.getMerchantId();
        String returnUrl = PayHereUtil.getAppUrl() + "/payments/success";
        String cancelUrl = PayHereUtil.getAppUrl() + "/payments/cancel";
        String notifyUrl = PayHereUtil.getAppUrl() + "/api/payments/payhere/ipn";

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body onload='document.forms[0].submit()'>")
          .append("<form method='post' action='").append(endpoint()).append("'>");
        appendInput(sb, "merchant_id", merchantId);
        appendInput(sb, "return_url", returnUrl);
        appendInput(sb, "cancel_url", cancelUrl);
        appendInput(sb, "notify_url", notifyUrl);
        appendInput(sb, "order_id", req.getOrderId());
        appendInput(sb, "items", req.getItems());
        appendInput(sb, "currency", req.getCurrency());
        appendInput(sb, "amount", String.format("%.2f", req.getAmount()));
        appendInput(sb, "first_name", req.getFirstName());
        appendInput(sb, "last_name", req.getLastName());
        appendInput(sb, "email", req.getEmail());
        appendInput(sb, "phone", req.getPhone());
        appendInput(sb, "address", req.getAddress());
        appendInput(sb, "city", req.getCity());
        appendInput(sb, "country", req.getCountry());
        appendInput(sb, "hash", req.getHash());
        sb.append("<noscript><input type='submit' value='Pay Now'/></noscript>");
        sb.append("</form></body></html>");
        return sb.toString();
    }

    private void appendInput(StringBuilder sb, String name, String value) {
        if (value == null) value = "";
        String escaped = htmlEscape(value);
        sb.append("<input type='hidden' name='").append(name).append("' value='")
          .append(escaped).append("'/>");
    }

    private String htmlEscape(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '&': out.append("&amp;"); break;
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '"': out.append("&quot;"); break;
                case '\'': out.append("&#x27;"); break;
                default: out.append(c);
            }
        }
        return out.toString();
    }
}
