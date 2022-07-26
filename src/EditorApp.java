import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
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

        System.out.println("AdyenOpenApiEditor starting");

        try
        {
            // load config variables from config.properties file
            Properties props = new Properties();

            // assume properties file is being loaded by the JAR
            String propertiesFile = Paths.get(EditorApp.class.getProtectionDomain().getCodeSource().getLocation()
                    .toURI()).getParent().resolve("config.properties").toString();

            // if config.properties file can't be found, fallback to checking root folder (during debugging)
            if (!Files.exists(Paths.get(propertiesFile)))
            {
                // reset properties file location
                propertiesFile = "config.properties";

                // throw exception if it still isn't found
                if (!Files.exists(Paths.get(propertiesFile)))
                {
                    throw new Exception("Could not locate properties file");
                }
            }

            try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(propertiesFile)))
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

            System.out.println("AdyenOpenApiEditor started successfully");

            // run editor service
            editorService.run();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            throw ex;
        }

        System.out.println("AdyenOpenApiEditor finished");
    }
}