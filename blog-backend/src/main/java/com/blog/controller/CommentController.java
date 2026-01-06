package com.blog.controller;

import com.blog.dto.ApiResponse;
import com.blog.dto.CommentRequest;
import com.blog.entity.Comment;
import com.blog.service.CommentService;
import com.blog.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/articles/{articleId}/comments")
    public ApiResponse<List<Comment>> getComments(@PathVariable Long articleId) {
        return ApiResponse.success(commentService.getCommentsByArticleId(articleId));
    }

    @PostMapping("/comments")
    public ApiResponse<Comment> createComment(@RequestBody CommentRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();

        Comment comment = new Comment();
        comment.setArticleId(request.getArticleId());
        comment.setUserId(userId);
        comment.setParentId(request.getParentId());
        comment.setContent(request.getContent());
        comment.setLikeCount(0);
        comment.setCreatedAt(DateUtil.now());
        comment.setUpdatedAt(DateUtil.now());

        commentService.save(comment);
        return ApiResponse.success(comment);
    }

    @PutMapping("/comments/{id}")
    public ApiResponse<Comment> updateComment(@PathVariable Long id, @RequestBody CommentRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Comment comment = commentService.getById(id);

        if (comment == null) {
            return ApiResponse.error("Comment not found");
        }
        if (!comment.getUserId().equals(userId)) {
            return ApiResponse.error("Unauthorized");
        }

        comment.setContent(request.getContent());
        comment.setUpdatedAt(DateUtil.now());
        commentService.updateById(comment);

        return ApiResponse.success(comment);
    }

    @DeleteMapping("/comments/{id}")
    public ApiResponse<Boolean> deleteComment(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Comment comment = commentService.getById(id);

        if (comment == null) {
            return ApiResponse.error("Comment not found");
        }
        if (!comment.getUserId().equals(userId)) {
            return ApiResponse.error("Unauthorized");
        }

        commentService.removeById(id);
        return ApiResponse.success(true);
    }

    @PostMapping("/comments/{id}/like")
    public ApiResponse<Boolean> likeComment(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        commentService.likeComment(id, userId);
        return ApiResponse.success(true);
    }

    @DeleteMapping("/comments/{id}/like")
    public ApiResponse<Boolean> unlikeComment(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        commentService.unlikeComment(id, userId);
        return ApiResponse.success(true);
    }
}
