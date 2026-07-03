package com.testplatform.dto;

public class JoinRequest {
    private Long testId;
    private String studentName;
    private String contactNumber;

    public Long getTestId() { return testId; }
    public void setTestId(Long testId) { this.testId = testId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
}
