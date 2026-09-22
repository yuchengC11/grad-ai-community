package com.graduate.graidaicommunity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@Data
public class ChatRequest {

    @NotBlank(message = "问题不能为空")
    @Size(max = 2000, message = "问题不能超过2000字")
    private String question;

    @Size(max = 20, message = "历史对话最多20轮")
    private List<Map<String, String>> history;

    private String persona;
}