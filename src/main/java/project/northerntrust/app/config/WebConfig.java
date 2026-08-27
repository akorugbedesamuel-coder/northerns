package project.northerntrust.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // classpath:/static/usa/ is populated at build time from project /usa (see pom.xml)
        registry.addResourceHandler("/usa/**")
                .addResourceLocations("classpath:/static/usa/");

        // HTML files reference scripts/styles at root (e.g. <script src="config.js">).
        // Serve usa/ assets at root so they resolve without the /usa/ prefix.
        registry.addResourceHandler("/*.js", "/*.css", "/*.svg", "/*.png", "/*.jpg", "/*.ico", "/*.woff", "/*.woff2")
                .addResourceLocations("classpath:/static/usa/", "classpath:/static/");
        registry.addResourceHandler("/public/**")
                .addResourceLocations("classpath:/static/usa/public/", "classpath:/static/public/");
    }
}
