/**
 * This class contains the main method to initiate execution of the application. Includes basic validation checks
 * against input arguments.
 */
public class EditorApp
{
	public static void main(String[] args)
			throws Exception
	{

		try
		{
			// confirm arguments have been provided
			if (args.length == 0)
			{
				throw new Exception("At least one argument specifying the location of the directory containing Adyen " +
						                    "OpenAPI .yaml files is required!");
			}

			// extract path to YAML directory
			String yamlDirectory = args[0];

			// check if a second argument has been provided - if so, extract this as a directory to copy the latest
			// subset of edited YAML files to
			String subsetCopyDirectory = null;

			if (args.length > 1)
			{
				subsetCopyDirectory = args[1];
			}

			// create editor service
			OpenApiYamlEditorService editorService = new OpenApiYamlEditorService(yamlDirectory,
			                                                                      subsetCopyDirectory);

			// run editor service
			editorService.run();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			throw ex;
		}
	}
}
