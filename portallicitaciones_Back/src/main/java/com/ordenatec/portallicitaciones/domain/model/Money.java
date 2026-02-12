package com.ordenatec.portallicitaciones.domain.model;

import java.math.BigDecimal;

public class Money {
    private BigDecimal amount;
    private String currency; // "EUR"
    private Boolean incluyeIVA; // null si no se sabe
    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    public Boolean getIncluyeIVA() {
        return incluyeIVA;
    }
    public void setIncluyeIVA(Boolean incluyeIVA) {
        this.incluyeIVA = incluyeIVA;
    }
}
