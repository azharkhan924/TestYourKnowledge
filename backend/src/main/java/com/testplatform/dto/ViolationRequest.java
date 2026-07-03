package com.testplatform.dto;

public class ViolationRequest {
    // "TAB_SWITCH" or "FULLSCREEN_EXIT"
    private String type;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
