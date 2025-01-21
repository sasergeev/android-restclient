package com.github.sasergeev.example.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Contracts implements Serializable {

    @JsonProperty("contracts")
    private List<Contract> contracts = null;

    public List<Contract> getContracts() {
        return contracts;
    }

    @JsonProperty("expired")
    private int expired;
    @JsonProperty("closed")
    private int closed;
    @JsonProperty("actived")
    private int actived;
    @JsonProperty("total")
    private int total;

    public int getExpired() {
        return expired;
    }

    public int getClosed() {
        return closed;
    }

    public int getActived() {
        return actived;
    }

    public int getTotal() {
        return total;
    }
}
