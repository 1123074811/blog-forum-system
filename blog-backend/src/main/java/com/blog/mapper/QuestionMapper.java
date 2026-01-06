package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Question;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    @Insert("<script>" +
            "INSERT INTO questions (quiz_bank_id, type, question, options, answer, explanation, sort_order) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.quizBankId}, #{item.type}, #{item.question}, #{item.options}, #{item.answer}, #{item.explanation}, #{item.sortOrder})" +
            "</foreach>" +
            "</script>")
    void insertBatch(@Param("list") List<Question> list);
}
