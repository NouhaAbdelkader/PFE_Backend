package com.example.pfe_backend.Configurations;


import org.apache.catalina.core.StandardContext;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addContextCustomizers(context -> {
            if (context instanceof StandardContext standardContext) {
                // Prevent JSP servlet registration
                standardContext.setAddWebinfClassesResources(false);
                // Disable JSP processing
                standardContext.addParameter("org.apache.jasper.servlet.JspServlet", "false");
            }
        });
    }
}