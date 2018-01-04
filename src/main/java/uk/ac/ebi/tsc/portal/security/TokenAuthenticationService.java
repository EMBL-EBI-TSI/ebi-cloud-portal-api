package uk.ac.ebi.tsc.portal.security;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.account.repo.AccountRepository;
import uk.ac.ebi.tsc.portal.api.account.service.AccountService;

import javax.servlet.http.HttpServletRequest;
import java.sql.Date;
import java.util.UUID;

/**
 * Checks whether a request contains a valid Json Web Token, from a valid user
 */
@Component
public class TokenAuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(TokenAuthenticationService.class);
    private static final String TOKEN_HEADER_KEY = "Authorization";
    private static final String TOKEN_HEADER_VALUE_PREFIX = "Bearer ";

    private final TokenHandler tokenHandler;
    private final AccountService accountService;

    @Autowired
    public TokenAuthenticationService(TokenHandler tokenHandler, AccountRepository accountRepository) {
        this.tokenHandler = tokenHandler;
        this.accountService = new AccountService(accountRepository);
    }

    Authentication getAuthentication(HttpServletRequest request) {
        try {
            final String header = request.getHeader(TOKEN_HEADER_KEY);
            if (header == null) {
                logger.trace("No {} header", TOKEN_HEADER_KEY);
                return null;
            }
            if (!header.startsWith(TOKEN_HEADER_VALUE_PREFIX)) {
                logger.trace("No {} prefix", TOKEN_HEADER_VALUE_PREFIX);
                return null;
            }
            final String token = header.substring(TOKEN_HEADER_VALUE_PREFIX.length());
            if (StringUtils.isEmpty(token)) {
                logger.trace("Missing jwt token");
                return null;
            }
            logger.trace("Got token {}", token);

            try { // Try to find user by token name claim
                UserDetails user = tokenHandler.loadUserFromTokenName(token);
                logger.trace("user details by sub {}", user.getUsername());
                // Here we need to update the legacy username by using sub instead
                String name = tokenHandler.parseNameFromToken(token);
                Account theAccount = this.accountService.findByUsername(name);
                String userName = tokenHandler.parseUserNameFromToken(token);
                theAccount.setUsername(userName);
                theAccount.setGivenName(name);
                this.accountService.save(theAccount);
                // Return the user authentication
                return new UserAuthentication(tokenHandler.loadUserFromTokenSub(token));
            } catch (UsernameNotFoundException usernameNotFoundException) { // Not found by given name...
                try { // Try to find user by sub name claim
                    UserDetails user = tokenHandler.loadUserFromTokenSub(token);
                    logger.trace("user details by sub {}", user.getUsername());
                    // Return the user authentication
                    return new UserAuthentication(user);
                } catch (UsernameNotFoundException anotherUsernameNotFoundException) { // User not found at all
                    String userName = tokenHandler.parseUserNameFromToken(token);
                    String email = tokenHandler.parseEmailFromToken(token);
                    String name = tokenHandler.parseNameFromToken(token);
                    logger.info("No account found for user " + userName);
                    logger.info("Creating account for user {}, {}", userName, name);
                    try {
                        Account newAccount = new Account(
                                "acc" + System.currentTimeMillis(),
                                userName,
                                name,
                                UUID.randomUUID().toString(),
                                email,
                                new Date(System.currentTimeMillis()),
                                null,
                                null
                        );
                        this.accountService.save(newAccount);
                        // Return the user authentication
                        return new UserAuthentication(tokenHandler.loadUserFromTokenSub(token));
                    } catch (Exception sql) {
                        logger.info("Couldn't add new account for user " + userName + ". Already added?");
                        logger.info(sql.getMessage());
                        return new UserAuthentication(tokenHandler.loadUserFromTokenSub(token));
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.trace("", e);
            return null;
        }
    }

}