import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class contains logic to perform edits to all OpenAPI YAML files identified within a given directory.
 */
public class OpenApiYamlEditorService
{
    // region INSTANCE VARIABLES

    // path to directory containing input .yaml files
    private final Path pathToInputDirectory;

    // path to directory to hold output .yaml files
    private final Path pathToOutputDirectory;

    // path to directory to hold latest versions of output files
    private final Path pathToLatestVersionsSubdirectory;

    // flag to determine if latest versions of API collections should be copied to a target subdirectory
    private final boolean copyLatestVersions;

    // set of paths to all .yaml files identified in input directory
    private Set<Path> inputYamlFilePaths;

    // endregion

    /**
     * Public constructor. Includes validation to check that directory paths are valid.
     *
     * @param pathToInputDirectory  String containing path to input directory containing source .yaml files
     * @param pathToOutputDirectory String containing path to output directory to store edited .yaml files
     * @param copyLatestVersions    boolean to control creation of subdirectory containing only latest versions
     * @throws Exception
     */
    public OpenApiYamlEditorService(String pathToInputDirectory, String pathToOutputDirectory, boolean copyLatestVersions)
            throws Exception
    {
        // set non-validated input parameters
        this.copyLatestVersions = copyLatestVersions;

        // validate other input parameters
        try
        {
            this.pathToInputDirectory = validateAndGetPathToDirectory(pathToInputDirectory);
        }
        catch (Exception ex)
        {
            throw new Exception(
                    "Path to input directory was invalid - please recheck. Value supplied was " + pathToInputDirectory);
        }

        try
        {
            this.pathToOutputDirectory = validateAndGetPathToDirectory(pathToOutputDirectory);
            this.pathToLatestVersionsSubdirectory = validateAndGetPathToDirectory(pathToOutputDirectory + "/_latest");
        }
        catch (Exception ex)
        {
            throw new Exception(
                    "Path to output directory was invalid - please recheck. Value supplied was " + pathToOutputDirectory);
        }
    }

    /**
     * Method to initiate and control overall flow of execution.
     */
    public void run()
            throws Exception
    {
        // get set of paths to YAML files in directory (includes validation checks)
        this.inputYamlFilePaths = getPathsToYamlFiles(this.pathToInputDirectory);

        // iterate over set of paths
        for (Path yamlFilePath : inputYamlFilePaths)
        {
            // create YAML file object from next path
            OpenApiYamlFile file = new OpenApiYamlFile(yamlFilePath, this.pathToOutputDirectory);

            // amend file and save to output directory
            file.amendAndSave();
        }

        // copy subset of latest files if requested
        if (this.copyLatestVersions)
        {
            copyLatestFiles();
        }
    }

    /**
     * Method to validate a string containing a directory and return a path object for that directory.
     *
     * @param directory String containing directory
     * @return Path to directory
     */
    private Path validateAndGetPathToDirectory(String directory)
            throws Exception
    {

        // create path object for supplied string - throws exception automatically if this doesn't map to a valid
        // filesystem object
        Path pathToDirectory = Path.of(directory);

        // confirm this path objects references a directory
        if (!Files.isDirectory(pathToDirectory))
        {
            throw new Exception("Value supplied does not reference a directory: " + directory);
        }

        return pathToDirectory;
    }

    /**
     * Method to get a set of paths to all YAML files identified within a directory. Throws exception if no YAML files
     * are found.
     */
    private Set<Path> getPathsToYamlFiles(Path directory)
            throws Exception
    {
        // get set of paths to YAML files in directory
        Set<Path> yamlFilePaths = Files.list(directory)
                .filter(path -> path.toString()
                        .endsWith(".yaml"))
                .collect(Collectors.toSet());

        // validation - throw exception if no YAML files found
        if (yamlFilePaths.size() == 0)
        {
            throw new Exception("No .yaml files were found in this directory: " + pathToLatestVersionsSubdirectory.toString());
        }

        return yamlFilePaths;
    }

    /**
     * Method to identify latest version of each API collection and copy just these files to a specified directory.
     */
    private void copyLatestFiles()
            throws Exception
    {
        // generate set of output files
        Set<Path> outputYamlFilePaths = getPathsToYamlFiles(this.pathToOutputDirectory);

        // create set of unique API collection names by removing "-vXX.yaml" from the end of the path
        Set<String> uniqueCollectionNames = outputYamlFilePaths.stream()
                .map(path -> path.getName(path.getNameCount() - 1)
                        .toString()
                        .replaceAll("(-v)\\d+(.yaml)", ""))
                .collect(Collectors.toSet());

        // prepare set to hold paths for latest versions of each collection
        Set<Path> highestVersions = new HashSet<>();

        // iterate over set of unique API collection names and find highest version for each
        for (String collectionName : uniqueCollectionNames)
        {
            // get set of paths containing this collection name
            Set<Path> collectionPaths = outputYamlFilePaths.stream()
                    .filter(path -> path.getName(path.getNameCount() - 1)
                            .toString()
                            .startsWith(collectionName))
                    .collect(Collectors.toSet());

            // search this set to find the highest version value
            int highestVersion = collectionPaths.stream()
                    .mapToInt(path -> Integer.parseInt(
                            path.getName(path.getNameCount() - 1)
                                    .toString()
                                    .replaceAll("[^\\d]", ""))
                    )
                    .max()
                    .orElseThrow();

            // get the path corresponding to this collection name and version number
            Path collectionPathHighestVersion =
                    collectionPaths.stream()
                            .filter(path -> path.getName(path.getNameCount() - 1)
                                    .toString()
                                    .contains("-v" + highestVersion))
                            .collect(Collectors.toList())
                            .get(0);

            // add this path to the set of paths to be copied
            highestVersions.add(collectionPathHighestVersion);
        }

        // copy all files to latest versions subdirectory
        for (Path pathToHighestVersionFile : highestVersions)
        {
            Files.copy(pathToHighestVersionFile,
                    this.pathToLatestVersionsSubdirectory.resolve(pathToHighestVersionFile.getFileName()),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        }
    }
}