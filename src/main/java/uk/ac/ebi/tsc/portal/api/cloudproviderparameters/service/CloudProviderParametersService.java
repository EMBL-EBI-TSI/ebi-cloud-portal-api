package uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.model.User;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.application.repo.Application;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.controller.CloudProviderParametersResource;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParameters;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParametersField;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParametersRepository;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParamsCopy;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParamsCopyField;
import uk.ac.ebi.tsc.portal.api.configuration.service.ConfigurationService;
import uk.ac.ebi.tsc.portal.api.deployment.controller.DeploymentRestController;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentStatusEnum;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentService;
import uk.ac.ebi.tsc.portal.api.encryptdecrypt.security.EncryptionService;
import uk.ac.ebi.tsc.portal.api.team.repo.Team;
import uk.ac.ebi.tsc.portal.api.utils.SendMail;


/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@Service
public class CloudProviderParametersService {

	private static final Logger logger = LoggerFactory.getLogger(CloudProviderParametersService.class);

	private final CloudProviderParametersRepository cloudProviderParametersRepository;
	private final DomainService domainService;
	private final CloudProviderParamsCopyService cloudProviderParametersCopyService;
	private final EncryptionService encryptionService;
	private final SendMail sendMail;

	@Autowired
	public CloudProviderParametersService(
			CloudProviderParametersRepository cloudProviderParametersRepository,
			DomainService domainService,
			CloudProviderParamsCopyService cloudProviderParametersCopyService,
			EncryptionService encryptionService,
			SendMail sendMail) {
		this.cloudProviderParametersRepository = cloudProviderParametersRepository;
		this.domainService = domainService;
		this.cloudProviderParametersCopyService = cloudProviderParametersCopyService;
		this.encryptionService = encryptionService;
		this.sendMail = sendMail;
	}

	public Collection<CloudProviderParameters> findByAccountUsername(String username) throws InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, IOException, BadPaddingException, IllegalBlockSizeException {
		Collection<CloudProviderParameters> encryptedRes = this.cloudProviderParametersRepository.findByAccountUsername(username);
		return decryptAll(encryptedRes);
	}

	public CloudProviderParameters findByName(String name){
		CloudProviderParameters encryptedRes = this.cloudProviderParametersRepository.findByName(name).get(0);
		return decryptOne(encryptedRes);
	}

	public CloudProviderParameters findByReference(String reference){
		CloudProviderParameters encryptedRes = this.cloudProviderParametersRepository.findByReference(reference).orElseThrow(
				() -> new CloudProviderParametersNotFoundException(reference));
		return decryptOne(encryptedRes);
	}

	public CloudProviderParameters findById(Long id){
		CloudProviderParameters encryptedRes = this.cloudProviderParametersRepository.findById(id).orElseThrow(
				() -> new CloudProviderParametersNotFoundException(id));
		return decryptOne(encryptedRes);
	}


	public void delete(CloudProviderParameters cloudParameters) {
		this.cloudProviderParametersRepository.delete(cloudParameters);
	}

	public Collection<CloudProviderParameters> findByCloudProviderAndAccountUsername(String cloudProvider, String username)
			throws InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException,
			NoSuchAlgorithmException, InvalidKeySpecException, IOException, BadPaddingException,
			IllegalBlockSizeException {
		Collection<CloudProviderParameters> encryptedRes = this.cloudProviderParametersRepository.findByCloudProviderAndAccountUsername(cloudProvider, username);
		if (encryptedRes.size() == 0) {
			throw new CloudProviderParametersNotFoundException(username,cloudProvider);
		}
		return decryptAll(encryptedRes);
	}

