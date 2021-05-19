package uk.ac.ebi.tsc.portal.security;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
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
import uk.ac.ebi.tsc.portal.api.team.repo.Team;
import uk.ac.ebi.tsc.portal.api.team.service.TeamNotFoundException;
import uk.ac.ebi.tsc.portal.api.team.service.TeamService;
import uk.ac.ebi.tsc.portal.clouddeployment.application.ApplicationDeployer;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.Date;
import java.util.*;

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
    private final String ecpAapUsername;
    private final String ecpAapPassword;
    private final Map<String, List<DefaultTeamMap>> defaultTeamsMap;

    @Autowired
	public EcpAuthenticationService(
            uk.ac.ebi.tsc.aap.client.security.TokenAuthenticationService tokenAuthenticationService,
            AccountService accountService, DeploymentService deploymentService,
            DeploymentConfigurationService deploymentConfigurationService,
            CloudProviderParamsCopyService cloudProviderParamsCopyService, TeamService teamService,
            ApplicationDeployer applicationDeployer, DomainService domainService, TokenService tokenService,
            EncryptionService encryptionService, ResourceLoader resourceLoader,
            @Value("${ecp.aap.username}") final String ecpAapUsername,
            @Value("${ecp.aap.password}") final String ecpAapPassword,
            @Value("${ecp.default.teams.file}") final String ecpDefaultTeamsFilePath) throws IOException {
        this.tokenAuthenticationService = tokenAuthenticationService;
        this.ecpAapUsername = ecpAapUsername;
        this.ecpAapPassword = ecpAapPassword;
        this.tokenService = tokenService;
        this.accountService = accountService;
        this.applicationDeployer = applicationDeployer;
        this.deploymentService = deploymentService;
        this.deploymentConfigurationService = deploymentConfigurationService;
        this.cloudProviderParamsCopyService = cloudProviderParamsCopyService;
        this.teamService = teamService;
        this.defaultTeamsMap = new HashMap<>();
        // Read maps from json file
        ObjectMapper mapper = new ObjectMapper();
        try {
            logger.info("Reading mappings file " + ecpDefaultTeamsFilePath);
            Resource defaultMapsResource = resourceLoader.getResource(ecpDefaultTeamsFilePath);
            DefaultTeamMap[] maps = mapper.readValue(defaultMapsResource.getInputStream(), DefaultTeamMap[].class);
            Arrays.stream(maps).forEach(defaultTeamMap -> {
                logger.info("Registering team " + defaultTeamMap.getTeamName() + " mapping for email domain " + defaultTeamMap.getEmailDomain());
                this.defaultTeamsMap.putIfAbsent(defaultTeamMap.getEmailDomain(), Lists.newArrayList());
                this.defaultTeamsMap.get(defaultTeamMap.getEmailDomain()).add(defaultTeamMap);
            });
        } catch (JsonMappingException jme) {
            logger.info("Can't find any default team mappings");
        }
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

        // Manage default team membership
        this.addAccountToDefaultTeamsByEmail(account);

        return authentication;
    }

    /**
     * Adds accounts to default teams if they are not already there.
     *
     * @param account The ECP Account which membership needs to be managed
     */
    public void addAccountToDefaultTeamsByEmail(Account account) {
        // Get email domain
        String[] emailSplit = account.getEmail().split("@");
        String emailDomain = emailSplit[emailSplit.length-1];

        // Add to all the associated teams
        List<DefaultTeamMap> defaultTeamMaps = this.defaultTeamsMap.get(emailDomain);
        if (defaultTeamMaps!=null) defaultTeamMaps.forEach(defaultTeamMap -> {
            // Get ECP AAP account token
            try {
                // Get associated team
                Team defaultTeam = this.teamService.findByNameAndGetAccounts(defaultTeamMap.getTeamName());
                if (!defaultTeam.getAccountsBelongingToTeam().stream().anyMatch(anotherAccount -> anotherAccount.getUsername().equals(account.getUsername()))) {
                    logger.info("Adding '" + account.getGivenName() + "' to team " + defaultTeam.getName());
                    // Add member to team
                    String ecpAapToken = this.tokenService.getAAPToken(this.ecpAapUsername, this.ecpAapPassword);
                    teamService.addMemberToTeamByAccountNoNotification(ecpAapToken, defaultTeam.getName(), account);
                }
            } catch (TeamNotFoundException tnfe) {
            	logger.info("Team " + defaultTeamMap.getTeamName() + " not found. Can't add user " + account.getEmail());
            }
        });
    }

}
