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

	// path to directory containing YAML files
	private final Path pathToYamlDirectory;

	// set of paths to all .yaml files identified in directory
	private Set<Path> yamlFilePaths;

	// flag to determine if latest versions of API collections should be copied to a target directory
	private final boolean copyLatestVersions;

	// target directory to copy latest versions of API collections to
	private final Path pathToLatestVersionsDirectory;

	// endregion

	/**
	 * Public constructor. Includes validation to check that directory paths are valid and throws exception if any
	 * issues are found.
	 * fails.
	 *
	 * @param yamlDirectory           String containing path to directory containing .yaml files
	 * @param latestVersionsDirectory String containing path to directory used to store latest API collections
	 *                                (optional - may be null)
	 */
	public OpenApiYamlEditorService(String yamlDirectory, String latestVersionsDirectory)
			throws Exception
	{
		// validate mandatory input for path to directory containing .yaml files
		try
		{
			this.pathToYamlDirectory = validateAndGetPathToDirectory(yamlDirectory);
		}
		catch (Exception ex)
		{
			throw new Exception(
					"Path to directory containing YAML files was invalid - please recheck. Value supplied was " + yamlDirectory);
		}

		// validate optional input for target directory to copy latest API collections to (if supplied)
		if (latestVersionsDirectory != null && !latestVersionsDirectory.isEmpty())
		{
			try
			{
				this.pathToLatestVersionsDirectory = validateAndGetPathToDirectory(latestVersionsDirectory);
				this.copyLatestVersions = true;
			}
			catch (Exception ex)
			{
				throw new Exception(
						"Path to subset copy directory was invalid - please recheck. Value supplied was " + latestVersionsDirectory);
			}
		} else
		{
			this.pathToLatestVersionsDirectory = null;
			this.copyLatestVersions = false;
		}
	}

	/**
	 * Method to initiate and control overall flow of execution.
	 */
	public void run()
			throws Exception
	{
		// get set of paths to YAML files in directory (includes validation checks)
		getPathsToYamlFiles();

		// iterate over set of paths
		for (Path yamlFilePath : yamlFilePaths)
		{
			// create YAML file object from next path
			OpenApiYamlFile file = new OpenApiYamlFile(yamlFilePath);

			// amend file
			file.amend();
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
			throw new Exception();
		}

		return pathToDirectory;
	}

	/**
	 * Method to get a set of paths to all YAML files identified within a directory. Throws exception if no YAML files
	 * are found.
	 */
	private void getPathsToYamlFiles()
			throws Exception
	{
		// get set of paths to YAML files in directory
		this.yamlFilePaths = Files.list(this.pathToYamlDirectory)
		                          .filter(path -> path.toString()
		                                              .endsWith(".yaml"))
		                          .collect(Collectors.toSet());

		// validation - throw exception if no YAML files found
		if (this.yamlFilePaths.size() == 0)
		{
			throw new Exception("No .yaml files were found in this directory - please recheck and rerun");
		}
	}

	/**
	 * Method to identify latest version of each API collection and copy just these files to a specified directory.
	 */
	private void copyLatestFiles()
			throws Exception
	{
		// create set of unique API collection names by removing "-vXX.yaml" from the end of the path
		Set<String> uniqueCollectionNames = this.yamlFilePaths.stream()
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
			Set<Path> collectionPaths = this.yamlFilePaths.stream()
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

		// copy all files to latest versions directory
		for (Path pathToHighestVersionFile : highestVersions)
		{
			Files.copy(pathToHighestVersionFile,
			           this.pathToLatestVersionsDirectory.resolve(pathToHighestVersionFile.getFileName()),
			           StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
		}
	}
}
