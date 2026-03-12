package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.exception.BusinessException;
import com.blog.exception.ErrorCode;
import com.blog.pojo.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * BaseController 测试
 */
class BaseControllerTest {

    private static class TestController extends BaseController {
        // 测试用的Controller实现
    }

    private final TestController controller = new TestController();

    @Test
    void getCurrentUserId_WithValidAuth_ShouldReturnUserId() {
        // Given
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(1L);

        // When
        Long userId = controller.getCurrentUserId(auth);

        // Then
        assertEquals(1L, userId);
    }

    @Test
    void getCurrentUserId_WithNullAuth_ShouldThrowException() {
        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            controller.getCurrentUserId(null);
        });
        
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void getCurrentUserIdOptional_WithNullAuth_ShouldReturnNull() {
        // When
        Long userId = controller.getCurrentUserIdOptional(null);

        // Then
        assertNull(userId);
    }

    @Test
    void checkPermission_WithSameUser_ShouldNotThrowException() {
        // When & Then
        assertDoesNotThrow(() -> {
            controller.checkPermission(1L, 1L);
        });
    }

    @Test
    void checkPermission_WithDifferentUser_ShouldThrowException() {
        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            controller.checkPermission(1L, 2L);
        });
        
        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void toPageResponse_ShouldConvertCorrectly() {
        // Given
        Page<String> page = new Page<>(1, 10);
        List<String> records = Arrays.asList("item1", "item2", "item3");
        page.setRecords(records);
        page.setTotal(3);

        // When
        PageResponse<String> response = controller.toPageResponse(page);

        // Then
        assertEquals(records, response.getData());
        assertEquals(3, response.getTotal());
        assertEquals(1, response.getPage());
        assertEquals(10, response.getLimit());
    }
}
