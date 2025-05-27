package com.coffeeshop.core.model;

import java.sql.Timestamp;

public class User {

    private int userId;
    private String username;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String email;
    private UserRole role;
    private String staffIdFk; // <<<< THÊM TRƯỜNG NÀY
    private Timestamp createdAt;

    public enum UserRole {
        ADMIN,
        STAFF;

        public static UserRole fromString(String roleString) {
            if (roleString != null) {
                try {
                    return UserRole.valueOf(roleString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid role string: " + roleString + ". Defaulting to STAFF.");
                    return STAFF;
                }
            }
            return STAFF;
        }
    }

    public User() {
    }

    // Constructor có thể cần cập nhật nếu muốn truyền staffIdFk
    public User(String username, String firstName, String lastName, String email, UserRole role, String staffIdFk) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        if (role == UserRole.STAFF) { // Chỉ gán staffIdFk nếu là STAFF
            this.staffIdFk = staffIdFk;
        } else {
            this.staffIdFk = null;
        }
    }

    // Constructor cũ (nếu vẫn cần)
    public User(String username, String firstName, String lastName, String email, UserRole role) {
        this(username, firstName, lastName, email, role, null);
    }

    // Getters and Setters (thêm cho staffIdFk)
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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

    public String getFullName() {
        String fName = firstName != null ? firstName : "";
        String lName = lastName != null ? lastName : "";
        return (fName + " " + lName).trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public void setRole(String roleString) {
        this.role = UserRole.fromString(roleString);
    }

    public String getStaffIdFk() {
        return staffIdFk;
    } // <<<< GETTER

    public void setStaffIdFk(String staffIdFk) { // <<<< SETTER
        if (this.role == UserRole.STAFF) {
            this.staffIdFk = staffIdFk;
        } else {
            this.staffIdFk = null; // Đảm bảo staffIdFk là null nếu không phải STAFF
            if (staffIdFk != null) {
                System.err.println("Cảnh báo: Cố gắng gán staff_id cho user không phải STAFF. staff_id_fk sẽ là null.");
            }
        }
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{"
                + "userId=" + userId
                + ", username='" + username + '\''
                + ", firstName='" + firstName + '\''
                + ", lastName='" + lastName + '\''
                + ", email='" + email + '\''
                + ", role=" + role
                + ", staffIdFk='" + staffIdFk + '\''
                + // <<<< THÊM VÀO TOSTRING
                ", createdAt=" + createdAt
                + '}';
    }
}
