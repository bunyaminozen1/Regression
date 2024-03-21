package commons.services;

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

public abstract class BaseService {

    protected static RequestSpecification getRequest() {
        return restAssured()
                .header("Content-type", "application/json");
    }

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

    protected static RequestSpecification getBodyApiKeyRequest(final Object body,
                                                               final String secretKey,
                                                               final Optional<String> idempotencyRef) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("api-key", secretKey)
                .header("idempotency-ref", idempotencyRef.orElse(""))
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

    protected static RequestSpecification getAuthorisationKeyRequest(final String authorisationKey) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("Authorization", authorisationKey);
    }

    protected static RequestSpecification getConsentAuthorisationKeyRequest(final String consentId,
                                                                            final String authorisationKey) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("Authorization", authorisationKey)
                .header("consentId", consentId);
    }

    protected static RequestSpecification getBodyAuthorisationKeyRequest(final Object body,
                                                                         final String authorisationKey) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("Authorization", authorisationKey)
                .and()
                .body(body);
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

    protected static RequestSpecification getFpiKeyRequest(final String fpiKey) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("fpi-key", fpiKey);
    }

    protected static RequestSpecification getBodyFpiKeyRequest(final Object body,
                                                               final String fpiKey) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("fpi-key", fpiKey)
                .and()
                .body(body);
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
        return restAssured(UrlType.BASE);
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

    protected static RequestSpecification assignHeaderParams(final RequestSpecification request,
                                                             final Optional<Map<String, Object>> headers) {

        headers.ifPresent(request::headers);

        return request;
    }

    protected static String getBaseUrl(final UrlType urlType) {
        final Configuration configuration = ConfigHelper.getEnvironmentConfiguration();
        return configuration.getBaseUrl(urlType);
    }
}
