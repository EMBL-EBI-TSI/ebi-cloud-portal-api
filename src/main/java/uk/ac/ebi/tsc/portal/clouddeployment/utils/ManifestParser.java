package uk.ac.ebi.tsc.portal.clouddeployment.utils;

import java.io.File;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import uk.ac.ebi.tsc.portal.api.application.repo.ApplicationInput;
import uk.ac.ebi.tsc.portal.clouddeployment.model.ApplicationManifest;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@Component
public class ManifestParser {

    public static ApplicationManifest parseApplicationManifest(String manifestFilePath) {
        
        try 
        {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(new File(manifestFilePath), ApplicationManifest.class);
        } 
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