	public CloudProviderParameters save(CloudProviderParameters cloudProviderParameters) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, InvalidKeySpecException, BadPaddingException, IllegalBlockSizeException {

		Map<String, String> encryptedCloudProviderParameters = encryptionService.encrypt(cloudProviderParameters);
		Collection<CloudProviderParametersField> fields = new ArrayList<CloudProviderParametersField>();
		for (Entry<String, String> cloudProviderParametersField : encryptedCloudProviderParameters.entrySet()) {	
			fields.add(new CloudProviderParametersField(cloudProviderParametersField.getKey(), cloudProviderParametersField.getValue(),
					cloudProviderParameters));
		}

		cloudProviderParameters.setFields(fields);

		return this.cloudProviderParametersRepository.save(cloudProviderParameters);
	
	}

	public void deleteByNameAndAccountName(String name, String username) {
		CloudProviderParameters cloudProviderParameters = this.cloudProviderParametersRepository.findByNameAndAccountUsername(name, username).orElseThrow(
				() -> new CloudProviderParametersNotFoundException(name, username));
		this.cloudProviderParametersRepository.delete(cloudProviderParameters.getId());
	}

	public CloudProviderParameters findByNameAndAccountUsername(String name, String username) {

		CloudProviderParameters encryptedRes = this.cloudProviderParametersRepository.findByNameAndAccountUsername(name, username).orElseThrow(
				() -> new CloudProviderParametersNotFoundException(username, name)
				);
		return decryptOne(encryptedRes);
	}

	private Collection<CloudProviderParameters> decryptAll(Collection<CloudProviderParameters> encryptedAll) {

		Collection<CloudProviderParameters> decryptedRes = new LinkedList<>();

		encryptedAll.forEach(encryptedCloudProviderParameters -> {
			decryptedRes.add(decryptOne(encryptedCloudProviderParameters));
		});

		return decryptedRes;
	}

	private CloudProviderParameters decryptOne(CloudProviderParameters encryptedCloudProviderParameters) {
		
			return decryptCloudProviderParameters(encryptedCloudProviderParameters);
	}
	
	private CloudProviderParameters decryptCloudProviderParameters(CloudProviderParameters encryptedCloudProviderParameters) {

		Map<String, String> decryptedValues = encryptionService.decryptOne(encryptedCloudProviderParameters);

		CloudProviderParameters decryptedCloudProviderParameters =
				new CloudProviderParameters(
						encryptedCloudProviderParameters.getName(),
						encryptedCloudProviderParameters.getCloudProvider(),
						encryptedCloudProviderParameters.getAccount()
						);

		//construct collection of fields
		Map<String, String> decryptedFields = encryptionService.decryptOne(encryptedCloudProviderParameters);
		for (Entry<String, String> cloudProviderParametersField : decryptedFields.entrySet()) {	
			//create new decrypted cpp field
			CloudProviderParametersField newDecryptedField = new CloudProviderParametersField(cloudProviderParametersField.getKey(), cloudProviderParametersField.getValue(),
					decryptedCloudProviderParameters);
			for(CloudProviderParametersField ecField: encryptedCloudProviderParameters.getFields()){
				if(ecField.getKey().equals(cloudProviderParametersField.getKey())){
					newDecryptedField.setId(ecField.getId());
					decryptedCloudProviderParameters.getFields().add(newDecryptedField);
				}
			}
		}

		decryptedCloudProviderParameters.setId(encryptedCloudProviderParameters.getId());
		decryptedCloudProviderParameters.setSharedWith(encryptedCloudProviderParameters.getSharedWith());
		decryptedCloudProviderParameters.setSharedWithTeams(encryptedCloudProviderParameters.getSharedWithTeams());
		decryptedCloudProviderParameters.setReference(encryptedCloudProviderParameters.getReference());
		return decryptedCloudProviderParameters;
	}

