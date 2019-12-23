package uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.team.repo.Team;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@Entity
public class CloudProviderParameters {

	static final long serialVersionUID = 1L;
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public String name;

    public String cloudProvider;

    @ManyToOne
    @JoinColumn (name = "account_id",referencedColumnName = "id")
    public Account account;

    @OneToMany(mappedBy = "cloudProviderParameters", cascade = CascadeType.ALL, orphanRemoval=true)
    public Collection<CloudProviderParametersField> fields;

    @ManyToMany
    @JoinTable(name="shared_cloud_provider_parameters",
            joinColumns=@JoinColumn(name="cloud_provider_parameters_id", referencedColumnName="id"),
            inverseJoinColumns=@JoinColumn(name="account_id", referencedColumnName="id")
    )
    public Set<Account> sharedWith = new HashSet<>();
    
    @ManyToMany(mappedBy="cppBelongingToTeam")
    private Set<Team> sharedWithTeams = new HashSet<>();
    
    @NotNull
    @Column(unique=true)
    public String reference;

    CloudProviderParameters() { // jpa only
    }

    public CloudProviderParameters(String name, String cloudProvider, Account account) {
        this.name = name;
        this.cloudProvider = cloudProvider;
        this.account = account;
        this.fields = new LinkedList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getCloudProvider() {
        return cloudProvider;
    }

    public Account getAccount() {
        return account;
    }

    public Collection<CloudProviderParametersField> getFields() {
        return fields;
    }

    public Set<Account> getSharedWith() {
        return sharedWith;
    }

    public void setSharedWith(Set<Account> sharedWith) {
        this.sharedWith = sharedWith;
    }
    
    public Set<Team> getSharedWithTeams() {
		return sharedWithTeams;
	}

	public void setSharedWithTeams(Set<Team> sharedWithTeams) {
		this.sharedWithTeams = sharedWithTeams;
	}
	
	public void setFields(Collection<CloudProviderParametersField> fields) {
		this.fields = fields;
	}

	public void setCloudProvider(String cloudProvider) {
		this.cloudProvider = cloudProvider;
	}

	@PreRemove
	private void removeCloudProviderParameterFromTeam(){
		for(Team team: sharedWithTeams ){
			team.getCppBelongingToTeam().remove(this);
		}
	}

    @Transient
    public Set<String> getSharedTeamNames(){
        return  getSharedWithTeams().stream().map(Team::getName).collect(Collectors.toSet());
    }

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}
	
	
}
