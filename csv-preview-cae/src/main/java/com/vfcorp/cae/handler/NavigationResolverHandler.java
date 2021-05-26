package com.vfcorp.cae.handler;

import com.coremedia.blueprint.cae.handlers.NavigationSegmentsUriHelper;
import com.coremedia.blueprint.common.navigation.Navigation;
import com.vfcorp.csv.common.VfCsvConstants;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Handler that tries to resolve a path-segment to a navigation-resource.
 *
 * Currently used from {@link com.coremedia.csv.importer.CustomCanonicalPropertyProcessor}
 *
 * Sample URL for local testing: http://localhost:40980/blueprint/servlet/getNavigationForUriPath?uriPath=blackapp-de-de/ccc
 *
 * @author Markus Schwarz
 */
@RequestMapping
public class NavigationResolverHandler {

    protected static final String NO_NAVIGATION_FOUND_MSG = "No navigation could be found for '%s'.";
    private NavigationSegmentsUriHelper navigationSegmentsUriHelper;

    public NavigationResolverHandler(NavigationSegmentsUriHelper navigationSegmentsUriHelper) {
        this.navigationSegmentsUriHelper = navigationSegmentsUriHelper;
    }

    @GetMapping(value = VfCsvConstants.GET_NAVIGATION_FOR_URI_PATH)
    @ResponseBody
    public Object getNavigationForUriPath(@RequestParam(value = "uriPath") String uriPath) {
        Navigation navigation = navigationSegmentsUriHelper.parsePath(uriPath);
        return (navigation == null)
            ? ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format(NO_NAVIGATION_FOUND_MSG, uriPath))
            : String.valueOf(navigation.getContext().getContentId());
    }
}
