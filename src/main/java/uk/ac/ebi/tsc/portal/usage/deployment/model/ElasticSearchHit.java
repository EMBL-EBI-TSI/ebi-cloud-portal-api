package uk.ac.ebi.tsc.portal.usage.deployment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 **/

@JsonIgnoreProperties(ignoreUnknown = true)
public class ElasticSearchHit {
    public String _index;
    public String _type;
    public int _score;
    public DeploymentDocument _source;
}
