package br.com.siecola.productsservice.products.interceptors;

import br.com.siecola.productsservice.products.controllers.ProductsController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
public class ProductsInterceptor implements HandlerInterceptor {
    private static final Logger LOG = LogManager.getLogger(ProductsInterceptor.class);
    @Override//executed right before the controller
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        ThreadContext.put("requestId", request.getHeader("requestId"));
        Map<String,String> headerMaps = new HashMap<>();
        Iterator<String> iterator = request.getHeaderNames().asIterator();
        while(iterator.hasNext()) {
            String next = iterator.next();
            headerMaps.put(next, request.getHeader(next));
        }
       // LOG.info("all headers={}", headerMaps);
        //inject request id to all logs
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) throws Exception {
        ThreadContext.clearAll();
    }
}