	public Set<CloudProviderParameters> getSharedCppsByAccount(Account account,  String token, User user ){

		Set<CloudProviderParameters> sharedCpps = new HashSet<>();
		logger.info("In CppService: getting shared cloud provider parameters");

		for(Team memberTeam: account.getMemberOfTeams()){
			try{
				logger.info("In CppService: checking if team has a domain reference ");
				if(memberTeam.getDomainReference() != null){
					logger.info("In CppService: check if user is a domain member");
					Domain domain = domainService.getDomainByReference(memberTeam.getDomainReference(), token);
					if(domain != null){
						Collection<User> users = domainService.getAllUsersFromDomain(domain.getDomainReference(), token);
						User domainUser = users.stream().filter(u -> u.getEmail().equals(user.getEmail())).findAny().orElse(null);
						if(domainUser != null){
							logger.info("In CppService: returning shared cloud provider parameters , if user is member of team and domain");
							sharedCpps.addAll(memberTeam.getCppBelongingToTeam());
						}
					}
				}else{
					logger.info("In CppService:  returning shared credentials, if user is member of team and no domain present");
					sharedCpps.addAll(memberTeam.getCppBelongingToTeam());
				}
			}catch(Exception e){
				logger.error("In CppService: Could not add all shared cloud provider parameters from team " + memberTeam.getName());
			}
		}
		return sharedCpps;
	}

	public CloudProviderParameters getSharedCppsByCppName(Account account,  String token, User user, String cppName ){

		logger.info("In CppService: getting shared cloud provider parameters by name");
		for(Team memberOfTeam: account.getMemberOfTeams()){
			try{
				logger.info("In CppService: checking if team has a domain reference ");
				if(memberOfTeam.getDomainReference() != null){
					logger.info("In CppService: check if user is a domain member");
					Domain domain = domainService.getDomainByReference(memberOfTeam.getDomainReference(), token);
					if(domain != null){
						Collection<User> users = domainService.getAllUsersFromDomain(domain.getDomainReference(), token);
						User domainUser = users.stream().filter(u -> u.getEmail().equals(user.getEmail())).findAny().orElse(null);
						if(domainUser != null){
							logger.info("In CppService: returning the shared cloud provider parameters, if user is member of team and domain");
							return memberOfTeam.getCppBelongingToTeam().stream().filter(cpp -> cppName.equals(cpp.getName()))
									.findFirst().get();
						}
					}
				}else{
					logger.info("In CppService:  returning shared credentials by name, if user is member of team and no domain present");
					return memberOfTeam.getCppBelongingToTeam().stream().filter(cpp -> cppName.equals(cpp.getName()))
							.findFirst().get();
				}
			}catch(Exception e){
				logger.error("Could not add shared cloud provider parameters " + cppName + e.getMessage() );
			}
		}
		return null;
	}

