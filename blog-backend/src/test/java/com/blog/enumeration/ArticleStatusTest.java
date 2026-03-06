package com.blog.enumeration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ArticleStatus 枚举测试
 */
class ArticleStatusTest {

    @Test
    void fromCode_WithValidCode_ShouldReturnEnum() {
        // When
        ArticleStatus draft = ArticleStatus.fromCode("draft");
        ArticleStatus published = ArticleStatus.fromCode("published");
        ArticleStatus archived = ArticleStatus.fromCode("archived");

        // Then
        assertEquals(ArticleStatus.DRAFT, draft);
        assertEquals(ArticleStatus.PUBLISHED, published);
        assertEquals(ArticleStatus.ARCHIVED, archived);
    }

    @Test
    void fromCode_WithInvalidCode_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            ArticleStatus.fromCode("invalid");
        });
    }

    @Test
    void isValid_WithValidCode_ShouldReturnTrue() {
        // When & Then
        assertTrue(ArticleStatus.isValid("draft"));
        assertTrue(ArticleStatus.isValid("published"));
        assertTrue(ArticleStatus.isValid("archived"));
    }

    @Test
    void isValid_WithInvalidCode_ShouldReturnFalse() {
        // When & Then
        assertFalse(ArticleStatus.isValid("invalid"));
        assertFalse(ArticleStatus.isValid(null));
        assertFalse(ArticleStatus.isValid(""));
    }

    @Test
    void getCode_ShouldReturnCorrectValue() {
        // When & Then
        assertEquals("draft", ArticleStatus.DRAFT.getCode());
        assertEquals("published", ArticleStatus.PUBLISHED.getCode());
        assertEquals("archived", ArticleStatus.ARCHIVED.getCode());
    }

    @Test
    void getDescription_ShouldReturnCorrectValue() {
        // When & Then
        assertEquals("草稿", ArticleStatus.DRAFT.getDescription());
        assertEquals("已发布", ArticleStatus.PUBLISHED.getDescription());
        assertEquals("已归档", ArticleStatus.ARCHIVED.getDescription());
    }
}
