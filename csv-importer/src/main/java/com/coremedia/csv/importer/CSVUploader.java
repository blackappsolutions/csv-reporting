package com.coremedia.csv.importer;

import com.coremedia.cap.Cap;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.user.Group;
import com.coremedia.cap.user.User;
import com.coremedia.cap.user.UserRepository;
import com.coremedia.cmdline.AbstractSpringAwareUAPIClient;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * This client moves content located in one directory into another
 */
public class CSVUploader extends AbstractSpringAwareUAPIClient {

    /**
     * Short version of the command line argument that specifies the source directory.
     */
    private static final String SOURCE_CSV_PARAMETER_SHORT = "s";

    /**
     * Long version of the command line argument that specifies the source directory.
     */
    private static final String SOURCE_CSV_PARAMETER_LONG = "source";

    /**
     * User description of the source directory parameter.
     */
    private static final String SOURCE_CSV_DESCRIPTION = "The path to the CSV file that which contains the content " +
            "to be uploaded/updated";

    /**
     * Short version of the command line argument that specifies if the updated content should be automatically published if the prior version was published.
     */
    private static final String AUTO_PUBLISH_PARAMETER_SHORT = "ap";

    /**
     * Long version of the command line argument that specifies if the updated content should be automatically published if the prior version was published.
     */
    private static final String AUTO_PUBLISH_PARAMETER_LONG = "autopublish";

    /**
     * User description of the autopublish parameter.
     */
    private static final String AUTO_PUBLISH_DESCRIPTION = "Use/specify if updated content should be automatically published if the prior version was published.";

    /**
     * Error message when the source CSV file does not exist or is a folder.
     */
    private static final String SOURCE_CSV_ERROR_DNE = "ERROR: The specified CSV file is a folder or does not" +
            " exist.\nFile specified: %s.";

    /**
     * Error message when the source CSV file is not a proper CSV file.
     */
    private static final String SOURCE_CSV_ERROR_NOT_CSV = "ERROR: The specified file is not a CSV.\nFile " +
            "specified: %s.";

  /**
   * Error message when the user is not authorized to perform the import.
   */
  private static final String USER_NOT_AUTHORIZED = "Cannot perform CSV Import. Unauthorized.";

    /**
     * Error message when parsing the CSV file fails.
     */
    private static final String ERROR_PARSING_CSV = "ERROR: An error occurred while trying to parse the CSV file, %s." +
            "\nReason: %s";

    /**
     * The source CSV file.
     */
    private String sourceCSV;

    /**
     * If updated content should be automatically be published if prior version was published.
     */
    private boolean autoPublish;

    /**
     * A relational map consisting of the names of the CSV headers and their corresponding content property names.
     */
    private Map<String, String> reportHeadersToContentProperties;

    /**
     * Logger for this class.
     */
    private final Logger logger;

    /**
     * The handler class which will parse the CSV and import the data into the respective content.
     */
    private CSVParserHelper csvHandler;

  /**
   * Flag indicating whether access to this endpoint should be restricted to authorized groups only
   */
  private Boolean restrictToAuthorizedGroups;

  /**
   * The Authorized Uer Groups which are allowed to conduct an import.
   */
  private List<String> authorizedGroups;

