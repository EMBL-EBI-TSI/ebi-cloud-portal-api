package uk.ac.ebi.tsc.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@SpringBootApplication
@ComponentScan(basePackages = {"uk.ac.ebi.tsc.portal", "uk.ac.ebi.tsc.aap.client.repo","uk.ac.ebi.cloud.portal.service"})
@EnableJpaRepositories(basePackages = {"uk.ac.ebi.tsc.portal", "uk.ac.ebi.tsc.aap.client", "uk.ac.ebi.cloud.portal.service"})
public class BePortalApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(BePortalApiApplication.class, args);
    }

}


