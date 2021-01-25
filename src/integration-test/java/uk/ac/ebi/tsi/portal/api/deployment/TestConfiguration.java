package uk.ac.ebi.tsi.portal.api.deployment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfiguration {

    CustomUserDetailsService  customUserDetailsService;

    WithUserDetailsSecurityContextFactory withUserDetailsSecurityContextFactory;

    @Bean
    public CustomUserDetailsService  customUserDetailsService(){
        return customUserDetailsService;
    }

    @Bean
    public WithUserDetailsSecurityContextFactory withUserDetailsSecurityContextFactory(){
        return withUserDetailsSecurityContextFactory;
    }

}
