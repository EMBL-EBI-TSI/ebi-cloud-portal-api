package uk.ac.ebi.tsc.portal.api.application.repo;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;


/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@Entity
public class ApplicationInput {

	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public String name;
   
    @ElementCollection
    @CollectionTable(
    		name = "application_input_values", 
    		joinColumns = @JoinColumn (name = "application_input_id"))
    @Column(name = "value")
    private List<String> values; 

    @ManyToOne
    public Application application;

    public ApplicationInput() {// jpa only
    }

    public ApplicationInput(String name) {
    	this.name = name;
    }
    
    public ApplicationInput(String name, Application application) {
        this.name = name;
        this.application = application;
        this.values = new LinkedList<>();
    }

    public String getName() {
        return name;
    }

    public Application getApplication() {
        return application;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getValues() {
		return values;
	}

	public void setValues(List<String> values) {
		this.values = values;
	}
}
