package com.vfcorp.cae;

import com.coremedia.blueprint.cae.handlers.NavigationSegmentsUriHelper;
import com.vfcorp.cae.handler.NavigationResolverHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Starting point for the new java-based spring bean configuration
 * (which lives in parallel to the existing legacy xml configuration).
 *
 * @author Markus Schwarz
 */
@Configuration
public class VfCsvCaeConfiguration {

    @Bean
    public NavigationResolverHandler navigationResolverHandler(NavigationSegmentsUriHelper navigationSegmentsUriHelper){
        return new NavigationResolverHandler(navigationSegmentsUriHelper);
    }
}
