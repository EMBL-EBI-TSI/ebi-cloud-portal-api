package uk.ac.ebi.tsc.portal.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import uk.ac.ebi.tsc.portal.security.EcpAuthenticationService;
import uk.ac.ebi.tsc.portal.security.StatelessAuthenticationFilter;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@Configuration
@ComponentScan(basePackages = {"uk.ac.ebi.tsc.portal.api", "uk.ac.ebi.tsc.portal.security"})
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebAuthorizationConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private EcpAuthenticationService tokenAuthenticationService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers( "/ping"
                            , "/deployment/done/**"
                            , "/deployment/**/stopme"
                            )
                            .permitAll()
                .antMatchers(HttpMethod.PUT, "/deployment/**/outputs").permitAll()
                .antMatchers(HttpMethod.POST, "/cloudproviderparameters").authenticated()
                .antMatchers(HttpMethod.GET, "/cloudproviderparameters").authenticated()
//                .antMatchers(HttpMethod.DELETE, "/cloudproviderparameters/**").authenticated()
                .antMatchers(HttpMethod.OPTIONS,"/**").permitAll() //allow CORS OPTIONS calls
                .anyRequest().authenticated()
                .and()
            .httpBasic()
                .and()
            .csrf().disable();
        // Custom Token based authentication based on the header previously given to the client
        http.addFilterBefore(new StatelessAuthenticationFilter(tokenAuthenticationService),
                        BasicAuthenticationFilter.class);
    }
}
