package com.blog.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 文章发布事件
 * 用于解耦文章发布和通知粉丝的逻辑
 */
@Getter
public class ArticlePublishedEvent extends ApplicationEvent {

    private final Long articleId;
    private final Long authorId;
    private final String articleTitle;

    public ArticlePublishedEvent(Object source, Long articleId, Long authorId, String articleTitle) {
        super(source);
        this.articleId = articleId;
        this.authorId = authorId;
        this.articleTitle = articleTitle;
    }
}
