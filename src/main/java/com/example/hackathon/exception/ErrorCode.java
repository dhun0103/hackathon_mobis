package com.example.hackathon.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    NOT_FOUND_MEMBER(HttpStatus.NOT_FOUND.value(), "M001", "유저가 존재하지 않습니다."),

    DELETED_USER_EXCEPTION(HttpStatus.BAD_REQUEST.value(),"M002" ,"이미 탈퇴한 계정입니다.\n다른 계정으로 시도해 주세요." );



//    ERROR(HttpStatus.NO_CONTENT.value(),"S001","알수없는오류");

    private final int status;
    private final String code;
    private final String message;
}
