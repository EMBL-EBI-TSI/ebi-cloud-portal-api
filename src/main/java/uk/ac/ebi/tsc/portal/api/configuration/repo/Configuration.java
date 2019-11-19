package uk.ac.ebi.tsc.portal.api.configuration.repo;

import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.team.repo.Team;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@Entity
public class Configuration {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	public String name;

	public String sshKey;

	@ManyToOne
	@JoinColumn (name = "account_id", referencedColumnName = "id")
	public Account account;

	public String cloudProviderParametersName;

	@ManyToMany(mappedBy="configurationsBelongingToTeam")
	private Set<Team> sharedWithTeams = new HashSet<>();

	private String reference;

	private Double softUsageLimit;

	private Double hardUsageLimit;

    @NotNull
    @Column(name="cloud_provider_params_reference")
    private String cloudProviderParametersReference;
    
    public String configDeploymentParamsName;
    
    @NotNull
    @Column(name="config_deployment_params_reference")
    private String configDeployParamsReference;
    

	public Configuration() { // jpa only
	}

	public Configuration(String name, Account account,
			String cloudProviderParametersName, 
			String cloudProviderParametersReference,
			String sshKey,
            Double softUsageLimit,
            Double hardUsageLimit,
			ConfigDeploymentParamsCopy configDeploymentParamsCopy) {
		this.name = name;
		this.sshKey = sshKey;
		this.softUsageLimit = softUsageLimit;
		this.hardUsageLimit = hardUsageLimit;
		this.cloudProviderParametersName = cloudProviderParametersName;
		this.cloudProviderParametersReference = cloudProviderParametersReference;
		this.account = account;
		this.configDeployParamsReference = configDeploymentParamsCopy.getConfigurationDeploymentParametersReference();
		this.configDeploymentParamsName = configDeploymentParamsCopy.getName();
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getSshKey() {
		return sshKey;
	}

	public String getCloudProviderParametersName() {
		return cloudProviderParametersName;
	}

	public Account getAccount() {
		return account;
	}

	public Set<Team> getSharedWithTeams() {
		return sharedWithTeams;
	}

	public void setSharedWithTeams(Set<Team> sharedWithTeams) {
		this.sharedWithTeams = sharedWithTeams;
	}
	
	public void setSshKey(String sshKey) {
		this.sshKey = sshKey;
	}

	public void setCloudProviderParametersName(String cloudProviderParametersName) {
		this.cloudProviderParametersName = cloudProviderParametersName;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public String getCloudProviderParametersReference() {
		return cloudProviderParametersReference;
	}

	public void setCloudProviderParametersReference(String cloudProviderParametersReference) {
		this.cloudProviderParametersReference = cloudProviderParametersReference;
	}
	
	public String getConfigDeployParamsReference() {
		return configDeployParamsReference;
	}

	public void setConfigDeployParamsReference(String configDeployParamsReference) {
		this.configDeployParamsReference = configDeployParamsReference;
	}
	
	public String getConfigDeploymentParamsName() {
		return configDeploymentParamsName;
	}

	public void setConfigDeploymentParamsName(String configDeploymentParamsName) {
		this.configDeploymentParamsName = configDeploymentParamsName;
	}

	@PreRemove
	private void removeConfigurationFromTeam(){
		for(Team team: sharedWithTeams ){
			team.getConfigurationsBelongingToTeam().remove(this);
		}
	}

	@Transient
	public Set<String> getSharedTeamNames(){
		return  getSharedWithTeams().stream().map(Team::getName).collect(Collectors.toSet());
	}

	public Double getSoftUsageLimit() {
        return softUsageLimit;
    }

    public void setSoftUsageLimit(Double softUsageLimit) {
        this.softUsageLimit = softUsageLimit;
    }

    public Double getHardUsageLimit() {
        return hardUsageLimit;
    }

    public void setHardUsageLimit(Double hardUsageLimit) {
        this.hardUsageLimit = hardUsageLimit;
    }
}
