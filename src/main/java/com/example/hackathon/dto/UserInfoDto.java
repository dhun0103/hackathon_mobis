package com.example.hackathon.dto;

import com.example.hackathon.domain.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDto {

    private Long userId;
    private String accountName;

    public UserInfoDto(Member member) {
        this.userId = member.getAccountId();
        this.accountName=member.getAccountName();
    }
}
