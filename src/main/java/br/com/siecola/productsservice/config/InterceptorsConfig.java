package br.com.siecola.productsservice.config;

import br.com.siecola.productsservice.products.interceptors.ProductsInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorsConfig implements WebMvcConfigurer {

    private final ProductsInterceptor productsInterceptor;

    @Autowired
    public InterceptorsConfig(ProductsInterceptor productsInterceptor) {
        this.productsInterceptor = productsInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this.productsInterceptor);
               // .addPathPatterns("/api/products/**");//merge doar pe get all request id, ceva nu e bine dar e bine dar merg mai departe
        //asa ca sterge
    }
}
