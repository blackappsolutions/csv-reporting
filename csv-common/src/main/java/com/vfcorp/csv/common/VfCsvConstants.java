package com.vfcorp.csv.common;

import static com.coremedia.csv.common.CSVConstants.PROPERTY_LOCAL_SETTINGS;

/**
 * @author Markus Schwarz
 */
public interface VfCsvConstants {

    /**
     * Common URL-Slug used by client and server.
     */
    String GET_NAVIGATION_FOR_URI_PATH = "/getNavigationForUriPath";
    /**
     * <LinkProperty Name="customCanonical" LinkType="coremedia:///cap/contenttype/CMLinkable" xlink:href=".."/>
     */
    String CUSTOM_CANONICAL = "customCanonical";
    /**
     * You can use
     *    'localSettings.customCanonical'
     * in
     *    /Settings/Options/Settings/ReportingSettings
     * to
     *  enable this special property-property-handling in imports/exports.
     */
    String PROPERTY_CUSTOM_CANONICAL = PROPERTY_LOCAL_SETTINGS + "." + CUSTOM_CANONICAL;
}
