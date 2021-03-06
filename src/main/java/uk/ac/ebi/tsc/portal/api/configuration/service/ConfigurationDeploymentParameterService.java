package uk.ac.ebi.tsc.portal.api.configuration.service;


import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationDeploymentParameter;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationDeploymentParameterRepository;
/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@Service
public class ConfigurationDeploymentParameterService {

	private final ConfigurationDeploymentParameterRepository configurationDeploymentParameterRepository;

	@Autowired
	public ConfigurationDeploymentParameterService(ConfigurationDeploymentParameterRepository configurationDeploymentParameterRepository) {
	        this.configurationDeploymentParameterRepository = configurationDeploymentParameterRepository;
	}

	public ConfigurationDeploymentParameter save(ConfigurationDeploymentParameter deploymentParameter) {
		return this.configurationDeploymentParameterRepository.save(deploymentParameter);
	}
	
	public Collection<ConfigurationDeploymentParameter> findAll() {
		return this.configurationDeploymentParameterRepository.findAll();
	}
}
