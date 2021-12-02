package com.itheima.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@WebFilter("/*")
public class CheckLoginLogoutFilter implements Filter {


    //放行的请求uri
    private static final String[] NO_CHECK_URIS = new String[]{
            "/backend/**",
            "/front/**",
            "/employee/login",
            "/common/**",
            "/employee/logout",
            "/user/sendMsg",
            "/user/login"
    };
    //匹配器
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    //拦截所有带有异步请求的操作
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        //判断是否与应该放行的资源匹配
        String requestURI = req.getRequestURI();
        boolean flag = matchUri(requestURI);
        //匹配
        if (!flag){
            chain.doFilter(req,resp);
            return;
        }
        //检查session中是否有数据
        Long id = (Long) req.getSession().getAttribute("employeeId");
        if ( id != null){//session存在放行
            BaseContext.setThreadLocal(id);
            chain.doFilter(req,resp);
            BaseContext.removeThreadLocal();
            return;
        }
        if(req.getSession().getAttribute("user") != null){
            log.info("该用户已经登录, 执行放行");
            //获取当前登录用户ID, 存入ThreadLocal
            Long currentId = (Long) req.getSession().getAttribute("user");
            BaseContext.setThreadLocal(currentId);
            chain.doFilter(req,resp);//放行
            //删除ThreadLocal中的数据
            BaseContext.removeThreadLocal();
            return;
        }
        //返回NOTLOGIN字符串提示没有登录
        log.info("用户没有登录");
        resp.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
    }

    //检查是否匹配
    private boolean matchUri(String requestURI) {
        for (String noCheckUris : NO_CHECK_URIS) {
            if (PATH_MATCHER.match(noCheckUris,requestURI)){
                return false;//false代表需要放行
            }
        }
        return true;//true表示需要校验
    }
}
