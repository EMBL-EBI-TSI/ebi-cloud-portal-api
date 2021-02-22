
package uk.ac.ebi.tsc.portal.api.team.repo;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.application.repo.Application;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParameters;
import uk.ac.ebi.tsc.portal.api.configuration.repo.Configuration;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationDeploymentParameters;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@Entity
public class Team {
	
	public Team(){}
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    private String domainReference;
    
    @ManyToOne
    @JoinColumn (name = "owner_account_id",referencedColumnName = "id")
    public Account account;

	@LazyCollection(LazyCollectionOption.FALSE)
    @ManyToMany
    @JoinTable(name="account_team",
            joinColumns=@JoinColumn(name="team_id", referencedColumnName="id"),
            inverseJoinColumns=@JoinColumn(name="account_id", referencedColumnName="id")
    )
    public Set<Account> accountsBelongingToTeam = new HashSet<>();

    @ManyToMany
    @JoinTable(name="team_shared_applications",
            joinColumns=@JoinColumn(name="team_id", referencedColumnName="id"),
            inverseJoinColumns=@JoinColumn(name="application_id", referencedColumnName="id")
    )
    public Set<Application> applicationsBelongingToTeam = new HashSet<>();

    @ManyToMany
    @JoinTable(name="team_shared_cloud_provider_parameters",
            joinColumns=@JoinColumn(name="team_id", referencedColumnName="id"),
            inverseJoinColumns=@JoinColumn(name="cloud_provider_parameters_id", referencedColumnName="id")
    )
    public Set<CloudProviderParameters> cppBelongingToTeam = new HashSet<>();

    @ManyToMany
    @JoinTable(name="team_shared_config_dep_params",
            joinColumns=@JoinColumn(name="team_id", referencedColumnName="id"),
            inverseJoinColumns=@JoinColumn(name="config_deploy_params_id", referencedColumnName="id")
    )
    public Set<ConfigurationDeploymentParameters> configDepParamsBelongingToTeam = new HashSet<>();

    @ManyToMany
    @JoinTable(name="team_shared_configurations",
            joinColumns=@JoinColumn(name="team_id", referencedColumnName="id"),
            inverseJoinColumns=@JoinColumn(name="configuration_id", referencedColumnName="id")
    )
    public Set<Configuration> configurationsBelongingToTeam = new HashSet<>();

	@LazyCollection(LazyCollectionOption.FALSE)
	@ElementCollection
	@CollectionTable(
			name="team_contact_emails",
			joinColumns=@JoinColumn(name="team_id")
	)
	@Column(name="email")
	private Set<String> teamContactEmails = new HashSet<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public Set<Account> getAccountsBelongingToTeam() {
		return accountsBelongingToTeam;
	}

	public void setAccountsBelongingToTeam(Set<Account> accountsBelongingToTeam) {
		this.accountsBelongingToTeam = accountsBelongingToTeam;
	}

	public Set<Application> getApplicationsBelongingToTeam() {
		return applicationsBelongingToTeam;
	}

	public void setApplicationsBelongingToTeam(Set<Application> applicationsBelongingToTeam) {
		this.applicationsBelongingToTeam = applicationsBelongingToTeam;
	}

	public Set<CloudProviderParameters> getCppBelongingToTeam() {
		return cppBelongingToTeam;
	}

	public void setCppBelongingToTeam(Set<CloudProviderParameters> cppBelongingToTeam) {
		this.cppBelongingToTeam = cppBelongingToTeam;
	}

	public Set<Configuration> getConfigurationsBelongingToTeam() {
		return configurationsBelongingToTeam;
	}

	public void setConfigurationsBelongingToTeam(Set<Configuration> configurationsBelongingToTeam) {
		this.configurationsBelongingToTeam = configurationsBelongingToTeam;
	}

	public Set<ConfigurationDeploymentParameters> getConfigDepParamsBelongingToTeam() {
		return configDepParamsBelongingToTeam;
	}

	public void setConfigDepParamsBelongingToTeam(Set<ConfigurationDeploymentParameters> configDepParamsBelongingToTeam) {
		this.configDepParamsBelongingToTeam = configDepParamsBelongingToTeam;
	}

	public String getDomainReference() {
		return domainReference;
	}

	public void setDomainReference(String domainReference) {
		this.domainReference = domainReference;
	}

	public Set<String> getTeamContactEmails() {
		return teamContactEmails;
	}

	public void setTeamContactEmails(Set<String> teamContactEmails) {
		this.teamContactEmails = teamContactEmails;
	}
}
