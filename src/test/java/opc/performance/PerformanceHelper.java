package opc.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import commons.config.ConfigHelper;
import commons.config.Configuration;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import opc.enums.opc.UrlType;
import opc.models.shared.ProgrammeDetailsModel;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import static io.gatling.javaapi.http.HttpDsl.http;

public class PerformanceHelper {

    private static ProgrammeDetailsModel testApplication;
    private static ProgrammeDetailsModel pluginTestApplication;

    public PerformanceHelper() {

        try {
            final Configuration configuration = ConfigHelper.getEnvironmentConfiguration();

            testApplication =
                    Arrays.stream(new ObjectMapper()
                            .readValue(new File(String.format("./src/test/resources/TestConfiguration/%s_multi_configuration.json",
                                            configuration.getMainTestEnvironment())),
                                    ProgrammeDetailsModel[].class)).filter(x -> x.getProgrammeName().equals("ApplicationOne"))
                            .collect(Collectors.toList()).get(0);

            pluginTestApplication =
                    Arrays.stream(new ObjectMapper()
                            .readValue(new File("./src/test/resources/TestConfiguration/qa_plugins_configuration.json"),
                                    ProgrammeDetailsModel[].class))
                            .filter(x -> x.getProgrammeName().equals("PluginsApp"))
                            .filter(x -> x.getFpiKey().equals(configuration.getFpiKey()))
                            .collect(Collectors.toList()).get(0);
        } catch (IOException e) {
            throw new RuntimeException("Issue with retrieving test data.");
        }
    }

    public ProgrammeDetailsModel getTestApplication() {
        return testApplication;
    }

    public ProgrammeDetailsModel getPluginsTestApplication() {
        return pluginTestApplication;
    }

    public HttpProtocolBuilder apiKeyAuthenticatedProtocol(final String authenticationToken) {
        return apiKeyAuthenticatedProtocol(authenticationToken, testApplication.getSecretKey());
    }

    public HttpProtocolBuilder apiKeyAuthenticatedProtocol(final String authenticationToken,
                                                            final String secretKey) {
        return http.baseUrl(getBaseUrl(UrlType.BASE))
                .header("Content-Type", "application/json")
                .header("api-key", secretKey)
                .header("Authorization", String.format("Bearer %s", authenticationToken));
    }

    public HttpProtocolBuilder apiKeyProtocol() {
        return apiKeyProtocol(testApplication.getSecretKey());
    }

    public HttpProtocolBuilder apiKeyProtocol(final String secretKey) {
        return http.baseUrl(getBaseUrl(UrlType.BASE))
                .header("Content-Type", "application/json")
                .header("api-key", secretKey);
    }

    public HttpProtocolBuilder apiKeyProtocolSimulator() {
        return http.baseUrl(getBaseUrl(UrlType.SIMULATOR))
                .header("Content-Type", "application/json")
                .header("programme-key", testApplication.getSecretKey());
    }

    private static String getBaseUrl(final UrlType urlType) {
        final Configuration configuration = ConfigHelper.getEnvironmentConfiguration();
        return configuration.getBaseUrl(urlType);
    }
}
