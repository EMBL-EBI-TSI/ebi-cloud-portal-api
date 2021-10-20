package uk.ac.ebi.tsc.portal.clouddeployment.application;

import static com.github.underscore.U.chain;
import static com.github.underscore.U.concat;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class DeploymentStrategy {
    
    static final String CONTAINER_APP_FOLDER = "/app";

    static final String CONTAINER_DEPLOYMENTS_FOLDER = "/deployments";

    static final String BASH_COMMAND = "bash";
    
    @Value("${be.deployments.root}")
    String deploymentsRoot;

    @Value("${be.docker}") 
    boolean docker;

    @Value("${ecp.scriptUser}")
    private String scriptUser;

    @Value("${ecp.executeScript.sudo}")
    private boolean sudoExecuteScript;
    
    void configure( ProcessBuilder processBuilder
                  , String cloudProviderPath
                  , Map<String, String> env
                  , String appFolder
                  , String deploymentsFolder
                  , String reference
                  , String script
                  )
    {
        if (this.docker) configureDocker(processBuilder, cloudProviderPath, env, appFolder, deploymentsFolder, reference, script);
                    else configureBash  (processBuilder, cloudProviderPath, env, appFolder, deploymentsFolder, reference, script);
        
    }
    
    void configureDocker( ProcessBuilder processBuilder
                        , String cloudProviderPath
                        , Map<String, String> env
                        , String appFolder
                        , String deploymentsFolder
                        , String reference
                        , String script
                        )
    {
        // Env
        setEnvDocker(env, reference);
        addKeyEnvVars(env, reference);
        
        // Command
        processBuilder.command(dockerCmd(appFolder, deploymentsFolder, cloudProviderPath, script, env));
    }
    
    void configureBash( ProcessBuilder processBuilder
                      , String cloudProviderPath
                      , Map<String, String> env
                      , String appFolder
                      , String deploymentsFolder
                      , String reference
                      , String script
                      )
    {
        // Env
        setEnvBash(env, reference, deploymentsRoot, appFolder);
        addKeyEnvVars(env, reference);
        
        Map<String, String> hostEnv = processBuilder.environment();
        hostEnv.putAll(env);
        
        // Command
        if(sudoExecuteScript)
            processBuilder.command("sudo", "-u", scriptUser, "-E", BASH_COMMAND, cloudProviderPath + File.separator + script);
        else
            processBuilder.command(BASH_COMMAND, cloudProviderPath + File.separator + script);
        // Working dir
        processBuilder.directory(new File(appFolder));
    }
    
    @SuppressWarnings("unchecked")
    List<String> dockerCmd(String appFolder, String deploymentsFolder, String cloudProviderPath, String script, Map<String, String> env) {
        
        return concat(
            
             asList("docker", "run" )
            , volume(appFolder         , CONTAINER_APP_FOLDER)           // appFolder
            , volume(deploymentsFolder , CONTAINER_DEPLOYMENTS_FOLDER)   // deploymentFolder
            , envToOpts(env),
             asList("-w", CONTAINER_APP_FOLDER)
            , asList( "--entrypoint", ""                                 // disable image entry-point
                   , "bioexcelhub/cpa-ecp-agent:tf-1.0.4_2"
                    ,BASH_COMMAND
                    , scriptPath(cloudProviderPath, script)              // script path
                    )
        );
    }
    
    @SuppressWarnings("unchecked")
    List<String> envToOpts(Map<String, String> env) {
        
      return chain(new ArrayList<>(env.entrySet()))
             .map(e -> asList("-e", e.toString()))
             .flatten()
             .value()
             ;
    }
    
    String scriptPath(String cloudProviderPath, String script) {
        
        return new File(
                     new File(CONTAINER_APP_FOLDER, cloudProviderPath)
                    ,script)
               .toString()
               ;
    }
    
    void setEnvDocker(Map<String, String> env, String reference) {
        
        env.put("PORTAL_DEPLOYMENTS_ROOT"       , CONTAINER_DEPLOYMENTS_FOLDER);
        env.put("PORTAL_APP_REPO_FOLDER"        , CONTAINER_APP_FOLDER);
        env.put("PORTAL_DEPLOYMENT_REFERENCE"   , reference);
    }
    
    void setEnvBash(Map<String, String> env, String reference, String deploymentsRoot, String repoPath) {
        
        env.put("PORTAL_DEPLOYMENTS_ROOT"       , deploymentsRoot);
        env.put("PORTAL_APP_REPO_FOLDER"        , repoPath);
        env.put("PORTAL_DEPLOYMENT_REFERENCE"   , reference);
    }
    
    void addKeyEnvVars(Map<String, String> env, String reference) {
        
        deploymentsRoot = this.docker ? CONTAINER_DEPLOYMENTS_FOLDER
                                      : this.deploymentsRoot
                                      ;
        
        String fileDestination = deploymentsRoot + File.separator + reference + File.separator + reference;
        
        String privateKeyPath = fileDestination;
        String publicKeyPath = privateKeyPath + ".pub";
        
        env.put("PRIVATE_KEY", privateKeyPath);
        env.put("PUBLIC_KEY", publicKeyPath);
        env.put("TF_VAR_private_key_path", privateKeyPath);
        env.put("TF_VAR_public_key_path", publicKeyPath);
        env.put("portal_private_key_path", privateKeyPath);
        env.put("portal_public_key_path", publicKeyPath);
        env.put("TF_VAR_portal_private_key_path", privateKeyPath);
        env.put("TF_VAR_portal_public_key_path", publicKeyPath);
    }
    
    List<String> volume(String folder, String mountPoint) {
        
        return folder == null ? asList()
                              : asList("-v", format("%s:%s", folder, mountPoint))
                              ;
    }
}
