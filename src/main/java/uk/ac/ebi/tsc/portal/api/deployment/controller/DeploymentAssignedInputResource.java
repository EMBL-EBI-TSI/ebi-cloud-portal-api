package uk.ac.ebi.tsc.portal.api.deployment.controller;

import org.springframework.hateoas.ResourceSupport;
import org.springframework.stereotype.Component;

import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentAssignedInput;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
public class DeploymentAssignedInputResource extends ResourceSupport {

    private String inputName;
    private String assignedValue;

    public DeploymentAssignedInputResource() {
    }

    public DeploymentAssignedInputResource(DeploymentAssignedInput deploymentAssignedInput) {

        this.inputName = deploymentAssignedInput.getInputName();
        this.assignedValue = deploymentAssignedInput.getValue();

        this.add(
            linkTo(DeploymentRestController.class)
                .slash(deploymentAssignedInput.getDeployment().getReference()
            ).withRel("deployment")
        );

    }

    public String getInputName() {
        return inputName;
    }

    public String getAssignedValue() {
        return assignedValue;
    }
}
