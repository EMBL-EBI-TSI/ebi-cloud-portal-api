package uk.ac.ebi.tsc.portal.api.deployment.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.ac.ebi.tsc.portal.api.deployment.controller.DeploymentAssignedInputResource;
import uk.ac.ebi.tsc.portal.api.deployment.repo.Deployment;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentApplication;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentRepository;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentStatus;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentStatusEnum;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentStatusRepository;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@Service
public class DeploymentService {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentService.class);

    private final DeploymentRepository deploymentRepository;
    private final DeploymentStatusRepository deploymentStatusRepository;

   @Autowired
   public DeploymentService(DeploymentRepository deploymentRepository,
                             DeploymentStatusRepository deploymentStatusRepository) {
        this.deploymentRepository = deploymentRepository;
        this.deploymentStatusRepository = deploymentStatusRepository;
    }

    public Collection<Deployment> findByAccountUsername(String username) {
        return deploymentRepository.findByAccountUsername(username);
    }

    public Collection<Deployment> findByDeploymentStatusStatus(DeploymentStatusEnum status) {
        return deploymentRepository.findByDeploymentStatusStatus(status);
    }

    public Deployment findByAccountUsernameAndId(String username, Long id) {
        return this.deploymentRepository.findByAccountUsernameAndId(username, id).orElseThrow(
                () -> new DeploymentNotFoundException(username, id));
    }
    public Collection<Deployment> findByAccountUsernameAndDeploymentStatusStatus(String username, DeploymentStatusEnum status) {
        return this.deploymentRepository.findByAccountUsernameAndDeploymentStatusStatus(username,status );
    }


    public Deployment save(Deployment deployment) {
        return this.deploymentRepository.save(deployment);
    }

    public void delete(Long deploymentId) {
        this.deploymentRepository.delete(deploymentId);
    }


    public DeploymentStatus findStatusByDeploymentId(Long deploymentId) {
        return this.deploymentStatusRepository.findByDeploymentId(deploymentId).orElseThrow(
                () -> new DeploymentStatusNotFoundException(deploymentId));
    }

    public Deployment findByReference(String reference) {
        return this.deploymentRepository.findByReference(reference).orElseThrow(
                () -> new DeploymentNotFoundException(reference));
    }

    public Deployment findByAccessIp(String ip) {
        return this.deploymentRepository.findByAccessIp(ip).orElseThrow(
                () -> new DeploymentNotFoundException(ip));
    }
    
    public List<Deployment> findByDeploymentApplicationId(Long id){
    	return this.deploymentRepository.findByDeploymentApplicationId(id);
    }
    
    public List<Deployment> findAll(){
    	return this.deploymentRepository.findAll();
    }

    public Collection<Deployment> findByConfigurationReference(String reference) {
        return this.deploymentRepository.findByDeploymentConfigurationConfigurationReference(reference);
    }

    public List<Deployment> findDeployments(String userId, boolean hideDestroyed) {
        if(hideDestroyed){
            //get only active deployments status RUNNING, STARTING, STARTING_FAILED
            Stream<Deployment> userDeploymentsStream = Stream.of(
                    this.findByAccountUsernameAndDeploymentStatusStatus(userId, DeploymentStatusEnum.STARTING),
                    this.findByAccountUsernameAndDeploymentStatusStatus(userId, DeploymentStatusEnum.STARTING_FAILED),
                    this.findByAccountUsernameAndDeploymentStatusStatus(userId, DeploymentStatusEnum.RUNNING),
                    this.findByAccountUsernameAndDeploymentStatusStatus(userId, DeploymentStatusEnum.RUNNING_FAILED))
                    .flatMap(Collection::stream);
            return userDeploymentsStream .collect(Collectors.toList());
        }else{
            Stream<Deployment> userDeploymentsStream = Stream.of(
                    this.findByAccountUsernameAndDeploymentStatusStatus(userId, DeploymentStatusEnum.DESTROYING_FAILED),
                    this.findByAccountUsernameAndDeploymentStatusStatus(userId, DeploymentStatusEnum.DESTROYING),
                    this.findByAccountUsernameAndDeploymentStatusStatus(userId, DeploymentStatusEnum.DESTROYED))
                    .flatMap(Collection::stream);
            return userDeploymentsStream .collect(Collectors.toList());
        }

    }
}
