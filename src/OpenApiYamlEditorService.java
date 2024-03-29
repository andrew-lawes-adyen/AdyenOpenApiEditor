import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class contains logic to perform edits to all OpenAPI YAML files identified within a given directory.
 */
public class OpenApiYamlEditorService
{
    // region STATIC VARIABLES

    public static final long MILLISECONDS_IN_ONE_HOUR = 3600000;

    // endregion

    // region INSTANCE VARIABLES

    // path to directory containing input .yaml files
    private final Path pathToInputDirectory;

    // path to subdirectory to hold all output files
    private final Path pathToAllVersionsOutputSubdirectory;

    // path to directory to hold latest versions of output files
    private final Path pathToLatestVersionsOutputSubdirectory;

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
        // set flag to copy latest versions into a specific subfolder
        this.copyLatestVersions = copyLatestVersions;

        // validate input directory
        try
        {
            this.pathToInputDirectory = validateAndGetPathToDirectory(pathToInputDirectory);
        }
        catch (Exception ex)
        {
            throw new Exception(
                    "Path to input directory was invalid - please recheck. Value supplied was " + pathToInputDirectory);
        }

        Path outputDirectory = null;

        // validate output directory
        try
        {
            outputDirectory = validateAndGetPathToDirectory(pathToOutputDirectory);
        }
        catch (Exception ex)
        {
            throw new Exception(
                    "Path to output directory was invalid - please recheck. Value supplied was " + pathToOutputDirectory);
        }

        // ensure output subdirectories exists
        try
        {
            this.pathToAllVersionsOutputSubdirectory = outputDirectory.resolve("all");
            this.pathToAllVersionsOutputSubdirectory.toFile().mkdir();

            this.pathToLatestVersionsOutputSubdirectory = outputDirectory.resolve("latest");
            this.pathToLatestVersionsOutputSubdirectory.toFile().mkdir();
        }
        catch (Exception ex)
        {
            throw new Exception("Could not create subdirectory in output folder: " + ex.getMessage());
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
            OpenApiYamlFile file = new OpenApiYamlFile(yamlFilePath, this.pathToAllVersionsOutputSubdirectory);

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
            throw new Exception("No .yaml files were found in this directory: " + pathToLatestVersionsOutputSubdirectory.toString());
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
        Set<Path> outputYamlFilePaths = getPathsToYamlFiles(this.pathToAllVersionsOutputSubdirectory);

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
                    this.pathToLatestVersionsOutputSubdirectory.resolve(pathToHighestVersionFile.getFileName()),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        }

        // get all files in output subdirectory
        Set<Path> latestYamlFilePaths = getPathsToYamlFiles(this.pathToLatestVersionsOutputSubdirectory);

        // filter only for stale files not updated in last hour
        Set<Path> staleFilePaths = latestYamlFilePaths.stream()
                .filter(path -> path.toFile().lastModified() < System.currentTimeMillis() - MILLISECONDS_IN_ONE_HOUR)
                .collect(Collectors.toSet());

        // delete stale files not updated in last hour
        for (Path staleFilePath : staleFilePaths)
        {
            Files.delete(staleFilePath);
        }
    }
}