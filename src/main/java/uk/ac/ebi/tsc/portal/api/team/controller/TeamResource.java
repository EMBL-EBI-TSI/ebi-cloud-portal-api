package uk.ac.ebi.tsc.portal.api.team.controller;

import uk.ac.ebi.tsc.portal.api.team.repo.Team;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
public class TeamResource {

	private String name;
	private String ownerAccountEmail;
	private Collection<String> memberAccountEmails;
	private Collection<String> applicationNames;
	private Collection<String> cloudProviderParameterNames;
	private Collection<String> configurationNames;
	private Collection<String> configurationDeploymentParameterNames;
	private String domainReference;
	private List<String> managerUserNames;
	private List<String> managerEmails;
	
	public TeamResource() {
	}

	public TeamResource(Team team) {
		this.name = team.getName();
		this.ownerAccountEmail = team.getAccount().getEmail();
		this.memberAccountEmails = team.getAccountsBelongingToTeam().stream().map(
				account -> account.getEmail()).collect(Collectors.toList());
		this.applicationNames = team.getApplicationsBelongingToTeam().stream().map(
				application -> application.getName()
		).collect(Collectors.toList());
		this.cloudProviderParameterNames = team.getCppBelongingToTeam().stream().map(
				cpp -> cpp.getName()).collect(Collectors.toList());
		this.configurationNames = team.getConfigurationsBelongingToTeam().stream().map(
				configuration -> configuration.getName()).collect(Collectors.toList());
		this.configurationDeploymentParameterNames = team.getConfigDepParamsBelongingToTeam().stream().map(
				configurationDeploymentParameterName -> configurationDeploymentParameterName.getName()).collect(Collectors.toList());
		this.domainReference = team.getDomainReference();
		this.managerEmails = new ArrayList<>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOwnerAccountEmail() {
		return ownerAccountEmail;
	}

	public void setOwnerAccountEmail(String ownerAccountEmail) {
		this.ownerAccountEmail = ownerAccountEmail;
	}

	public Collection<String> getMemberAccountEmails() {
		return memberAccountEmails;
	}

	public void setMemberAccountEmails(Collection<String> memberAccountEmails) {
		this.memberAccountEmails = memberAccountEmails;
	}

	public Collection<String> getApplicationNames() {
		return applicationNames;
	}

	public void setApplicationNames(Collection<String> applicationNames) {
		this.applicationNames = applicationNames;
	}

	public Collection<String> getCloudProviderParameterNames() {
		return cloudProviderParameterNames;
	}

	public void setCloudProviderParameterNames(Collection<String> cloudProviderParameterNames) {
		this.cloudProviderParameterNames = cloudProviderParameterNames;
	}

	public String getDomainReference() {
		return domainReference;
	}

	public void setDomainReference(String domainReference) {
		this.domainReference = domainReference;
	}

	public Collection<String> getConfigurationNames() {
		return configurationNames;
	}

	public void setConfigurationNames(Collection<String> configurationNames) {
		this.configurationNames = configurationNames;
	}

	public Collection<String> getConfigurationDeploymentParameterNames() {
		return configurationDeploymentParameterNames;
	}

	public void setConfigurationDeploymentParameterNames(Collection<String> configurationDeploymentParameterNames) {
		this.configurationDeploymentParameterNames = configurationDeploymentParameterNames;
	}

	public List<String> getManagerUserNames() {
		return managerUserNames;
	}

	public void setManagerUserNames(List<String> managerUserNames) {
		this.managerUserNames = managerUserNames;
	}

	public List<String> getManagerEmails() {
		return managerEmails;
	}

	public void setManagerEmails(List<String> managerEmails) {
		this.managerEmails = managerEmails;
	}
}
