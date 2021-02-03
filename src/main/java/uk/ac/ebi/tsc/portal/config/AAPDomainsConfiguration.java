package uk.ac.ebi.tsc.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ecp.aap.domains")
public class AAPDomainsConfiguration {

    private Deployments deployments;

    public Deployments getDeployments() {
        return deployments;
    }

    public void setDeployments(Deployments deployments) {
        this.deployments = deployments;
    }

    public static class Deployments {

        private String view;

        public String getView() {
            return view;
        }

        public void setView(String view) {
            this.view = view;
        }
    }

}
