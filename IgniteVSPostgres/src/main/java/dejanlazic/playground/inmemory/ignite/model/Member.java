package dejanlazic.playground.inmemory.ignite.model;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Ignite-compatible Member entity
 * Uses @QuerySqlField annotations for SQL indexing
 */
public class Member implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @QuerySqlField(index = true)
    private Long memberId;
    
    @QuerySqlField(index = true)
    private String memberNumber;
    
    @QuerySqlField
    private String firstName;
    
    @QuerySqlField
    private String lastName;
    
    @QuerySqlField
    private Date dateOfBirth;
    
    @QuerySqlField
    private String gender;
    
    @QuerySqlField
    private String address;
    
    @QuerySqlField
    private String city;
    
    @QuerySqlField
    private String state;
    
    @QuerySqlField
    private String zipCode;
    
    @QuerySqlField
    private String phone;
    
    @QuerySqlField
    private String email;
    
    @QuerySqlField
    private Timestamp createdAt;
    
    @QuerySqlField
    private Timestamp updatedAt;
    
    // Constructors
    public Member() {
    }
    
    public Member(Long memberId, String memberNumber, String firstName, String lastName) {
        this.memberId = memberId;
        this.memberNumber = memberNumber;
        this.firstName = firstName;
        this.lastName = lastName;
    }
    
    // Getters and Setters
    public Long getMemberId() {
        return memberId;
    }
    
    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }
    
    public String getMemberNumber() {
        return memberNumber;
    }
    
    public void setMemberNumber(String memberNumber) {
        this.memberNumber = memberNumber;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public Date getDateOfBirth() {
        return dateOfBirth;
    }
    
    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
    
    public String getGender() {
        return gender;
    }
    
    public void setGender(String gender) {
        this.gender = gender;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public String getZipCode() {
        return zipCode;
    }
    
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
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
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Utility methods
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (address != null) sb.append(address);
        if (city != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city);
        }
        if (state != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(state);
        }
        if (zipCode != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(zipCode);
        }
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Member member = (Member) o;
        return Objects.equals(memberId, member.memberId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(memberId);
    }
    
    @Override
    public String toString() {
        return "Member{" +
                "memberId=" + memberId +
                ", memberNumber='" + memberNumber + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                '}';
    }
}

// Made with Bob
