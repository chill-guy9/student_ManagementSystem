package com.sms.nexus.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int code;
    private T data;
    private String message;

    public ApiResponse() {}

    public ApiResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, data, "success");
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(200, data, message);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, null, "success");
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, null, message);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, null, message);
    }
}
