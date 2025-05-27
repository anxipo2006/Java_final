package com.coffeeshop.core.model;

import java.sql.Date; // Dùng java.sql.Date cho trường date

public class RotaEntry {

    private int rowId;      // row_id (có thể là PK auto-increment)
    private String rotaId;  // rota_id
    private Date date;      // date
    private String shiftId; // shift_id (FK to shift)
    private String staffId; // staff_id (FK to staff)

    // Constructors
    public RotaEntry() {
    }

    public RotaEntry(String rotaId, Date date, String shiftId, String staffId) {
        this.rotaId = rotaId;
        this.date = date;
        this.shiftId = shiftId;
        this.staffId = staffId;
    }

    public RotaEntry(int rowId, String rotaId, Date date, String shiftId, String staffId) {
        this.rowId = rowId;
        this.rotaId = rotaId;
        this.date = date;
        this.shiftId = shiftId;
        this.staffId = staffId;
    }

    // Getters and Setters
    public int getRowId() {
        return rowId;
    }

    public void setRowId(int rowId) {
        this.rowId = rowId;
    }

    public String getRotaId() {
        return rotaId;
    }

    public void setRotaId(String rotaId) {
        this.rotaId = rotaId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    @Override
    public String toString() {
        return "RotaEntry{"
                + "rowId=" + rowId
                + ", rotaId='" + rotaId + '\''
                + ", date=" + date
                + ", shiftId='" + shiftId + '\''
                + ", staffId='" + staffId + '\''
                + '}';
    }
}
