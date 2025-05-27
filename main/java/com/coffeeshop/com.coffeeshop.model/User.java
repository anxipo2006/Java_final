package com.coffeeshop.com.coffeeshop.model;

import lombok.Data; // Nếu dùng Lombok
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.sql.Timestamp;

@Data // Bao gồm @Getter, @Setter, @ToString, @EqualsAndHashCode, @RequiredArgsConstructor
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private int userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private UserRole role; // Sẽ tạo enum UserRole
    private String staffIdFk; // Khóa ngoại tới bảng staff
    private Timestamp createdAt;

    // Constructor, getters, setters, toString (nếu không dùng Lombok)
}
