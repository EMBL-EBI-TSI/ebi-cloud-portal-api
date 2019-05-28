package uk.ac.ebi.tsc.portal.api.application.controller;

import java.util.Collection;
import java.util.LinkedList;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import uk.ac.ebi.tsc.portal.api.application.repo.ApplicationInput;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationInputResource extends ResourceSupport {

    private Long id;
    private String name;
    private Collection<String> values;

    // we need this for usage with the @RequestBody annotation
    public ApplicationInputResource() {
    }
    
    public ApplicationInputResource(ApplicationInput applicationInput) {

        this.id = applicationInput.getId();
        this.name = applicationInput.getName();
        if(!applicationInput.getValues().isEmpty()) {
            this.values = new LinkedList<>();
            this.values.addAll(applicationInput.getValues());
        }
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Collection<String> getValues() {
		return values;
	}

	public void setValues(Collection<String> values) {
		this.values = values;
	}
}
