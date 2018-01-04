package uk.ac.ebi.tsc.portal.api.deployment.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 
 * @author navis
 *
 */
public interface DeploymentApplicationRepository extends JpaRepository<DeploymentApplication, Long> {
	List<DeploymentApplication> findByAccountIdAndRepoPath(Long accountId, String repoPath);
}
