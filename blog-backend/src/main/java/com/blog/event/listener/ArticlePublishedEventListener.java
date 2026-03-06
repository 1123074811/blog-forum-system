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
        log.info("处理文章发布事件，文章ID: {}, 作者ID: {}", event.getArticleId(), event.getAuthorId());

        try {
            // 查询作者的所有粉丝
            List<Follow> followers = followMapper.selectList(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFollowingId, event.getAuthorId())
            );

            if (followers.isEmpty()) {
                log.debug("作者 {} 没有粉丝，跳过通知", event.getAuthorId());
                return;
            }

            // 批量发送通知
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
                    log.error("发送通知失败，粉丝ID: {}", follow.getFollowerId(), e);
                }
            }

            log.info("文章发布通知完成，通知 {} 位粉丝", notifyCount);

        } catch (Exception e) {
            log.error("处理文章发布事件失败", e);
        }
    }
}
