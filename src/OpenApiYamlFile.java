import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class represents a single YAML file and contains the logic required to amend it.
 */
public class OpenApiYamlFile
{
    // region STATIC VARIABLES

    // list of strings representing lines associated with request-level authentication
    public static final List<String> REQUEST_LEVEL_AUTHENTICATION = List.of(
            "      security:",
            "      - BasicAuth: []",
            "      - ApiKeyAuth: []"
    );

    // list of strings representing lines associated with collection-level authentication
    public static final List<String> COLLECTION_LEVEL_AUTHENTICATION_API_KEY = List.of(
            "security:",
            "  - ApiKeyAuth: []"
    );

    // endregion


    // region INSTANCE VARIABLES

    // path to YAML file
    private final Path pathToInputFile;

    // path to output directory
    private final Path pathToOutputFile;

    // list of lines within YAML files
    private List<String> lines;

    // endregion

    /**
     * Public constructor.
     *
     * @param pathToInputFile Path to YAML file
     */
    public OpenApiYamlFile(Path pathToInputFile, Path pathToOutputDirectory)
    {
        // store path to input file
        this.pathToInputFile = pathToInputFile;

        // resolve path to output file
        this.pathToOutputFile = pathToOutputDirectory.resolve(pathToInputFile.getFileName());
    }

    /**
     * Method to amend and save file.
     */
    public void amendAndSave()
            throws Exception
    {
        // get file content as list of strings
        this.lines = Files.readAllLines(this.pathToInputFile);

        // add variable to base URL
        addVariableToBaseUrl();

        // remove request-level authentication
        removeRequestLevelAuthentication();

        // set default authentication for the collection to use API key
        setDefaultAuthenticationToApiKey();

        // update title to add version
        updateCollectionTitleWithVersion();

        // write changes to file
        writeChangesToFile();
    }

    /**
     * Method to add variable to collection URL by inserting a variable into the string.
     */
    private void addVariableToBaseUrl()
    {
        // add variable environment to url property
        // enables use of environment variables to change environment
        try
        {
            // identify line containing URL for this collection
            String urlOriginal = this.lines.stream()
                    .filter(line -> line.startsWith("- url:"))
                    .collect(Collectors.toList())
                    .get(0);

            // replace hard-coded value with variable
            String urlUpdated = urlOriginal.replace("test", "{{env}}");

            // overwrite original line
            this.lines.set(this.lines.indexOf(urlOriginal), urlUpdated);
        }
        catch (Exception ex)
        {
            // suppress exception - API definitions for notification services/webhooks do not contain this URL
        }
    }

    /**
     * Method to remove request-level authentication by deleting lines where this is specified.
     */
    private void removeRequestLevelAuthentication()
    {
        // delete request-specific authentication details
        // ensures Postman defaults each request to inheriting auth from parent
        this.lines.removeAll(REQUEST_LEVEL_AUTHENTICATION);
    }

    /**
     * Method to set collection-level authentication to API key as default by inserting lines.
     */
    private void setDefaultAuthenticationToApiKey()
    {
        // add additional lines to bottom of file (if not found)
        // ensures Postman defaults entire collection to using API key
        if (!this.lines.containsAll(COLLECTION_LEVEL_AUTHENTICATION_API_KEY))
        {
            this.lines.addAll(COLLECTION_LEVEL_AUTHENTICATION_API_KEY);
        }
    }

    /**
     * Method to append version of collection to title in square brackets.
     */
    private void updateCollectionTitleWithVersion()
    {
        // get line containing collection version
        String versionLine = this.lines.stream()
                .filter(line -> line.startsWith("  version:"))
                .collect(Collectors.toList())
                .get(0);

        // get version number from line
        int version = Integer.parseInt(versionLine.split("'")[1]);

        // get line containing collection title
        String titleOriginal = this.lines.stream()
                .filter(line -> line.startsWith("  title:"))
                .collect(Collectors.toList())
                .get(0);

        // create updated line by appending version number to collection title within square brackets
        String titleUpdated = titleOriginal.substring(0, titleOriginal.length() - 1) + " [v" + version + "]'";

        // overwrite line containing collection title
        this.lines.set(this.lines.indexOf(titleOriginal), titleUpdated);
    }

    /**
     * Method to write changes to the file once completed.
     */
    private void writeChangesToFile()
            throws Exception
    {

        // create PrintWriter object with automatic closure
        try (PrintWriter fileWriter = new PrintWriter(Files.newBufferedWriter(this.pathToOutputFile)))
        {
            // erase file content
            fileWriter.write("");
            fileWriter.flush();

            // write each line to file
            for (String line : this.lines)
            {
                fileWriter.println(line);
            }

            // flush writer to complete writing data to file
            fileWriter.flush();
        }
    }
}