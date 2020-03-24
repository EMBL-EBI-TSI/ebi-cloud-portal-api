package uk.ac.ebi.tsc.portal.api.deployment.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
public interface DeploymentRepository extends JpaRepository<Deployment, Long> {
    Collection<Deployment> findByAccountUsername(String username);
    Collection<Deployment> findByDeploymentStatusStatus(DeploymentStatusEnum status);
    Optional<Deployment> findByAccountUsernameAndId(String username, Long id);
    Optional<Deployment> findByReference(String reference);
    Optional<Deployment> findByAccessIp(String accessIp);
    List<Deployment> findByDeploymentApplicationId(Long id);
    Collection<Deployment> findByDeploymentConfigurationConfigurationReference(String reference);
    Collection<Deployment> findByAccountUsernameAndDeploymentStatusStatus(String username, DeploymentStatusEnum status);

}
