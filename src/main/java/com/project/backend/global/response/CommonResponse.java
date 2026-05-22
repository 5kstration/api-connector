package com.project.backend.global.response;

/*
 * 모든 API 응답 형식을 통일하기 위한 공통 응답 DTO입니다.
 */
public record CommonResponse<T>(
        boolean success,
        T data,
        String message
) {

    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(true, data, "요청이 성공했습니다.");
    }

    public static <T> CommonResponse<T> success(T data, String message) {
        return new CommonResponse<>(true, data, message);
    }

    public static CommonResponse<Void> success(String message) {
        return new CommonResponse<>(true, null, message);
    }

    public static CommonResponse<Void> fail(String message) {
        return new CommonResponse<>(false, null, message);
    }
}
