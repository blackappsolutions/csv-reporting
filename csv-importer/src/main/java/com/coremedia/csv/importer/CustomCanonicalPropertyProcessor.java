package com.coremedia.csv.importer;

import com.coremedia.blueprint.common.contentbeans.CMLinkable;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.content.ContentType;
import com.coremedia.cap.struct.Struct;
import com.coremedia.cap.struct.StructBuilder;
import com.coremedia.cap.struct.StructService;
import com.vfcorp.csv.common.VfCsvConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;

import static com.coremedia.csv.common.CSVConstants.PROPERTY_LOCAL_SETTINGS;
import static com.vfcorp.csv.common.VfCsvConstants.CUSTOM_CANONICAL;

/**
 * This processor tries
 * <ul>
 *  <li>to resolve a url path using {@link com.vfcorp.cae.handler.NavigationResolverHandler}</li>
 *  <li>to a navigationId AND</li>
 *  <li>set that id into the customCanonical LinkProperty in localSettings</li>
 * </ul>
 * during CSV Imports.
 * <p>
 * Sample:
 * <Struct xmlns="http://www.coremedia.com/2008/struct" xmlns:xlink="http://www.w3.org/1999/xlink">
 *   <StringProperty Name="headerVisibility">hidden</StringProperty>
 *    ...
 *   <LinkProperty Name="customCanonical" LinkType="coremedia:///cap/contenttype/CMLinkable" xlink:href="coremedia:///cap/content/190882"/>
 * </Struct>
 *
 * @author Markus Schwarz
 */
public class CustomCanonicalPropertyProcessor implements PropertyValueObjectProcessor {
    private final StructService structService;
    private final ContentType cmLinkableType;
    private ContentRepository contentRepository;
    private String previewRestUrlPrefix;
    private Logger logger;

    public CustomCanonicalPropertyProcessor(String previewRestUrlPrefix, Logger logger, ContentRepository contentRepository) {
        this.previewRestUrlPrefix = previewRestUrlPrefix;
        this.logger = logger;
        this.contentRepository = contentRepository;
        this.structService = contentRepository.getConnection().getStructService();
        this.cmLinkableType = contentRepository.getContentType(CMLinkable.NAME);
    }

    /**
     * @param content             The content for which to process the property
     * @param propertyName        Name of property
     * @param propertyValueObject Value of property, eg. aaa/bbb/ccc
     * @return a Markup String
     */
    @Override
    public Object process(Content content, String propertyName, Object propertyValueObject) {
        if (!StringUtils.isEmpty(propertyValueObject.toString())) {
            String navigationID = getNavigationID(propertyValueObject);
            if (navigationID != null) {
                Content navigation = contentRepository.getContent(navigationID);
                if (navigation != null) {
                    Struct localSettings = getLocalSettings(content);
                    StructBuilder structBuilder = getStructBuilder(localSettings);
                    setCustomCanonical(navigation, localSettings, structBuilder);
                    return structBuilder.build().toMarkup().toString();
                }
            }
        }
        return "";
    }

    private void setCustomCanonical(Content navigation, Struct localSettings, StructBuilder structBuilder) {
        if (localSettings.get(CUSTOM_CANONICAL) == null) {
            structBuilder.declareLink(CUSTOM_CANONICAL, cmLinkableType, navigation);
        } else {
            structBuilder.set(CUSTOM_CANONICAL, navigation);
        }
    }

    private StructBuilder getStructBuilder(Struct localSettings) {
        StructBuilder structBuilder = structService.createStructBuilder();
        structBuilder.setAll(localSettings.toNestedMaps());
        return structBuilder;
    }

    private Struct getLocalSettings(Content content) {
        Struct localSettings = content.getStruct(PROPERTY_LOCAL_SETTINGS);
        if (localSettings == null) {
            localSettings = structService.emptyStruct();
        }
        return localSettings;
    }

    private String getNavigationID(Object propertyValueObject) {
        if (previewRestUrlPrefix != null) {
            HttpGet httpGet = null;
            try {
                CloseableHttpClient client = HttpClients.createDefault();
                String requestUrl = previewRestUrlPrefix + VfCsvConstants.GET_NAVIGATION_FOR_URI_PATH;
                URIBuilder builder = new URIBuilder(requestUrl);
                builder.setParameter("uriPath", propertyValueObject.toString());
                httpGet = new HttpGet(builder.build());
                HttpResponse response = client.execute(httpGet);
                if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
                    return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                logger.error("Error resolving navigation", e);
            } finally {
                if (httpGet != null) httpGet.releaseConnection();
            }
        }
        return null;
    }
}
