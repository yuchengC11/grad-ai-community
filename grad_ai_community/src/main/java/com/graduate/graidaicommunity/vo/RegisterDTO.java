package com.graduate.graidaicommunity.vo;

import lombok.Data;

@Data
public class RegisterDTO {
    private String username;
    private String nickname;
    private String password;
    private Integer interestTag;
}