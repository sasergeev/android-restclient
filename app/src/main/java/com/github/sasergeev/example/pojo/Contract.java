package com.github.sasergeev.example.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Contract implements Serializable {

    @JsonProperty("number")
    private String number;
    @JsonProperty("date")
    private String date;
    @JsonProperty("product")
    private String product;
    @JsonProperty("client")
    private String client;
    @JsonProperty("summa")
    private String summa;
    @JsonProperty("status")
    private String status;
    @JsonProperty("expdays")
    private int expdays;
    @JsonProperty("expsumma")
    private String expsumma;
    @JsonProperty("paymentday")
    private String paymentday;
    @JsonProperty("paymentsum")
    private String paymentsum;
    @JsonProperty("active")
    private boolean active;
    @JsonProperty("expstage")
    private int expstage;
    @JsonProperty("pdl")
    private boolean pdl;
    @JsonProperty("priority")
    private int priority;
    @JsonProperty("isTopUp")
    private boolean isTopUp;

    public String getNumber() {
        return number;
    }

    public String getDate() {
        return date;
    }

    public String getProduct() {
        return product;
    }

    public String getClient() {
        return client;
    }

    public String getSumma() {
        return summa;
    }

    public String getStatus() {
        return status;
    }

    public Integer getExpdays() {
        return expdays;
    }

    public String getExpsumma() {
        return expsumma;
    }

    public String getPaymentday() {
        return paymentday;
    }

    public Date getPaymentdate() {
        return new Date();
    }

    public String getPaymentsum() {
        return paymentsum;
    }

    public boolean isActive() {
        return active;
    }

    public Integer getExpstage() {
        return expstage;
    }

    public String getExpstageValue() {
        return "(" + expstage + ")";
    }

    public boolean isPdl() {
        return pdl;
    }

    public boolean isExpired() {
        return expdays > 0;
    }

    public int getPriority() {
        return priority;
    }

    public int getPriorityColor() {
        return priority;
    }

    public boolean isTopUp() {
        return isTopUp;
    }

    public Long getDateLong() {
        return new Date().getTime();
    }

}
