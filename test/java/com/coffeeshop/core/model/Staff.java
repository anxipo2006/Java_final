package com.coffeeshop.core.model;

public class Staff {

    private String staffId;
    private String firstName;
    private String lastName;
    private String position;
    private double salaryPerHour; // sal_per_hour

    // Constructors
    public Staff() {
    }

    public Staff(String staffId, String firstName, String lastName, String position, double salaryPerHour) {
        this.staffId = staffId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.position = position;
        this.salaryPerHour = salaryPerHour;
    }

    // Getters and Setters
    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
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

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public double getSalaryPerHour() {
        return salaryPerHour;
    }

    public void setSalaryPerHour(double salaryPerHour) {
        this.salaryPerHour = salaryPerHour;
    }

    // Tiện lợi để hiển thị tên đầy đủ
    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return "Staff{"
                + "staffId='" + staffId + '\''
                + ", firstName='" + firstName + '\''
                + ", lastName='" + lastName + '\''
                + ", position='" + position + '\''
                + ", salaryPerHour=" + salaryPerHour
                + '}';
    }

    // Phương thức để hiển thị thông tin nhân viên
    public String displayInfo() {
        return "ID: " + staffId + ", Name: " + getFullName() + ", Position: " + position + ", Salary/Hour: " + salaryPerHour;
    }

    // Phương thức để hiển thị thông tin nhân viên trong bảng
    public String[] toTableRow() {
        return new String[]{staffId, firstName, lastName, position, String.valueOf(salaryPerHour)};
    }

}
