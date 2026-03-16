package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.pojo.entity.Comment;
import com.blog.pojo.entity.CommentLike;
import com.blog.pojo.entity.User;
import com.blog.mapper.CommentLikeMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.UserMapper;
import com.blog.service.CommentService;
import com.blog.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final CommentLikeMapper commentLikeMapper;
    private final UserMapper userMapper;

    @Override
    public List<Comment> getCommentsByArticleId(Long articleId) {
        List<Comment> comments = list(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getArticleId, articleId)
                .orderByAsc(Comment::getCreatedAt));

        List<Long> userIds = comments.stream().map(Comment::getUserId).distinct().collect(Collectors.toList());
        if (!userIds.isEmpty()) {
            Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
            comments.forEach(c -> {
                User user = userMap.get(c.getUserId());
                if (user != null) {
                    // 优先显示昵称，若无昵称则回退到账号
                    String displayName = (user.getNickname() != null && !user.getNickname().isBlank())
                            ? user.getNickname() : user.getUsername();
                    c.setUsername(displayName);
                    c.setAvatar(user.getAvatar());
                }
            });
        }
        return comments;
    }

    @Override
    public void likeComment(Long commentId, Long userId) {
        CommentLike existing = commentLikeMapper.selectOne(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId)
                .eq(CommentLike::getUserId, userId));

        Comment comment = getById(commentId);
        if (existing != null) {
            commentLikeMapper.delete(new LambdaQueryWrapper<CommentLike>()
                    .eq(CommentLike::getCommentId, commentId)
                    .eq(CommentLike::getUserId, userId));
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
        } else {
            CommentLike like = new CommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            like.setCreatedAt(DateUtil.now());
            commentLikeMapper.insert(like);
            comment.setLikeCount(comment.getLikeCount() + 1);
        }
        updateById(comment);
    }

    @Override
    public void unlikeComment(Long commentId, Long userId) {
        commentLikeMapper.delete(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId)
                .eq(CommentLike::getUserId, userId));

        Comment comment = getById(commentId);
        comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
        updateById(comment);
    }
}