	public CloudProviderParameters updateFields(CloudProviderParameters cloudProviderParameters, CloudProviderParamsCopy cloudProviderParametersCopy, CloudProviderParametersResource input, String username) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeySpecException, BadPaddingException, IllegalBlockSizeException {

		//update changed values
		logger.info("In CloudProviderParmeterService: checking to see if values are to be updated");
		cloudProviderParameters.fields.forEach(field -> {
			//if the value is different than 'ENCRYPTED_VALUE' , it should be updated
			input.getFields().stream().filter(f -> !f.getValue().equals("ENCRYPTED VALUE") && f.getKey().equals(field.getKey()))
			.forEach(matchedField -> {
				logger.info("In CloudProviderParmeterService: updated value found for " + matchedField.getKey());
				logger.info("matched " + matchedField.getValue());
				field.setValue(matchedField.getValue());
			});
		});

		//update changed values in cpp copy
		logger.info("In CloudProviderParmeterService: checking to see if the cloud params copy values are to be updated");
		cloudProviderParametersCopy.fields.forEach(field -> {
			//if the value is different than 'ENCRYPTED_VALUE' , it should be updated
			input.getFields().stream().filter(f -> !f.getValue().equals("ENCRYPTED VALUE") && f.getKey().equals(field.getKey()))
			.forEach(matchedField -> {
				logger.info("In CloudProviderParmeterService: updated value found for copy " + matchedField.getKey());
				field.setValue(matchedField.getValue());
			});
		});

		//remove unused 
		List<String> parameterFieldKeysPresent = cloudProviderParameters.getFields().stream().map(f -> f.getKey()).collect(Collectors.toList());
		List<String> inputFieldKeysPresent = input.getFields().stream().map(f -> f.getKey()).collect(Collectors.toList());
		//if the input field keys does not contain key, mark it for removal
		List<String> parameterFieldKeysToRemove = parameterFieldKeysPresent.stream().filter(f -> !inputFieldKeysPresent.contains(f)).collect(Collectors.toList());
		//Get list of fields 
		List<CloudProviderParametersField> cppFieldsToRemove = new ArrayList<>();
		if(!parameterFieldKeysToRemove.isEmpty()){
			logger.info("In CloudProviderParmeterService: removing deleted parameter");
			cloudProviderParameters.getFields().forEach(field -> {
				if(parameterFieldKeysToRemove.contains(field.getKey())){
					logger.info("In CloudProviderParmeterService: removing deleted parameter " + field.getKey());
					cppFieldsToRemove.add(field);
				}
			});
		}
		cloudProviderParameters.getFields().removeAll(cppFieldsToRemove);

		//remove unused in cpp copy
		List<String> parameterFieldKeysPresentCopy = cloudProviderParametersCopy.getFields().stream().map(f -> f.getKey()).collect(Collectors.toList());
		List<String> inputFieldKeysPresentCopy = input.getFields().stream().map(f -> f.getKey()).collect(Collectors.toList());
		//if the input field keys does not contain key, mark it for removal
		List<String> parameterFieldKeysToRemoveCopy = parameterFieldKeysPresentCopy.stream().filter(f -> !inputFieldKeysPresentCopy.contains(f)).collect(Collectors.toList());
		//Get list of fields 
		List<CloudProviderParamsCopyField> cppFieldsToRemoveCopy = new ArrayList<>();
		if(!parameterFieldKeysToRemoveCopy.isEmpty()){
			logger.info("In CloudProviderParmeterService: removing deleted parameter from copy");
			cloudProviderParametersCopy.getFields().forEach(field -> {
				if(parameterFieldKeysToRemoveCopy.contains(field.getKey())){
					logger.info("In CloudProviderParmeterService: removing deleted parameter from copy " + field.getKey());
					cppFieldsToRemoveCopy.add(field);
				}
			});
		}
		cloudProviderParameters.getFields().removeAll(cppFieldsToRemove);
		cloudProviderParametersCopy.getFields().removeAll(cppFieldsToRemoveCopy);

		//add new
		logger.info("In CloudProviderParmeterService: checking to see if new parameters are to be added");
		List<CloudProviderParametersField> inputFields = input.getFields().stream().map(f -> new CloudProviderParametersField(f.getKey(), f.getValue(), cloudProviderParameters)).collect(Collectors.toList());
		List<String> presentFields = cloudProviderParameters.fields.stream().map(f -> f.getKey()).collect(Collectors.toList());
		List<CloudProviderParametersField> parameterFieldKeysToAdd = inputFields.stream().filter(f -> !presentFields.contains(f.getKey())).collect(Collectors.toList());
		if(!parameterFieldKeysToAdd.isEmpty()){
			logger.info("In CloudProviderParmeterService: adding new " + parameterFieldKeysToAdd.size() + " parameter(s)");
			cloudProviderParameters.fields.addAll(parameterFieldKeysToAdd);
		}

		//add new in cpp copy
		logger.info("In CloudProviderParmeterService: checking to see if new parameters are to be added in copy");
		List<CloudProviderParamsCopyField> inputFieldsCopy = input.getFields().stream().map(f -> new CloudProviderParamsCopyField(f.getKey(), f.getValue(), cloudProviderParametersCopy)).collect(Collectors.toList());
		List<String> presentFieldsCopy = cloudProviderParametersCopy.fields.stream().map(f -> f.getKey()).collect(Collectors.toList());
		List<CloudProviderParamsCopyField> parameterFieldKeysToAddCopy = inputFieldsCopy.stream().filter(f -> !presentFieldsCopy.contains(f.getKey())).collect(Collectors.toList());
		if(!parameterFieldKeysToAddCopy.isEmpty()){
			logger.info("In CloudProviderParmeterService: adding new " + parameterFieldKeysToAdd.size() + " parameter(s) to copy");
			cloudProviderParametersCopy.fields.addAll(parameterFieldKeysToAddCopy);
		}
		this.cloudProviderParametersCopyService.save(cloudProviderParametersCopy);
		return this.save(cloudProviderParameters);
	}

