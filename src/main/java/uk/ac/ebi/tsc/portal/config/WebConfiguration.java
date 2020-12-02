package uk.ac.ebi.tsc.portal.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@Configuration
public class WebConfiguration extends WebMvcConfigurerAdapter {

    @Value("#{'${be.friendly.origins}'.split(',')}")
    List<String> origins;

    @Value("${ecp.aai.authority}")
    public String adminAuthority;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(origins.toArray(new String[0]))
                .allowedMethods("*", "OPTIONS", "PUT")
                .allowedHeaders("accept", "authorization", "origin", "content-type", "x-requested-with")
                .allowCredentials(true).maxAge(3600);
    }
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
       return builder.build();
    }
    
    @Bean
    public String elasticindexurl(@Value("${elasticsearch.url}") String elasticindexurl) {
       return elasticindexurl;
    }
    
    @Bean
    public String elasticsearchusername(@Value("${elasticsearch.username}") String elasticsearchusername) {
       return elasticsearchusername;
    }
    
    @Bean
    public String elasticsearchpassword(@Value("${elasticsearch.password}") String elasticsearchpassword) {
       return elasticsearchpassword;
    }

    @Bean
    public String adminAuthority(){
        return adminAuthority;
    }

}
