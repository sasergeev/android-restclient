package com.github.sasergeev.example.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "clientId",
        "isActive",
        "nickName"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserData implements Serializable {

    @JsonProperty("clientId")
    private String clientId;
    @JsonProperty("isActive")
    private Boolean isActive;
    @JsonProperty("nickName")
    private String nickName;

    public UserData() {
    }

    public UserData(String clientId, Boolean isActive, String nickName) {
        this.clientId = clientId;
        this.isActive = isActive;
        this.nickName = nickName;
    }

    public String getClientId() {
        return clientId;
    }

    public String getActive() {
        return String.valueOf(isActive);
    }

    public String getNickName() {
        return nickName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append("clientId");
        sb.append('=');
        sb.append(((this.clientId == null)?"<null>":this.clientId));
        sb.append(',');
        sb.append("isActive");
        sb.append('=');
        sb.append(((this.isActive == null)?"<null>":this.isActive));
        sb.append(',');
        sb.append("nickName");
        sb.append('=');
        sb.append(((this.nickName == null)?"<null>":this.nickName));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }
}