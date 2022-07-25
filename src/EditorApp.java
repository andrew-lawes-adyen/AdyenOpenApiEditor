import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Properties;

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
            // load config variables from config.properties file
            Properties props = new Properties();

            try (BufferedInputStream in = new BufferedInputStream(new FileInputStream("src/config.properties")))
            {
                props.load(in);
            }
            catch (Exception ex)
            {
                throw ex;
            }

            String pathToInputDirectory = props.getProperty("pathToInputDirectory");
            String pathToOutputDirectory = props.getProperty("pathToOutputDirectory");
            boolean copyLatestVersions = Boolean.valueOf(props.getProperty("copyLatestVersions"));

            // create editor service
            OpenApiYamlEditorService editorService = new OpenApiYamlEditorService(pathToInputDirectory,
                    pathToOutputDirectory, copyLatestVersions);

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
