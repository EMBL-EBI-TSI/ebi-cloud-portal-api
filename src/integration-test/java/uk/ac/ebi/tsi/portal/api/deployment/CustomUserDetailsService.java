package uk.ac.ebi.tsi.portal.api.deployment;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.account.repo.AccountRepository;

import java.util.*;


public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = Logger.getLogger(CustomUserDetailsService.class);

    @Value("${ajayUserName}")
    private String ajayUserName;

    @Value("${ajayPassword}")
    private String ajayPassword;

   /* @Bean("customUserDetailsService")
    @Primary
    public UserDetailsService userDetailsService() {

       /* Account ajayAccount = accountRepository.findByReference("usr-e8c1d6d5-6bf4-4636-a70e-41b8f32c70b4").get();

        return new InMemoryUserDetailsManager(Arrays.asList(
                new org.springframework.security.core.userdetails.User(ajayAccount.username, ajayAccount.password,
                        true, true, true, true,
                        AuthorityUtils.createAuthorityList("self.AUTH_PORTAL", "write"))
        ));

        return this::loadUserByUsername;
    }*/

    private User getAdminUser() {

        return new org.springframework.security.core.userdetails.User(ajayUserName, ajayPassword,
                true, true, true, true,
                AuthorityUtils.createAuthorityList("self.AUTH_PORTAL", "write"));
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        if (Objects.equals(username, ajayUserName))
            return getAdminUser();
        throw new UsernameNotFoundException(username);
    }
}
