package uk.ac.ebi.tsc.portal.api.application.repo;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PreRemove;

import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.team.repo.Team;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@Entity
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Account account;

    public String repoUri;

    @Column(unique=true)
    public String repoPath;

    public String name;

    public String about;

    public String contact;

    public String version;

    private String reference;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL)
    public Collection<ApplicationCloudProvider> cloudProviders;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL)
    public Collection<ApplicationDeploymentParameter> deploymentParameters;

    @OneToMany(fetch=FetchType.EAGER, mappedBy = "application", cascade = CascadeType.ALL)
    public Collection<ApplicationOutput> outputs;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL)
    public Collection<ApplicationVolume> volumes;
    
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL)
    public Collection<ApplicationInput> inputs;
    
    @ManyToMany
    @JoinTable(name="shared_application",
            joinColumns=@JoinColumn(name="application_id", referencedColumnName="id"),
            inverseJoinColumns=@JoinColumn(name="account_id", referencedColumnName="id")
    )
    public Set<Account> sharedApplicationWith = new HashSet<>();
    
    @ManyToMany(mappedBy="applicationsBelongingToTeam")
    private Set<Team> sharedWithTeams = new HashSet<>();
    
    Application() { // jpa only
    }

    public Application(String repoUri, String repoPath, String name, String reference, Account account) {
        this.repoUri = repoUri;
        this.repoPath = repoPath;
        this.name = name;
        this.reference = reference;
        this.account = account;
        this.cloudProviders = new LinkedList<>();
        this.inputs = new LinkedList<>();
        this.outputs = new LinkedList<>();
        this.volumes = new LinkedList<>();
        this.deploymentParameters = new LinkedList<>();
    }

    public Long getId() {
        return id;
    }

    public String getRepoUri() {
        return repoUri;
    }

    public String getRepoPath() {
        return repoPath;
    }

    public String getName() {
        return name;
    }

    public Account getAccount() {
        return account;
    }

    public Collection<ApplicationCloudProvider> getCloudProviders() {
        return cloudProviders;
    }

    public Collection<ApplicationInput> getInputs() {
        return inputs;
    }

    public Collection<ApplicationDeploymentParameter> getDeploymentParameters() {
        return deploymentParameters;
    }
    
    public void setDeploymentParameters(Collection<ApplicationDeploymentParameter> deploymentParameters) {
		this.deploymentParameters = deploymentParameters;
	}

	public Collection<ApplicationOutput> getOutputs() {
        return outputs;
    }

    public Collection<ApplicationVolume> getVolumes() {
        return volumes;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

	public Set<Account> getSharedApplicationWith() {
		return sharedApplicationWith;
	}

	public void setSharedApplicationWith(Set<Account> sharedApplicationWith) {
		this.sharedApplicationWith = sharedApplicationWith;
	}

	public Set<Team> getSharedWithTeams() {
		return sharedWithTeams;
	}

	public void setSharedWithTeams(Set<Team> sharedWithTeams) {
		this.sharedWithTeams = sharedWithTeams;
	}
	
	
	public void setRepoUri(String repoUri) {
		this.repoUri = repoUri;
	}

    public String getReference() {
        return reference;
    }

    @PreRemove
	private void removeApplicationFromTeam(){
		for(Team team: sharedWithTeams ){
			team.getApplicationsBelongingToTeam().remove(this);
		}
	}
}
