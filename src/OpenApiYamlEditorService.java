import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public class OpenApiYamlEditorService
{
	// instance variables
	private Path pathToYamlDirectory;

	private boolean copyLatestVersions;
	private Path pathToLatestVersionsDirectory;

	private Set<Path> yamlFilePaths;

	public OpenApiYamlEditorService(String yamlDirectory, String latestVersionsDirectory)
			throws Exception
	{
		// validate mandatory input
		try
		{
			this.pathToYamlDirectory = validateAndGetPathToDirectory(yamlDirectory);
		}
		catch (Exception ex)
		{
			throw new Exception(
					"Path to YAML directory was invalid - please recheck. Value supplied was " + yamlDirectory);
		}

		// validate optional input
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

	private Path validateAndGetPathToDirectory(String directory)
			throws Exception
	{

		Path pathToDirectory = Path.of(directory);

		if (!Files.isDirectory(pathToDirectory))
		{
			throw new Exception();
		}

		return pathToDirectory;
	}

	public void run()
			throws Exception
	{
		// get set of paths to YAML files in directory (includes validation checks)
		getPathsToYamlFiles();

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
	 * Method to get a set of paths to all YAML files identified within a directory. Throws exception if no YAML files
	 * are found.
	 *
	 * @return Set of paths to all YAML files identified in specified directory
	 * @throws Exception
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

		// iterate over unique collection names and find highest version for each
		for (String collectionName : uniqueCollectionNames)
		{
			// get set of paths containing this collection name
			Set<Path> collectionPaths = this.yamlFilePaths.stream()
			                                              .filter(path -> path.getName(path.getNameCount() - 1)
			                                                                  .toString()
			                                                                  .startsWith(collectionName))
			                                              .collect(Collectors.toSet());

			// search this set to find the highest version value
			Integer highestVersion = collectionPaths.stream()
			                                        .mapToInt(path -> Integer.valueOf(
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
