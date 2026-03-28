package com.blog.controller;

import com.blog.annotation.RateLimit;
import com.blog.exception.BusinessException;
import com.blog.exception.ErrorCode;
import com.blog.pojo.dto.ApiResponse;
import com.blog.pojo.dto.CommentRequest;
import com.blog.pojo.entity.Comment;
import com.blog.pojo.entity.User;
import com.blog.service.CommentService;
import com.blog.service.SensitiveWordFilterService;
import com.blog.service.UserService;
import com.blog.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class CommentController extends BaseController {

    private final CommentService commentService;
    private final com.blog.service.HotArticleService hotArticleService;
    private final SensitiveWordFilterService sensitiveWordFilterService;
    private final UserService userService;

    @GetMapping("/articles/{articleId}/comments")
    public ApiResponse<List<Comment>> getComments(@PathVariable Long articleId) {
        return ApiResponse.success(commentService.getCommentsByArticleId(articleId));
    }

    @PostMapping("/comments")
    @RateLimit(key = "comment:create", count = 10, time = 60, limitType = RateLimit.LimitType.USER, message = "评论过于频繁，请稍后再试")
    public ApiResponse<Comment> createComment(@Valid @RequestBody CommentRequest request, Authentication auth) {
        Long userId = getCurrentUserId(auth);

        Comment comment = new Comment();
        comment.setArticleId(request.getArticleId());
        comment.setUserId(userId);
        comment.setParentId(request.getParentId());
        comment.setContent(sensitiveWordFilterService.filter(request.getContent()));
        comment.setLikeCount(0);
        comment.setCreatedAt(DateUtil.now());
        comment.setUpdatedAt(DateUtil.now());

        commentService.save(comment);
        User user = userService.getById(userId);
        if (user != null) {
            String displayName = (user.getNickname() != null && !user.getNickname().isBlank())
                    ? user.getNickname()
                    : user.getUsername();
            comment.setUsername(displayName);
            comment.setAvatar(user.getAvatar());
        }
        // 更新文章热度分数
        hotArticleService.updateArticleHotScore(request.getArticleId());
        return ApiResponse.success(comment);
    }

    @PutMapping("/comments/{id}")
    public ApiResponse<Comment> updateComment(@PathVariable Long id, @Valid @RequestBody CommentRequest request, Authentication auth) {
        Long userId = getCurrentUserId(auth);
        Comment comment = commentService.getById(id);

        if (comment == null) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        
        checkPermission(comment.getUserId(), userId);

        comment.setContent(sensitiveWordFilterService.filter(request.getContent()));
        comment.setUpdatedAt(DateUtil.now());
        commentService.updateById(comment);

        return ApiResponse.success(comment);
    }

    @DeleteMapping("/comments/{id}")
    public ApiResponse<Boolean> deleteComment(@PathVariable Long id, Authentication auth) {
        Long userId = getCurrentUserId(auth);
        Comment comment = commentService.getById(id);

        if (comment == null) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        
        checkPermission(comment.getUserId(), userId);

        Long articleId = comment.getArticleId();
        commentService.removeById(id);
        // 更新文章热度分数
        hotArticleService.updateArticleHotScore(articleId);
        return ApiResponse.success(true);
    }

    @PostMapping("/comments/{id}/like")
    public ApiResponse<Boolean> likeComment(@PathVariable Long id, Authentication auth) {
        Long userId = getCurrentUserId(auth);
        commentService.likeComment(id, userId);
        return ApiResponse.success(true);
    }

    @DeleteMapping("/comments/{id}/like")
    public ApiResponse<Boolean> unlikeComment(@PathVariable Long id, Authentication auth) {
        Long userId = getCurrentUserId(auth);
        commentService.unlikeComment(id, userId);
        return ApiResponse.success(true);
    }
}
