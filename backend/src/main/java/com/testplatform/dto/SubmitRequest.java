package com.testplatform.dto;

import java.util.Map;

public class SubmitRequest {
    // questionId -> selected option ('A'/'B'/'C'/'D'), missing entry = unanswered
    private Map<Long, String> answers;
    // "MANUAL", "TIME_UP", "FULLSCREEN_EXIT", "TAB_SWITCH_LIMIT"
    private String endReason;

    public Map<Long, String> getAnswers() { return answers; }
    public void setAnswers(Map<Long, String> answers) { this.answers = answers; }
    public String getEndReason() { return endReason; }
    public void setEndReason(String endReason) { this.endReason = endReason; }
}
