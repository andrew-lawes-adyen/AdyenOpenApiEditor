import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class OpenApiYamlFile
{
	// static variables
	public static final List<String> REQUEST_LEVEL_AUTHENTICATION = List.of(
			"      security:",
			"      - BasicAuth: []",
			"      - ApiKeyAuth: []"
	);

	public static final List<String> COLLECTION_LEVEL_AUTHENTICATION_API_KEY = List.of(
			"security:",
			"  - ApiKeyAuth: []"
	);

	// instance variables
	private final Path pathToFile;
	private List<String> lines;

	public OpenApiYamlFile(Path pathToFile)
	{

		this.pathToFile = pathToFile;
	}

	public void amend()
			throws Exception
	{
		// get file content as list of strings
		this.lines = Files.readAllLines(this.pathToFile);

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

	private void addVariableToBaseUrl()
	{
		// add variable environment to url property
		// enables use of environment variables to change environment
		try
		{
			String urlOriginal = this.lines.stream()
			                               .filter(line -> line.startsWith("- url:"))
			                               .collect(Collectors.toList())
			                               .get(0);

			String urlUpdated = urlOriginal.replace("test", "{{env}}");

			this.lines.set(this.lines.indexOf(urlOriginal), urlUpdated);
		}
		catch (Exception ex)
		{
			// suppress exception - API definitions for notification services/webhooks do not contain this URL
		}
	}

	private void removeRequestLevelAuthentication()
	{
		// delete request-specific authentication details
		// ensures Postman defaults each request to inheriting auth from parent
		this.lines.removeAll(REQUEST_LEVEL_AUTHENTICATION);
	}

	private void setDefaultAuthenticationToApiKey()
	{
		// add additional lines to bottom of file (if not found)
		// ensures Postman defaults entire collection to using API key
		if (!this.lines.containsAll(COLLECTION_LEVEL_AUTHENTICATION_API_KEY))
		{
			this.lines.addAll(COLLECTION_LEVEL_AUTHENTICATION_API_KEY);
		}
	}

	private void updateCollectionTitleWithVersion()
	{

		String versionLine = this.lines.stream()
		                               .filter(line -> line.startsWith("  version:"))
		                               .collect(Collectors.toList())
		                               .get(0);

		Integer version = Integer.valueOf(versionLine.split("'")[1]);

		String titleOriginal = this.lines.stream()
		                                 .filter(line -> line.startsWith("  title:"))
		                                 .collect(Collectors.toList())
		                                 .get(0);

		String titleUpdated = titleOriginal + " [v" + version + "]";

		this.lines.set(this.lines.indexOf(titleOriginal), titleUpdated);
	}

	private void writeChangesToFile()
			throws Exception
	{

		try (PrintWriter fileWriter = new PrintWriter(Files.newBufferedWriter(this.pathToFile)))
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
