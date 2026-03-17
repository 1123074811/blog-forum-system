package com.blog.event.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.event.ArticlePublishedEvent;
import com.blog.mapper.FollowMapper;
import com.blog.pojo.entity.Follow;
import com.blog.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文章发布事件监听器
 * 异步处理文章发布后的通知逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticlePublishedEventListener {

    private final FollowMapper followMapper;
    private final NotificationService notificationService;

    /**
     * 监听文章发布事件，通知所有粉丝
     */
    @Async
    @EventListener
    public void handleArticlePublished(ArticlePublishedEvent event) {
        try {
            List<Follow> followers = followMapper.selectList(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFollowingId, event.getAuthorId())
            );

            if (followers.isEmpty()) return;

            int notifyCount = 0;
            for (Follow follow : followers) {
                try {
                    notificationService.send(
                        follow.getFollowerId(),
                        event.getAuthorId(),
                        "article_published",
                        event.getArticleId(),
                        "发布了新文章：" + event.getArticleTitle()
                    );
                    notifyCount++;
                } catch (Exception e) {
                    log.error("通知粉丝{}失败", follow.getFollowerId(), e);
                }
            }

            log.debug("文章{}发布通知完成，共{}位粉丝", event.getArticleId(), notifyCount);

        } catch (Exception e) {
            log.error("处理文章发布事件失败, articleId={}", event.getArticleId(), e);
        }
    }
}
