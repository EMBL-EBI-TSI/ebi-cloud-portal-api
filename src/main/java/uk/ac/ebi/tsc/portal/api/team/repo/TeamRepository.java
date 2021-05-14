package uk.ac.ebi.tsc.portal.api.team.repo;

import org.springframework.data.jpa.repository.EntityGraph;
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
public interface TeamRepository extends JpaRepository<Team, Long>{

    @EntityGraph(attributePaths = {"accountsBelongingToTeam"}, type = EntityGraph.EntityGraphType.LOAD)
	Optional<Team> findTeamByName(String name);
	Collection<Team> findByAccountUsername(String accountUsername);
    Optional<Team> findByNameAndAccountUsername(String teamName, String name);
    Optional<Team> findByDomainReference(String domainReference);
    Optional<Team> findByName(String name);
}
