package uk.ac.ebi.tsc.portal.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.TokenService;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.account.service.AccountService;
import uk.ac.ebi.tsc.portal.api.account.service.UserNotFoundException;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParamsCopyService;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentConfigurationService;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentService;
import uk.ac.ebi.tsc.portal.api.encryptdecrypt.security.EncryptionService;
import uk.ac.ebi.tsc.portal.api.team.service.TeamService;
import uk.ac.ebi.tsc.portal.clouddeployment.application.ApplicationDeployer;

import javax.servlet.http.HttpServletRequest;
import java.sql.Date;
import java.util.UUID;

/**
 * Extracts user authentication details from Token using AAP domains API
 *
 * @author Jose A Dianes  <jdianes@ebi.ac.uk>
 * @since 09/05/2018.
 */
@Component
public class EcpAuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(EcpAuthenticationService.class);
    private static final String TOKEN_HEADER_KEY = "Authorization";
    private static final String TOKEN_HEADER_VALUE_PREFIX = "Bearer ";


    uk.ac.ebi.tsc.aap.client.security.TokenAuthenticationService tokenAuthenticationService;

    private final AccountService accountService;
    private final TokenService tokenService;
    private final TeamService teamService;
    private final DeploymentService deploymentService;
    private final CloudProviderParamsCopyService cloudProviderParamsCopyService;
    private final DeploymentConfigurationService deploymentConfigurationService;
    private final ApplicationDeployer applicationDeployer;

    @Autowired
	public EcpAuthenticationService(
            uk.ac.ebi.tsc.aap.client.security.TokenAuthenticationService tokenAuthenticationService,
            AccountService accountService, DeploymentService deploymentService,
            DeploymentConfigurationService deploymentConfigurationService,
            CloudProviderParamsCopyService cloudProviderParamsCopyService, TeamService teamService,
            ApplicationDeployer applicationDeployer, DomainService domainService, TokenService tokenService,
            EncryptionService encryptionService) {
        this.tokenAuthenticationService = tokenAuthenticationService;
        this.tokenService = tokenService;
        this.accountService = accountService;
        this.applicationDeployer = applicationDeployer;
        this.deploymentService = deploymentService;
        this.deploymentConfigurationService = deploymentConfigurationService;
        this.cloudProviderParamsCopyService = cloudProviderParamsCopyService;
        this.teamService = teamService;
    }

    /**
     * Builds an authentication object from the the http request. Relies on the app client implementation + ECP
     * account creation and handling default team membership.
     *
     * @param request
     * @return
     */
    public Authentication getAuthentication(HttpServletRequest request) {
        Authentication authentication = this.tokenAuthenticationService.getAuthentication(request);
        if (authentication == null) {
            return null;
        }

        // Get token - at this point we know the token is there since we passed the super.getAuthentication
        final String header = request.getHeader(TOKEN_HEADER_KEY);
        final String aapToken = header.substring(TOKEN_HEADER_VALUE_PREFIX.length());
        logger.trace("Got token {}", aapToken);

        // Get user details from authentication details
        uk.ac.ebi.tsc.aap.client.model.User user = (uk.ac.ebi.tsc.aap.client.model.User) authentication.getDetails();

        // Retrieve or create ECP account from DB
        Account account = null;
        try { // Try to find user by token name claim (legacy ECP accounts)
            logger.trace("Looking for account by token 'name' claim {}", user.getUserName());
            // get the account
            account = this.accountService.findByUsername(user.getFullName());
            // Update the account username with the token sub claim
            account.setUsername(user.getUsername()); // TODO - check with @ameliec
            account.setGivenName(user.getFullName());
            this.accountService.save(account);
        } catch (UserNotFoundException userNameNotFoundException) {
            try { // Try with sub claim (currently used)
                logger.trace("Looking for account by token 'sub' claim {}", user.getUsername());
                // check if account exists
                account = this.accountService.findByUsername(user.getUsername());
                // Update the account given name
                account.setGivenName(user.getFullName());
                this.accountService.save(account);
            } catch (UserNotFoundException usernameNotFoundException) {
                logger.info("No account found for user " + user.getUsername() + " ("+ user.getUserName());
                logger.info("Creating account for user {}, {}", user.getUsername(), user.getUserName());
                try {
                    account = new Account(
                            "acc" + System.currentTimeMillis(),
                            user.getUsername(),
                            user.getFullName(),
                            UUID.randomUUID().toString(),
                            user.getEmail(),
                            new Date(System.currentTimeMillis()),
                            null,
                            null
                    );
                    this.accountService.save(account);
                } catch (Exception sql) {
                    logger.info("Couldn't add new account for user "
                            + user.getUsername() + " ("+ user.getUserName() +"). Already added?");
                    logger.info(sql.getMessage());
                    return authentication;
                }
            }
        }

        // We should have retrieved or created an ECP account by now... otherwise
        if (account==null) {
            return null;
        }

        return authentication;
    }

}
