package com.example.kartu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Serves the React build from classpath:/static and hands every other page path to
 * index.html, so React Router decides what to show (including its 404 page) — no list of
 * SPA routes to keep in sync. Unknown /api/** paths and missing files (anything with a
 * dot, e.g. .js) still answer 404 instead of returning HTML.
 */
@Configuration
public class SpaFallbackConfig implements WebMvcConfigurer {

    private static final Resource INDEX = new ClassPathResource("/static/index.html");

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String path, Resource location) throws IOException {
                        Resource file = location.createRelative(path);
                        if (file.exists() && file.isReadable()) {
                            return file;
                        }
                        boolean pagePath = !path.startsWith("api/") && !path.contains(".");
                        return pagePath && INDEX.exists() ? INDEX : null;
                    }
                });
    }
}
