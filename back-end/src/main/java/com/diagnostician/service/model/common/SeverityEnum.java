package com.diagnostician.service.model.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

interface ISeverity {
    String getMessage();
}

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum SeverityEnum implements ISeverity {
    HIGH("high"),
    MEDIUM("medium"),
    LOW("low");
    private String message;

    public void setMessage(String message) {
        this.message = message;
    }

}