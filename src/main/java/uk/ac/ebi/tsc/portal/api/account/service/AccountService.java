package uk.ac.ebi.tsc.portal.api.account.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.account.repo.AccountRepository;

import javax.servlet.http.HttpServletRequest;
import javax.swing.plaf.synth.SynthTextAreaUI;
import java.util.Collection;
import java.util.Optional;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@Service
public class AccountService implements UserDetailsService {

    @Autowired
    DomainService domainService;

    private static final String TOKEN_HEADER_KEY = "Authorization";
    private static final String TOKEN_HEADER_VALUE_PREFIX = "Bearer ";


    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account findByReference(String reference) {
        return this.accountRepository.findByReference(reference).orElseThrow(
                () -> new UserNotFoundException(reference));
    }

    public Account findByUsername(String username) {
        return this.accountRepository.findByUsername(username).orElseThrow(
                () -> new UserNotFoundException(username));
    }

    public Account findByEmail(String email) {
        return this.accountRepository.findByEmail(email).stream().findFirst().orElseThrow(
                () -> new UserNotFoundException(email));
    }


    public Account save(Account account) {
        return this.accountRepository.save(account);
    }

    @Override
    public UserDetails loadUserByUsername(String un) throws UsernameNotFoundException {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.
                        currentRequestAttributes()).
                        getRequest();
        final String header = request.getHeader(TOKEN_HEADER_KEY);
        final String aapToken = header.substring(TOKEN_HEADER_VALUE_PREFIX.length());
        Collection<Domain> domains = domainService.getMyManagementDomains(aapToken);
        Optional<Domain> adminDomain = domains.stream().filter(domain -> domain.getDomainName().matches("self.AUTH_PORTAL")).findAny();

        Account a = this.findByUsername(un);

        if (adminDomain.isPresent()) {
        return new User(a.username, a.password, true, true, true, true,
                    AuthorityUtils.createAuthorityList("ROLE_USER","ROLE_ADMIN", "write"));
            // .orElseThrow(
            //   () -> new UsernameNotFoundException("could not find the user '" + un + "'"));

        }
        return new User(a.username, a.password, true, true, true, true,
                AuthorityUtils.createAuthorityList("ROLE_USER", "write"));

    }
}
        /*}else{
            return accountRepository
                    .findByUsername(un)
                    .map(a -> new User(a.username, a.password, true, true, true, true,
                            AuthorityUtils.createAuthorityList("ROLE_USER", "write")))
                    .orElseThrow(
                            () -> new UsernameNotFoundException("could not find the user '" + un + "'"));

*/
