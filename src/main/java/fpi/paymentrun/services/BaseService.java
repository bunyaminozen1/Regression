package fpi.paymentrun.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import commons.config.ConfigHelper;
import commons.config.Configuration;
import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
import opc.enums.opc.UrlType;

import java.util.Map;
import java.util.Optional;

import static io.restassured.RestAssured.given;

public class BaseService {

    protected static RequestSpecification getBodyRequest(final Object body) {
        return restAssured()
                .header("Content-type", "application/json")
                .and()
                .body(body);
    }

    protected static RequestSpecification getBodyAuthenticatedRequest(final Object body, final String token) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("Authorization", String.format("Bearer %s", token))
                .and()
                .body(body);
    }

    protected static RequestSpecification getBodyApiKeyRequest(final Object body, final String secretKey) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("api-key", secretKey)
                .and()
                .body(body);
    }

    protected static RequestSpecification getAuthenticatedRequest(final String token) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("Authorization", String.format("Bearer %s", token));
    }

    protected static RequestSpecification getApiKeyRequest(final String secretKey) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("api-key", secretKey);
    }

    protected static RequestSpecification getApiKeyAuthenticationRequest(final String secretKey,
                                                                         final String token) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("api-key", secretKey)
                .header("Authorization", String.format("Bearer %s", token));
    }

    protected static RequestSpecification getApiKeyAuthenticationRequest(final String secretKey,
                                                                         final String token,
                                                                         final Optional<String> idempotencyRef) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("api-key", secretKey)
                .header("Authorization", String.format("Bearer %s", token))
                .header("idempotency-ref", idempotencyRef.orElse(""));
    }

    protected static RequestSpecification getBodyApiKeyAuthenticationRequest(final Object body,
                                                                             final String secretKey,
                                                                             final String token) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("api-key", secretKey)
                .header("Authorization", String.format("Bearer %s", token))
                .and()
                .body(body);
    }

    protected static RequestSpecification getBodyApiKeyAuthenticationRequest(final Object body,
                                                                             final String secretKey,
                                                                             final String token,
                                                                             final Optional<String> idempotencyRef) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("api-key", secretKey)
                .header("Authorization", String.format("Bearer %s", token))
                .header("idempotency-ref", idempotencyRef.orElse(""))
                .and()
                .body(body);
    }

    protected static RequestSpecification getBodyAuthorisationKeyRequest(final Object body,
                                                                         final String authorisationKey) {
        return restAssured(UrlType.OPEN_BANKING)
                .header("Content-type", "application/json")
                .header("Authorization", authorisationKey)
                .and()
                .body(body);
    }

    protected static RequestSpecification getAuthorisationKeyRequest(final String authorisationKey) {
        return restAssured(UrlType.OPEN_BANKING)
                .header("Content-type", "application/json")
                .header("Authorization", authorisationKey);
    }

    protected static RequestSpecification getIgnoreNullBodyAuthenticatedRequest(final Object body, final String token) throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return restAssured()
                .header("Content-type", "application/json")
                .header("Authorization", String.format("Bearer %s", token))
                .and()
                .body(objectMapper.writeValueAsString(body));
    }

    protected static RequestSpecification getProgrammeKeyAuthenticationRequest(final String sharedKey,
                                                                               final String token) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", token));
    }

    protected static RequestSpecification getBodyProgrammeKeyAuthenticationRequest(final Object body,
                                                                                   final String sharedKey,
                                                                                   final String token) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", token))
                .and()
                .body(body);
    }

    protected static RequestSpecification restAssured() {
        return restAssured(UrlType.PAYMENT_RUN);
    }

    protected static RequestSpecification restAssured(final UrlType urlType) {
        RestAssured.baseURI = getBaseUrl(urlType);
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.urlEncodingEnabled = false;

        return given()
                .config(RestAssuredConfig.config()
                        .encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
                .filters(new ResponseLoggingFilter(500), new ResponseLoggingFilter(503), new ResponseLoggingFilter(504));
    }

    protected static RequestSpecification assignQueryParams(final RequestSpecification request,
                                                            final Optional<Map<String, Object>> filters) {

        filters.ifPresent(request::queryParams);

        return request;
    }

    protected static String getBaseUrl(final UrlType urlType) {
        final Configuration configuration = ConfigHelper.getEnvironmentConfiguration();
        return configuration.getBaseUrl(urlType);
    }

    protected static String getPaymentRunEnvironmentPrefix() {
        return ConfigHelper.getEnvironmentConfiguration().getTestRunEnvironment()
                .equals("qa") ?
                "/payment-run" : "";
    }

    protected static String getOpenBankingEnvironmentPrefix() {
        return ConfigHelper.getEnvironmentConfiguration().getTestRunEnvironment()
                .equals("qa") ?
                "/open-banking" : "";
    }

    protected static String getSweepingJobApiKey() {
        return ConfigHelper.getEnvironmentConfiguration().getSweepingJobApiKey();
    }

    protected static String getOpenBankingApiKey() {
        return ConfigHelper.getEnvironmentConfiguration().getOpenBankingAuthKey();
    }
}