	public void stopDeploymentsOnDelete(Principal principal, String name, 
			DeploymentRestController deploymentRestController, 
			DeploymentService deploymentService,
			ConfigurationService configurationService,
			CloudProviderParameters cloudParameters) throws Exception {

		List<String> toNotify = new ArrayList<>();
		logger.info("Find and stop deployments using the cloud credential copy.");
		deploymentService.findAll().forEach(deployment -> {
			try{
				CloudProviderParamsCopy cloudProviderParametersCopy =
						this.cloudProviderParametersCopyService.findByCloudProviderParametersReference(deployment.getCloudProviderParametersReference());
				if(cloudParameters.getReference().equals(cloudProviderParametersCopy.getCloudProviderParametersReference())){
					try{
						if(deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.RUNNING)
								|| deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.STARTING)){
							deploymentRestController.stopByReference(deployment.getReference());
							toNotify.add(deployment.getAccount().getEmail());
						}
					}catch(Exception e){
						e.printStackTrace();
						logger.error("Failed to stop deployment, on deletion of cloud credential");
					}
				}
			}catch(CloudProviderParamsCopyNotFoundException e){
				e.printStackTrace();
				if(deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.RUNNING)
						|| deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.STARTING)){
					logger.error("Failed to stop deployments on deletion of cloud parameter,"
							+ "\n because the cpp copy was not found for deployment " + deployment.getReference());
				}else{
					logger.error("The cpp copy was not found , but no active deployment found for " + deployment.getReference());
				}
			}
		});

		if(!toNotify.isEmpty()){
			logger.info("There are users who are to be notified, regarding deployments destruction");
			String message = "Your deployments were destroyed. \n"
					+ "This was because the cloud credential " + "'" + cloudParameters.name + "'" +" was \n"
					+ " deleted by its owner " + cloudParameters.getAccount().givenName + ".";
			try{
				sendMail.send(toNotify, "Deployments destroyed", message );
			}catch(IOException e){
				logger.error("Failed to send messages to concerned persons, regarding destroying deployments");
			}
		}
	}
	
	public boolean isCloudProviderParametersSharedWithAccount(Account account, CloudProviderParameters cloudParameters){
		if(account.getMemberOfTeams().stream().anyMatch(t ->
		t.getCppBelongingToTeam().stream().anyMatch(c -> c.getReference().equals(cloudParameters.getReference())))){
			return true;
		}
		return false;
	}

	public boolean canCredentialBeUsedForApplication(CloudProviderParameters cloudProviderParameters,
			Application application, Account account) {
		
		logger.info("Looking if shared cloudProviderParameters " + cloudProviderParameters.getName()
		+ " is usable for application " + application.getName());

		try{//for team which user belongs to
			for(Team team: account.getMemberOfTeams()) {
				Team cloudProviderParametersSharedWithUser = cloudProviderParameters.getSharedWithTeams()
						.stream().filter(t -> t.getDomainReference().equals(team.getDomainReference()))
						.findAny().get();
				//so cloudProviderParameters is shared with the team, check if application is also shared
				if(application.getSharedWithTeams().stream()
						.anyMatch(t -> t.getDomainReference().equals(cloudProviderParametersSharedWithUser.getDomainReference()))){
					logger.info("Matching team has been found " + team.getName());
					return true;
				}
			}
		}catch(NoSuchElementException e) {
			logger.info("No matching team found " + e.getMessage());
		}
		return false;
	}

}
