package com.ymgal.harvest.vndb.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ymgal.harvest.vndb.Constants;

public class Login {
    @JsonProperty("protocol")
    public Integer ProtocolVersion = 1;
    @JsonProperty("client")
    public String ClientName;
    @JsonProperty("clientver")
    public String ClientVersion;

    public Login() {
        this.ClientName = Constants.ClientName;
        this.ClientVersion = Constants.ClientVersion;
    }
}
