package uk.ac.ebi.tsc.portal.clouddeployment.application;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class DeploymentStrategyTest {

    @Test
    public void scriptPath() throws Exception {
        DeploymentStrategy deployer = new DeploymentStrategy();
        assertEquals("/app/ostack/deploy.sh", deployer.scriptPath("ostack", "deploy.sh"));
    }

    @Test
    public void dockerCmd() throws Exception {
        DeploymentStrategy deployer = new DeploymentStrategy();
        Map<String, String> env = new HashMap<String, String>();
        env.put("a", "1");
        env.put("b", "2");
        List<String> cmd = deployer.dockerCmd("/var/ecp/myapp", "/var/ecp/deployments", "ostack", "deploy.sh", env);

        assertEquals(asList(
                "docker", "run", "-v", "/var/ecp/myapp:/app"
                , "-v", "/var/ecp/deployments:/deployments"
                , "-e", "a=1"
                , "-e", "b=2"
                , "-w", "/app"
                , "--entrypoint", ""
                , "bioexcelhub/cpa-ecp-agent:tf-1.0.4_2"
                , "bash"
                , "/app/ostack/deploy.sh"
                )
                , cmd);
    }

    @Test
    public void envToOpts() throws Exception {

        DeploymentStrategy strategy = new DeploymentStrategy();
        Map<String, String> env = new HashMap<String, String>();
        env.put("a", "1");
        env.put("b", "2");
        List<String> r = strategy.envToOpts(env);
        assertEquals(asList("-e", "a=1", "-e", "b=2"), r);
    }

    @Test
    public void volume() throws Exception {
        DeploymentStrategy deployer = new DeploymentStrategy();
        assertEquals(asList("-v", "/var/ecp/myapp:/app"), deployer.volume("/var/ecp/myapp", "/app"));
        assertEquals(asList(), deployer.volume(null, "/app"));
    }
}
