package com.blog.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.pojo.entity.SiteVisit;
import com.blog.service.SiteVisitService;
import com.blog.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class VisitInterceptor implements HandlerInterceptor {

    private final SiteVisitService siteVisitService;
    private static final ConcurrentHashMap<String, Boolean> todayUvMap = new ConcurrentHashMap<>();
    private static String currentDate = "";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String today = DateUtil.now().substring(0, 10);
        String ip = getClientIp(request);

        // 日期变化时清空UV缓存
        if (!today.equals(currentDate)) {
            todayUvMap.clear();
            currentDate = today;
        }

        // 更新访问统计
        synchronized (this) {
            SiteVisit visit = siteVisitService.getOne(
                new LambdaQueryWrapper<SiteVisit>().eq(SiteVisit::getVisitDate, today));

            if (visit == null) {
                visit = new SiteVisit();
                visit.setVisitDate(today);
                visit.setPv(1);
                visit.setUv(1);
                visit.setNewUsers(0);
                visit.setNewArticles(0);
                visit.setNewComments(0);
                todayUvMap.put(ip, true);
                siteVisitService.save(visit);
            } else {
                visit.setPv(visit.getPv() + 1);
                if (!todayUvMap.containsKey(ip)) {
                    visit.setUv(visit.getUv() + 1);
                    todayUvMap.put(ip, true);
                }
                siteVisitService.updateById(visit);
            }
        }
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip.split(",")[0].trim();
    }
}
