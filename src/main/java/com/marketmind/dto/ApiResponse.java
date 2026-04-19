package com.marketmind.dto;

import java.util.List;

/**
 * Generic API response wrapper for list operations.
 * Provides consistent response format with data and messages.
 */
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private List<T> data;
    private int count;

    // Constructor for successful responses with data
    public ApiResponse(List<T> data) {
        this.success = true;
        this.data = data;
        this.count = data != null ? data.size() : 0;
        this.message = data != null && !data.isEmpty()
            ? "Data retrieved successfully"
            : "No data found";
    }

    // Constructor for custom messages
    public ApiResponse(boolean success, String message, List<T> data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.count = data != null ? data.size() : 0;
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
        this.count = data != null ? data.size() : 0;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}