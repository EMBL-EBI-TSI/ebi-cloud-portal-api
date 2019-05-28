package uk.ac.ebi.tsc.portal.clouddeployment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.ebi.tsc.portal.api.application.repo.ApplicationDeploymentParameter;
import uk.ac.ebi.tsc.portal.api.application.repo.ApplicationInput;

import java.util.Collection;

import org.springframework.stereotype.Component;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Component
public class ApplicationManifest {

    @JsonProperty
    public String applicationName;

    @JsonProperty
    public String about;

    @JsonProperty
    public String contactEmail;

    @JsonProperty
    public String version;

    @JsonProperty
    public Collection<CloudProvider> cloudProviders;

    @JsonProperty
    public ImageSource imageSource;

    @JsonProperty
    public Collection<String> volumes;

    @JsonProperty
    public Collection<ApplicationInput> inputs;

    @JsonProperty
    public Collection<String> outputs;
    
    @JsonProperty
    public Collection<String> deploymentParameters;

}
