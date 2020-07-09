package uk.ac.ebi.tsc.portal.api.configuration.controller;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;

import uk.ac.ebi.tsc.portal.api.account.controller.AccountRestController;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParamsCopy;
import uk.ac.ebi.tsc.portal.api.configuration.repo.Configuration;
import uk.ac.ebi.tsc.portal.api.team.controller.TeamResource;
import uk.ac.ebi.tsc.portal.api.team.repo.Team;


/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
public class ConfigurationResource extends ResourceSupport {

    private Long id;
    private String name;
    private String sshKey;
    private String cloudProviderParametersName;
    private String deploymentParametersName;
    private Collection<String> sharedWithTeamNames;
    private String accountUsername;
    private String accountGivenName;
    private String accountEmail;
    private boolean isObsolete;
    private String cloudProviderParametersReference;
    private String configDeploymentParametersReference;
    private String reference;
    private String cloudProviderType;
    private Double softUsageLimit;
    private Double hardUsageLimit;
    private Collection<TeamResource> sharedWithTeams;
    
    public ConfigurationResource() {
    }

    public ConfigurationResource(Configuration configuration, CloudProviderParamsCopy cppCopy) {

        this.id = configuration.getId();
        this.name = configuration.getName();
        this.accountUsername = configuration.getAccount().getUsername();
        this.accountGivenName = configuration.getAccount().getGivenName();
        this.accountEmail = configuration.getAccount().getEmail();
        this.sshKey = configuration.getSshKey();
        this.cloudProviderParametersName = configuration.getCloudProviderParametersName();
        this.sharedWithTeamNames = configuration.getSharedWithTeams().stream().map(
                Team::getName
        ).collect(Collectors.toList());
        this.cloudProviderParametersReference = configuration.getCloudProviderParametersReference();
        this.configDeploymentParametersReference = configuration.getConfigDeployParamsReference();
        this.deploymentParametersName = configuration.getConfigDeploymentParamsName();
        this.reference = configuration.getReference();
        this.cloudProviderType = cppCopy != null ? cppCopy.getCloudProvider(): null;
        this.softUsageLimit = configuration.getSoftUsageLimit();
        this.hardUsageLimit = configuration.getHardUsageLimit();
        this.sharedWithTeams = configuration.getSharedWithTeams().stream().map(TeamResource::new).collect(Collectors.toList());
        this.isObsolete = this.isObsolete();
        
        this.add(
                ControllerLinkBuilder.linkTo(
                        methodOn(
                                AccountRestController.class
                        ).getAccountByUsername(
                                configuration.getAccount().getUsername()
                        )
                ).withSelfRel()
        );
    }

    public String getName() {
        return name;
    }
    
	public void setName(String name) {
		this.name = name;
	}

    public String getSshKey() {
        return sshKey;
    }

    public String getCloudProviderParametersName() {
        return cloudProviderParametersName;
    }

	public String getDeploymentParametersName() {
		return deploymentParametersName;
	}
	
	public void setDeploymentParametersName(String deploymentParametersName) {
		this.deploymentParametersName = deploymentParametersName;
	}

	public Collection<String> getSharedWithTeamNames() {
		return sharedWithTeamNames;
	}

	public void setSharedWithTeamNames(Collection<String> sharedWithTeamNames) {
		this.sharedWithTeamNames = sharedWithTeamNames;
	}

	public String getAccountUsername() {
		return accountUsername;
	}

	public void setAccountUsername(String accountUsername) {
		this.accountUsername = accountUsername;
	}

	public boolean isObsolete() {
		return isObsolete;
	}

	public void setObsolete(boolean isObsolete) {
		this.isObsolete = isObsolete;
	}

	public void setCloudProviderParametersName(String cloudProviderParametersName) {
		this.cloudProviderParametersName = cloudProviderParametersName;
	}

	public String getCloudProviderParametersReference() {
		return cloudProviderParametersReference;
	}

	public void setCloudProviderParametersReference(String cloudProviderParametersReference) {
		this.cloudProviderParametersReference = cloudProviderParametersReference;
	}

	public String getConfigDeploymentParametersReference() {
		return configDeploymentParametersReference;
	}

	public void setConfigDeploymentParametersReference(String configDeploymentParametersReference) {
		this.configDeploymentParametersReference = configDeploymentParametersReference;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public String getCloudProviderType() {
		return cloudProviderType;
	}

	public void setCloudProviderType(String cloudProviderType) {
		this.cloudProviderType = cloudProviderType;
	}

	public String getAccountGivenName() {
		return accountGivenName;
	}

	public String getAccountEmail() {
		return accountEmail;
	}

	public Double getSoftUsageLimit() {
		return softUsageLimit;
	}

	public Double getHardUsageLimit() {
		return hardUsageLimit;
	}

	public Collection<TeamResource> getSharedWithTeams() {
		return sharedWithTeams;
	}

	public void setSharedWithTeams(Collection<TeamResource> sharedWithTeams) {
		this.sharedWithTeams = sharedWithTeams;
	}

	public void setSshKey(String sshKey) {
		this.sshKey = sshKey;
	}
}
