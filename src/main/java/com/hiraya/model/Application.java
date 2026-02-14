package com.hiraya.model;

import java.sql.Timestamp;
import java.sql.Date;

public class Application {
    private int id;
    private String applicationId;
    private int userId;
    private String referenceNumber;
    
    // Personal Information
    private String firstName;
    private String middleName;
    private String lastName;
    private String sex;
    private Date birthdate;
    private Integer age;
    private String placeOfBirth;
    private Double height;
    private Double weight;
    private String mobileNumber;
    private String email;
    private String facebookUrl;
    
    // Present Address
    private String presentRegion;
    private String presentProvince;
    private String presentMunicipality;
    private String presentBarangay;
    private String presentHouseNumber;
    private String presentStreet;
    private String presentZipCode;
    
    // Permanent Address
    private String permanentRegion;
    private String permanentProvince;
    private String permanentMunicipality;
    private String permanentBarangay;
    private String permanentHouseNumber;
    private String permanentStreet;
    private String permanentZipCode;
    
    // Academic Information - Junior High
    private String jhsName;
    private String jhsSchoolId;
    private String jhsType;
    
    // Academic Information - Senior High
    private String shsName;
    private String shsSchoolId;
    private String shsType;
    private String track;
    private String strand;
    private Double grade12Gwa;
    private String honorsReceived;
    
    // College Choices
    private String collegeFirst;
    private String collegeSecond;
    private String collegeThird;
    private String programFirst;
    private String programSecond;
    private String programThird;
    
    // Essay
    private String essay;
    
    // Status
    private String applicationStatus;
    private Timestamp submissionDate;
    private Timestamp lastSaved;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // Constructors
    public Application() {}
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
    
    public Date getBirthdate() { return birthdate; }
    public void setBirthdate(Date birthdate) { this.birthdate = birthdate; }
    
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    
    public String getPlaceOfBirth() { return placeOfBirth; }
    public void setPlaceOfBirth(String placeOfBirth) { this.placeOfBirth = placeOfBirth; }
    
    public Double getHeight() { return height; }
    public void setHeight(Double height) { this.height = height; }
    
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFacebookUrl() { return facebookUrl; }
    public void setFacebookUrl(String facebookUrl) { this.facebookUrl = facebookUrl; }
    
    public String getPresentRegion() { return presentRegion; }
    public void setPresentRegion(String presentRegion) { this.presentRegion = presentRegion; }
    
    public String getPresentProvince() { return presentProvince; }
    public void setPresentProvince(String presentProvince) { this.presentProvince = presentProvince; }
    
    public String getPresentMunicipality() { return presentMunicipality; }
    public void setPresentMunicipality(String presentMunicipality) { this.presentMunicipality = presentMunicipality; }
    
    public String getPresentBarangay() { return presentBarangay; }
    public void setPresentBarangay(String presentBarangay) { this.presentBarangay = presentBarangay; }
    
    public String getPresentHouseNumber() { return presentHouseNumber; }
    public void setPresentHouseNumber(String presentHouseNumber) { this.presentHouseNumber = presentHouseNumber; }
    
    public String getPresentStreet() { return presentStreet; }
    public void setPresentStreet(String presentStreet) { this.presentStreet = presentStreet; }
    
    public String getPresentZipCode() { return presentZipCode; }
    public void setPresentZipCode(String presentZipCode) { this.presentZipCode = presentZipCode; }
    
    public String getPermanentRegion() { return permanentRegion; }
    public void setPermanentRegion(String permanentRegion) { this.permanentRegion = permanentRegion; }
    
    public String getPermanentProvince() { return permanentProvince; }
    public void setPermanentProvince(String permanentProvince) { this.permanentProvince = permanentProvince; }
    
    public String getPermanentMunicipality() { return permanentMunicipality; }
    public void setPermanentMunicipality(String permanentMunicipality) { this.permanentMunicipality = permanentMunicipality; }
    
    public String getPermanentBarangay() { return permanentBarangay; }
    public void setPermanentBarangay(String permanentBarangay) { this.permanentBarangay = permanentBarangay; }
    
    public String getPermanentHouseNumber() { return permanentHouseNumber; }
    public void setPermanentHouseNumber(String permanentHouseNumber) { this.permanentHouseNumber = permanentHouseNumber; }
    
    public String getPermanentStreet() { return permanentStreet; }
    public void setPermanentStreet(String permanentStreet) { this.permanentStreet = permanentStreet; }
    
    public String getPermanentZipCode() { return permanentZipCode; }
    public void setPermanentZipCode(String permanentZipCode) { this.permanentZipCode = permanentZipCode; }
    
    public String getJhsName() { return jhsName; }
    public void setJhsName(String jhsName) { this.jhsName = jhsName; }
    
    public String getJhsSchoolId() { return jhsSchoolId; }
    public void setJhsSchoolId(String jhsSchoolId) { this.jhsSchoolId = jhsSchoolId; }
    
    public String getJhsType() { return jhsType; }
    public void setJhsType(String jhsType) { this.jhsType = jhsType; }
    
    public String getShsName() { return shsName; }
    public void setShsName(String shsName) { this.shsName = shsName; }
    
    public String getShsSchoolId() { return shsSchoolId; }
    public void setShsSchoolId(String shsSchoolId) { this.shsSchoolId = shsSchoolId; }
    
    public String getShsType() { return shsType; }
    public void setShsType(String shsType) { this.shsType = shsType; }
    
    public String getTrack() { return track; }
    public void setTrack(String track) { this.track = track; }
    
    public String getStrand() { return strand; }
    public void setStrand(String strand) { this.strand = strand; }
    
    public Double getGrade12Gwa() { return grade12Gwa; }
    public void setGrade12Gwa(Double grade12Gwa) { this.grade12Gwa = grade12Gwa; }
    
    public String getHonorsReceived() { return honorsReceived; }
    public void setHonorsReceived(String honorsReceived) { this.honorsReceived = honorsReceived; }
    
    public String getCollegeFirst() { return collegeFirst; }
    public void setCollegeFirst(String collegeFirst) { this.collegeFirst = collegeFirst; }
    
    public String getCollegeSecond() { return collegeSecond; }
    public void setCollegeSecond(String collegeSecond) { this.collegeSecond = collegeSecond; }
    
    public String getCollegeThird() { return collegeThird; }
    public void setCollegeThird(String collegeThird) { this.collegeThird = collegeThird; }
    
    public String getProgramFirst() { return programFirst; }
    public void setProgramFirst(String programFirst) { this.programFirst = programFirst; }
    
    public String getProgramSecond() { return programSecond; }
    public void setProgramSecond(String programSecond) { this.programSecond = programSecond; }
    
    public String getProgramThird() { return programThird; }
    public void setProgramThird(String programThird) { this.programThird = programThird; }
    
    public String getEssay() { return essay; }
    public void setEssay(String essay) { this.essay = essay; }
    
    public String getApplicationStatus() { return applicationStatus; }
    public void setApplicationStatus(String applicationStatus) { this.applicationStatus = applicationStatus; }
    
    public Timestamp getSubmissionDate() { return submissionDate; }
    public void setSubmissionDate(Timestamp submissionDate) { this.submissionDate = submissionDate; }
    
    public Timestamp getLastSaved() { return lastSaved; }
    public void setLastSaved(Timestamp lastSaved) { this.lastSaved = lastSaved; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    
    public String getFullName() {
        return firstName + " " + 
               (middleName != null && !middleName.isEmpty() && !middleName.equals("N/A") ? middleName + " " : "") + 
               lastName;
    }
}