package com.example.resumematcher.model;

import java.util.ArrayList;
import java.util.List;

public class ResumeKeyInfo {
    private String name;
    private String phone;
    private String email;
    private String address;
    private String jobIntention;
    private String expectedSalary;
    private String yearsOfExperience;
    private List<String> educationBackground = new ArrayList<>();
    private List<String> projectExperiences = new ArrayList<>();
    private List<String> skills = new ArrayList<>();
    private List<String> workExperiences = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getJobIntention() {
        return jobIntention;
    }

    public void setJobIntention(String jobIntention) {
        this.jobIntention = jobIntention;
    }

    public String getExpectedSalary() {
        return expectedSalary;
    }

    public void setExpectedSalary(String expectedSalary) {
        this.expectedSalary = expectedSalary;
    }

    public String getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(String yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public List<String> getEducationBackground() {
        return educationBackground;
    }

    public void setEducationBackground(List<String> educationBackground) {
        this.educationBackground = safeList(educationBackground);
    }

    public List<String> getProjectExperiences() {
        return projectExperiences;
    }

    public void setProjectExperiences(List<String> projectExperiences) {
        this.projectExperiences = safeList(projectExperiences);
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = safeList(skills);
    }

    public List<String> getWorkExperiences() {
        return workExperiences;
    }

    public void setWorkExperiences(List<String> workExperiences) {
        this.workExperiences = safeList(workExperiences);
    }

    private List<String> safeList(List<String> value) {
        return value == null ? new ArrayList<>() : value;
    }
}
