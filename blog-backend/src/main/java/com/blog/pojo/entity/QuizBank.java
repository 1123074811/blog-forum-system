package com.blog.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("quiz_banks")
public class QuizBank {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String type; // quiz 或 file
    private String filePath; // 文件路径
    private String fileType; // 文件类型：txt, docx, pdf, md 等
    private Integer questionCount;
    private Boolean isPublic;
    private String createdAt;

    @TableField(exist = false)
    private java.util.List<Question> questions;
}
