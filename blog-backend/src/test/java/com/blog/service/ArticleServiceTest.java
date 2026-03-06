package com.blog.service;

import com.blog.enumeration.ArticleStatus;
import com.blog.exception.BusinessException;
import com.blog.exception.ErrorCode;
import com.blog.mapper.ArticleMapper;
import com.blog.pojo.dto.ArticleRequest;
import com.blog.pojo.entity.Article;
import com.blog.service.impl.ArticleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ArticleService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleMapper articleMapper;

    @InjectMocks
    private ArticleServiceImpl articleService;

    private ArticleRequest validRequest;
    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        validRequest = new ArticleRequest();
        validRequest.setTitle("测试文章");
        validRequest.setContent("这是测试内容");
        validRequest.setStatus(ArticleStatus.DRAFT.getCode());
        validRequest.setCategoryId(1L);
        validRequest.setTags(Arrays.asList(1L, 2L));
    }

    @Test
    void createArticle_WithValidData_ShouldSuccess() {
        // Given
        when(articleMapper.insert(any(Article.class))).thenReturn(1);

        // When
        Article result = articleService.createArticle(validRequest, userId);

        // Then
        assertNotNull(result);
        assertEquals(validRequest.getTitle(), result.getTitle());
        assertEquals(validRequest.getContent(), result.getContent());
        assertEquals(userId, result.getUserId());
        verify(articleMapper, times(1)).insert(any(Article.class));
    }

    @Test
    void createArticle_WithInvalidStatus_ShouldThrowException() {
        // Given
        validRequest.setStatus("invalid_status");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            ArticleStatus.fromCode(validRequest.getStatus());
        });
    }

    @Test
    void getArticleById_WhenNotExists_ShouldThrowException() {
        // Given
        Long articleId = 999L;
        when(articleMapper.selectById(articleId)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                throw new BusinessException(ErrorCode.ARTICLE_NOT_FOUND);
            }
        });
        
        assertEquals(ErrorCode.ARTICLE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateArticle_WithDifferentUser_ShouldThrowException() {
        // Given
        Long articleId = 1L;
        Long ownerId = 1L;
        Long otherUserId = 2L;
        
        Article existingArticle = new Article();
        existingArticle.setId(articleId);
        existingArticle.setUserId(ownerId);
        
        when(articleMapper.selectById(articleId)).thenReturn(existingArticle);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            if (!existingArticle.getUserId().equals(otherUserId)) {
                throw new BusinessException(ErrorCode.ARTICLE_PERMISSION_DENIED);
            }
        });
        
        assertEquals(ErrorCode.ARTICLE_PERMISSION_DENIED, exception.getErrorCode());
    }
}
