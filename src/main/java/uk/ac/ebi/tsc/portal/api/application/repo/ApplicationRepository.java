package uk.ac.ebi.tsc.portal.api.application.repo;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Optional<Application> findById(Long id);
    Optional<Application> findByReference(String reference);
    Collection<Application> findByAccountUsername(String username, Sort sort);
    Optional<Application> findByAccountUsernameAndName(String username, String name);
}