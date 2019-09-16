package uk.ac.ebi.tsc.portal.api.application.controller;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.ebi.tsc.portal.api.account.controller.AccountRestController;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.application.repo.Application;
import uk.ac.ebi.tsc.portal.api.team.repo.Team;

import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */

public class ApplicationResource extends ResourceSupport {

    private Long id;
    private String accountUsername;
    private String accountGivenName;
    private String accountEmail;
    private String repoUri;
    private String name;
    private String about;
    private String contact;
    private String version;
    private String reference;
    private Collection<ApplicationCloudProviderResource> cloudProviders;
    private Collection<ApplicationInputResource> inputs;
    private Collection<String> deploymentParameters;
    private Collection<String> outputs;
    private Collection<String> volumes;
    private Collection<String> sharedWithAccountEmails;
    private Collection<String> sharedWithTeamNames;

    // we need this for usage with the @RequestBody annotation
    public ApplicationResource() {
    }

    public ApplicationResource(Application application) {

        this.id = application.getId();
        this.accountUsername = application.getAccount().getUsername();
        this.accountGivenName = application.getAccount().getGivenName();
        this.accountEmail = application.getAccount().getEmail();
        this.repoUri = application.getRepoUri();
        this.name = application.getName();
        this.cloudProviders = new LinkedList<>();
        this.cloudProviders.addAll(
                application.getCloudProviders().stream().map(ApplicationCloudProviderResource::new).collect(Collectors.toList())
        );
        this.inputs = new LinkedList<>();
        this.inputs.addAll(
                application.getInputs().stream().map(ApplicationInputResource::new).collect(Collectors.toList())
        );
        
        this.deploymentParameters = new LinkedList<>();
        this.deploymentParameters.addAll(
                application.getDeploymentParameters().stream().map(param -> param.name).collect(Collectors.toList())
        );
        
        this.outputs = new LinkedList<>();
        this.outputs.addAll(
                application.getOutputs().stream().map(param -> param.name).collect(Collectors.toList())
        );
        this.volumes = new LinkedList<>();
        this.volumes.addAll(
                application.getVolumes().stream().map(volume -> volume.name).collect(Collectors.toList())
        );
        this.about = application.getAbout();
        this.contact = application.getContact();
        this.version = application.getVersion();
        this.sharedWithAccountEmails = application.getSharedApplicationWith().stream().map(
                Account::getEmail
        ).collect(Collectors.toList());
        this.sharedWithTeamNames = application.getSharedWithTeams().stream().map(
                Team::getName
        ).collect(Collectors.toList());

        this.add(
            linkTo(AccountRestController.class)
                .slash(application.getAccount().getUsername())
                .withRel("account")
        );
        this.add(
            linkTo(
                ApplicationRestController.class
            ).withRel("applications")
        );
        this.add(
            linkTo(ApplicationRestController.class)
                            .slash(application.getName()
            ).withSelfRel()
        );
        this.add(new Link(application.getRepoUri(), "application-repo-uri"));
        this.reference = application.getReference();

    }

    public String getRepoUri() {
        return this.repoUri;
    }

    public String getName() {
        return this.name;
    }

    public String getAbout() {
        return about;
    }

    public String getContact() {
        return contact;
    }

    public String getVersion() {
        return version;
    }

    public Collection<ApplicationCloudProviderResource> getCloudProviders() {
        return cloudProviders;
    }
    
    public Collection<String> getDeploymentParameters() {
		return deploymentParameters;
	}

	public Collection<String> getOutputs() {
        return outputs;
    }

    public Collection<String> getVolumes() {
        return volumes;
    }

    public Collection<String> getSharedWithAccountEmails() {
        return sharedWithAccountEmails;
    }

    public Collection<String> getSharedWithTeamNames() {
        return sharedWithTeamNames;
    }

	public String getAccountUsername() {
		return accountUsername;
	}

    public String getAccountGivenName() {
        return accountGivenName;
    }

    public String getAccountEmail() {
        return accountEmail;
    }

    public String getReference() {
        return reference;
    }

	public Collection<ApplicationInputResource> getInputs() {
		return inputs;
	}

	public void setInputs(Collection<ApplicationInputResource> inputs) {
		this.inputs = inputs;
	}
}
