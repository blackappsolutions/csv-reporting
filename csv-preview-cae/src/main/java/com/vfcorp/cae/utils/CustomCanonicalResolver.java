package com.vfcorp.cae.utils;

import com.coremedia.blueprint.cae.handlers.NavigationSegmentsUriHelper;
import com.coremedia.blueprint.common.contentbeans.CMLinkable;
import com.coremedia.blueprint.common.navigation.Navigation;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.struct.Struct;
import com.coremedia.csv.cae.utils.BaseCSVUtil;
import com.coremedia.csv.common.CSVConstants;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.vfcorp.csv.common.VfCsvConstants.CUSTOM_CANONICAL;
import static com.vfcorp.csv.common.VfCsvConstants.PROPERTY_CUSTOM_CANONICAL;

/**
 * Custom VF Import-Property-Resolver
 *
 * Transforms a Link to a Navigation in localSettings.customCanonical to its url-path representation (e.g. aaa/bbb).
 *
 * @author Markus Schwarz
 */
public class CustomCanonicalResolver extends BaseCSVUtil {

    private NavigationSegmentsUriHelper navigationSegmentsUriHelper;

    /**
     * @param csvRecord     the CSV record to which to populate the properties of the content
     * @param content       the content from which the property values will be parsed
     * @param headerList    the list of headers which determines which metadata is added to the CSV record and which
     * @param propertiesMap the mapping list from Header-Name to Property-name
     */
    @Override
    protected void populateCustomPropertyFields(Map<String, String> csvRecord, Content content,
                                                List<String> headerList, Map<String, String> propertiesMap) {

        Optional<Map.Entry<String, String>> customCanonical = getCustomCanonical(propertiesMap);

        if (customCanonical.isEmpty()) {
            return;
        }

        Struct struct = content.getStruct(CSVConstants.PROPERTY_LOCAL_SETTINGS);
        if (struct == null) {
            return;
        }

        Content link = (Content) struct.get(CUSTOM_CANONICAL);
        if (link == null) {
            return;
        }

        CMLinkable cmLinkable = contentBeanFactory.createBeanFor(link, CMLinkable.class);
        if (cmLinkable instanceof Navigation) {
            putCanonicalUrlInCsvRecord(csvRecord, customCanonical, (Navigation) cmLinkable);
        }
    }

    private Optional<Map.Entry<String, String>> getCustomCanonical(Map<String, String> propertiesMap) {
        return propertiesMap.entrySet().stream()
            .filter(entry -> entry.getValue().equals(PROPERTY_CUSTOM_CANONICAL))
            .findFirst();
    }

    private void putCanonicalUrlInCsvRecord(
        Map<String, String> csvRecord, Optional<Map.Entry<String, String>> customCanonical, Navigation navigation
    ) {
        List<String> pathList = navigationSegmentsUriHelper.getPathList(navigation);
        String customCanonicalPath = String.join("/", pathList);
        csvRecord.put(customCanonical.get().getKey(), customCanonicalPath);
    }

    public void setNavigationSegmentsUriHelper(NavigationSegmentsUriHelper navigationSegmentsUriHelper) {
        this.navigationSegmentsUriHelper = navigationSegmentsUriHelper;
    }
}
