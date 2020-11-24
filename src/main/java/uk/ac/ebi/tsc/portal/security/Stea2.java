package uk.ac.ebi.tsc.portal.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import uk.ac.ebi.tsc.portal.api.account.service.AccountService;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class Stea2 extends GenericFilterBean {

    private final EcpAuthenticationService authenticationService;

    private final AccountService accountService;

    @Autowired
    public Stea2(EcpAuthenticationService authenticationService, AccountService accountService) {
        this.authenticationService = authenticationService;
        this.accountService = accountService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        Authentication authentication = authenticationService.getAuthentication(httpRequest);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        System.out.println("authentication.getPrincipal()" + authentication.getPrincipal());
        logger.info("ste2 USer ");
       // User user = (User) accountService.loadUserByUsername(authentication.getPrincipal().toString());
        //logger.info("Authorities size " + user.getAuthorities().size());
        filterChain.doFilter(request, response);
    }
}
