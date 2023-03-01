package com.github.sasergeev.example.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "data",
        "total",
        "page",
        "limit"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class DummyData implements Serializable {
    @JsonProperty("data")
    public List<DummyModel> data;
    @JsonProperty("total")
    public Integer total;
    @JsonProperty("page")
    public Integer page;
    @JsonProperty("limit")
    public Integer limit;

    public List<DummyModel> getData() {
        return data;
    }

    public Integer getTotal() {
        return total;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getLimit() {
        return limit;
    }
}
