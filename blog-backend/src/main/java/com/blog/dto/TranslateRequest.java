package com.blog.dto;

import lombok.Data;

@Data
public class TranslateRequest {
    private String text;
    private String context; // database_field 或 table
}