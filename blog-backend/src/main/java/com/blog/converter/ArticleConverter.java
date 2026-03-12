package com.blog.converter;

import com.blog.pojo.entity.Article;
import com.blog.pojo.vo.ArticleVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文章实体与VO转换器
 */
@Component
public class ArticleConverter {

    public ArticleVO toVO(Article article) {
        if (article == null) {
            return null;
        }
        ArticleVO vo = new ArticleVO();
        vo.setId(article.getId());
        vo.setUserId(article.getUserId());
        vo.setTitle(article.getTitle());
        vo.setContent(article.getContent());
        vo.setCategoryId(article.getCategoryId());
        vo.setStatus(article.getStatus());
        vo.setViewCount(article.getViewCount());
        vo.setLikeCount(article.getLikeCount());
        vo.setLiked(article.getLiked());
        vo.setHotScore(article.getHotScore());
        vo.setCreatedAt(article.getCreatedAt());
        vo.setUpdatedAt(article.getUpdatedAt());
        vo.setAuthorName(article.getAuthorName());
        vo.setAuthorAvatar(article.getAuthorAvatar());
        vo.setTags(article.getTags());
        return vo;
    }

    public List<ArticleVO> toVOList(List<Article> articles) {
        if (articles == null) {
            return null;
        }
        return articles.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }
}
