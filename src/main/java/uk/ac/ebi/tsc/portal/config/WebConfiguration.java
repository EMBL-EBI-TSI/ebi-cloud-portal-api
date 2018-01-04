package uk.ac.ebi.tsc.portal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 */
@Configuration
public class WebConfiguration extends WebMvcConfigurerAdapter {

    @Value("#{'${be.friendly.origins}'.split(',')}")
    List<String> origins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(origins.toArray(new String[0]))
                .allowedMethods("*", "OPTIONS", "PUT")
                .allowedHeaders("accept", "authorization", "origin", "content-type", "x-requested-with")
                .allowCredentials(true).maxAge(3600);
    }


}
