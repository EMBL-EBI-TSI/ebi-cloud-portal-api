package uk.ac.ebi.tsc.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ecp.roles.deployments")
public class ECPApplicationProperties {

    private String view;

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }
}
