package uk.ac.ebi.tsc.portal.api.deployment.repo;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@Entity
@Table(name="dep_app_cloud_provider_output")
public class DeploymentApplicationCloudProviderOutput {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public String name;

    @ManyToOne
    public DeploymentApplicationCloudProvider depAppCloudProvider;

    DeploymentApplicationCloudProviderOutput() { // jpa only
    }

    public DeploymentApplicationCloudProviderOutput(String name, DeploymentApplicationCloudProvider deploymentApplicationCloudProvider) {
        this.name = name;
        this.depAppCloudProvider = deploymentApplicationCloudProvider;
    }

    public String getName() {
        return name;
    }

	public DeploymentApplicationCloudProvider getDeploymentApplicationCloudProvider() {
		return depAppCloudProvider;
	}

	public void setDeploymentApplicationCloudProvider(
			DeploymentApplicationCloudProvider deploymentApplicationCloudProvider) {
		this.depAppCloudProvider = deploymentApplicationCloudProvider;
	}

}
