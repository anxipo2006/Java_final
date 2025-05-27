package com.coffeeshop.core.exception;

public class DatabaseOperationException extends RuntimeException { // Hoặc extends Exception nếu bạn muốn nó là checked exception

    public DatabaseOperationException(String message) {
        super(message);
    }

    public DatabaseOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