    /**
     * Constructor.
     */
    public CSVUploader() {
        sourceCSV = null;
        logger = LoggerFactory.getLogger(CSVUploader.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fillInOptions(Options options) {
        options.addOption(OptionBuilder.hasArg()
                .withDescription(SOURCE_CSV_DESCRIPTION)
                .withLongOpt(SOURCE_CSV_PARAMETER_LONG)
                .isRequired(true)
                .hasArg(true)
                .create(SOURCE_CSV_PARAMETER_SHORT));
        options.addOption(OptionBuilder.hasArg()
                .withDescription(AUTO_PUBLISH_DESCRIPTION)
                .withLongOpt(AUTO_PUBLISH_PARAMETER_LONG)
                .isRequired(false)
                .hasArg(false)
                .create(AUTO_PUBLISH_PARAMETER_SHORT));
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    protected String getUsage() {
        return "cm csv-uploader -u <user> [other options] [--" + AUTO_PUBLISH_PARAMETER_LONG + "] --" + SOURCE_CSV_PARAMETER_LONG
                + " <source CSV file>";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean parseCommandLine(CommandLine commandLine) {
        // Pull args from command line
        sourceCSV = commandLine.getOptionValue(SOURCE_CSV_PARAMETER_SHORT);
        autoPublish = commandLine.hasOption(AUTO_PUBLISH_PARAMETER_SHORT);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getApplicationContextPath() {
        return "classpath:/META-INF/coremedia/component-csv-uploader.xml";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void enableVerboseLogging(boolean verbose) {
        String newLevel = verbose ? "debug" : "info";
        String oldLevel = System.setProperty("stdout.log.level", newLevel);
        boolean levelChanged = !newLevel.equalsIgnoreCase(oldLevel);
        if (levelChanged) {
            this.reloadLoggingConfiguration();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Logger getLogger() {
        return logger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fillInConnectionParameters(Map<String, Object> params) {
        super.fillInConnectionParameters(params);
        params.put(Cap.USE_WORKFLOW, "false");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void run() {

      restrictToAuthorizedGroups = getApplicationContext().getBean("restrictToAuthorizedGroups", Boolean.class);
      authorizedGroups = getApplicationContext().getBean("authorizedGroups", List.class);

      // Check that the user is a member of the requisite group
      if(restrictToAuthorizedGroups && !isAuthorized()) {
        getLogger().error(USER_NOT_AUTHORIZED);
      }
      else {
        reportHeadersToContentProperties = getApplicationContext().getBean("reportHeadersToContentProperties",
                Map.class);

        // Verify that the source CSV file is a CSV
        File csvFile = new File(sourceCSV);
        if (!csvFile.exists()) {
          getLogger().error(String.format(SOURCE_CSV_ERROR_DNE, sourceCSV));
        } else if (csvFile.isDirectory()) {
          getLogger().error(String.format(SOURCE_CSV_ERROR_NOT_CSV, sourceCSV));
        } else {
          try {
            // Pass the CSV to the CSVParser
            FileInputStream fileStream = new FileInputStream(csvFile);
            CSVParser parser = new CSVParser(new BufferedReader(new InputStreamReader(fileStream, StandardCharsets.UTF_8)),
                    CSVFormat.EXCEL.withHeader());
            csvHandler = new CSVParserHelper(autoPublish, getContentRepository(), logger, null);
            logger.info("CSVParser: executing ...");
            csvHandler.parseCSV(parser, reportHeadersToContentProperties);
            logger.info("CSVParser: Completed content upload.");
          } catch (IOException e) {
            getLogger().error(String.format(ERROR_PARSING_CSV, e.getMessage(), e));
          }
        }
      }
    }

  /**
   * Checks whether the current user is authorized to initiate a CSV export.
   *
   * @return whether the current user is authorized to initiate a CSV export
   */
  private boolean isAuthorized() {
    if (this.authorizedGroups == null || this.authorizedGroups.isEmpty())
      return false;

    ContentRepository contentRepository = getContentRepository();
    User user = contentRepository.getConnection().getSession().getUser();
    UserRepository userRepository = contentRepository.getConnection().getUserRepository();
    for (String authorizedGroupName : authorizedGroups) {
      Group group = userRepository.getGroupByName(authorizedGroupName);
      if (group != null && user.isMemberOf(group)) {
        return true;
      }
    }
    return false;
  }

    public void runFromRequest(InputStream fileInputStream) {
      reportHeadersToContentProperties = getApplicationContext().getBean("reportHeadersToContentProperties",
              Map.class);

      try {
        // Pass the CSV to the CSVParser
        CSVParser parser = new CSVParser(new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8)),
                CSVFormat.EXCEL.withHeader());
        csvHandler = new CSVParserHelper(autoPublish, getContentRepository(), logger, null);
        logger.info("CSVParser: executing ...");
        csvHandler.parseCSV(parser, reportHeadersToContentProperties);
        logger.info("CSVParser: Completed content upload.");
      } catch (IOException e) {
        getLogger().error(String.format(ERROR_PARSING_CSV, e.getMessage(), e));
      }
  }

    /**
     * Sets the report headers to content properties map.
     *
     * @param reportHeadersToContentProperties the map to set as the report headers to content properties map
     */
    @Required
    public void setReportHeadersToContentProperties(Map<String, String> reportHeadersToContentProperties) {
        this.reportHeadersToContentProperties = reportHeadersToContentProperties;
    }

  /**
   * Sets the authorized groups.
   *
   * @param authorizedGroups the authorized groups to set
   */
  public void setAuthorizedGroups(List<String> authorizedGroups) {
    this.authorizedGroups = authorizedGroups;
  }

  /**
   * Set the flag indicating whether access to this endpoint should be restricted to authorized groups only.
   *
   * @param restrictToAuthorizedGroups the value to set
   */
  public void setRestrictToAuthorizedGroups(boolean restrictToAuthorizedGroups) {
    this.restrictToAuthorizedGroups = restrictToAuthorizedGroups;
  }

    /**
     * Main function called from the command line. Passes the arguments to the class.
     *
     * @param args the arguments to pass to the ContentMover
     */
    public static void main(String[] args) {
        main(new CSVUploader(), args);
    }
}
