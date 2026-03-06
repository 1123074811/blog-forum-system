package com.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.pojo.entity.Comment;
import java.util.List;

public interface CommentService extends IService<Comment> {
    List<Comment> getCommentsByArticleId(Long articleId);
    void likeComment(Long commentId, Long userId);
    void unlikeComment(Long commentId, Long userId);
}
