package com.moneylog.apiconnector.global.response;


/* API 요청의 응답 확인을 위한
record dto response
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
