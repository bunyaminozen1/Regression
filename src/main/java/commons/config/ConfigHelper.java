package commons.config;

import java.util.ResourceBundle;

public class ConfigHelper {

    public static Configuration getEnvironmentConfiguration() {

        if (System.getProperty("execution.location") != null && System.getProperty("execution.location").equals("Sandbox")) {
            return new SandboxEnvironment("https://sandbox.weavr.io");
        } else {
            final ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
            final String baseUrl = resourceBundle.getString("baseUrl");

            if (baseUrl.contains("qa")) {
                return new QaEnvironment(baseUrl);
            } else if (baseUrl.contains("sandbox")) {
                return new SandboxEnvironment(baseUrl);
            } else if (baseUrl.contains("8080")) {
                return new DevEnvironment(baseUrl.split(":8")[0]);
            } else {
                return new PaymentRunDevEnvironment(baseUrl.split(":5")[0]);
            }
        }
    }
}
