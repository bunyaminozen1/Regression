package opc.services.secure;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import opc.enums.opc.UrlType;
import opc.models.secure.AnonTokenizeModel;
import opc.models.secure.DetokenizeModel;
import opc.models.secure.DeviceIdModel;
import opc.models.secure.EnrolBiometricModel;
import opc.models.secure.LoginBiometricModel;
import opc.models.secure.LoginWithPasswordModel;
import opc.models.secure.SetIdentityDetailsModel;
import opc.models.secure.TokenizeModel;
import commons.services.BaseService;
import opc.models.secure.VerificationBiometricModel;

import java.util.Optional;

public class SecureService extends BaseService {

    public static Response getIdentityDetails(final String sharedKey,
                                              final String token,
                                              final String referenceId) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", token))
                .header("origin", getBaseUrl(UrlType.SECURE_SERVICE))
                .pathParam("referenceId", referenceId)
                .when()
                .post("/app/secure/api/session/kyi/params/{referenceId}/get");
    }

    public static Response getIdentityDetailsWithBody(final Object object,
                                                      final String sharedKey,
                                                      final String token,
                                                      final String referenceId) {
        return restAssured()
                .body(object)
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", token))
                .header("origin", getBaseUrl(UrlType.SECURE_SERVICE))
                .pathParam("referenceId", referenceId)
                .when()
                .post("/app/secure/api/session/kyi/params/{referenceId}/get");
    }

    public static Response setIdentityDetails(final String sharedKey,
                                              final String token,
                                              final String referenceId,
                                              final SetIdentityDetailsModel setIdentityDetailsModel) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", token))
                .header("origin", getBaseUrl(UrlType.SECURE_SERVICE))
                .pathParam("referenceId", referenceId)
                .body(setIdentityDetailsModel)
                .when()
                .post("/app/secure/api/session/kyi/params/{referenceId}/set");
    }

    public static Response getIdentityState(final String sharedKey,
                                            final String token,
                                            final String referenceId) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", token))
                .header("origin", getBaseUrl(UrlType.SECURE_SERVICE))
                .pathParam("referenceId", referenceId)
                .when()
                .post("/app/secure/api/session/kyi/params/{referenceId}/state");
    }

    public static Response anonTokenize(final String sharedKey,
                                        final AnonTokenizeModel anonTokenizeModel,
                                        final Optional<String> origin) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("origin", origin.orElse(getBaseUrl(UrlType.SECURE_SERVICE)))
                .body(anonTokenizeModel)
                .when()
                .post("/app/secure/api/session/anon_tokenize");
    }

    public static Response tokenize(final String sharedKey,
                                    final String token,
                                    final TokenizeModel tokenizeModel) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", token))
                .header("origin", getBaseUrl(UrlType.SECURE_SERVICE))
                .body(tokenizeModel)
                .when()
                .post("/app/secure/api/session/tokenize");
    }

    public static Response detokenize(final String sharedKey,
                                      final String token,
                                      final DetokenizeModel detokenizeModel) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", token))
                .header("origin", getBaseUrl(UrlType.SECURE_SERVICE))
                .body(detokenizeModel)
                .when()
                .post("/app/secure/api/session/detokenize");
    }

    public static Response associate(final String sharedKey,
                                     final String token,
                                     final Optional<String> origin) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", token))
                .header("origin", origin.orElse(getBaseUrl(UrlType.SECURE_SERVICE)))
                .when()
                .post("/app/secure/api/session/associate");
    }

    public static Response enrolDeviceBiometric(final String token,
                                                final String sharedKey,
                                                final EnrolBiometricModel enrolBiometricModel) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", token))
                .header("origin", getBaseUrl(UrlType.SECURE_SERVICE))
                .body(enrolBiometricModel)
                .when()
                .post("/app/secure/api/session/authentication_factors/biometric/setup");
    }

    public static Response loginViaBiometric(final String sharedKey,
                                             final LoginBiometricModel loginBiometricModel) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("origin", getBaseUrl(UrlType.SECURE_SERVICE))
                .body(loginBiometricModel)
                .when()
                .post("/app/secure/api/session/authentication_factors/biometric/issue");
    }


    public static Response getBiometricBranding(final String token,
                                                final String sharedKey,
                                                final EnrolBiometricModel provideRandomModel) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", token))
                .header("origin", getBaseUrl(UrlType.SECURE_SERVICE))
                .body(provideRandomModel)
                .when()
                .post("/app/secure/api/session/branding/okay/get");
    }

    public static Response getBiometricChallengesStatus(final Optional<String> authenticationToken,
                                                        final String sharedKey,
                                                        final String sessionId) {

        final RequestSpecification requestSpecification = restAssured();

        authenticationToken.ifPresent(token ->
                requestSpecification.header("Authorization", String.format("Bearer %s", token)));

        return requestSpecification
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("origin", getBaseUrl(UrlType.SECURE_SERVICE))
                .pathParam("sessionId", sessionId)
                .when()
                .post("/app/secure/api/session/status/okay/{sessionId}/get");
    }

    public static Response loginWithPassword(final String sharedKey,
                                             final LoginWithPasswordModel loginWithPasswordModel) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("origin", getBaseUrl(UrlType.SECURE_SERVICE))
                .body(loginWithPasswordModel)
                .when()
                .post("/app/secure/api/session/login_with_password");
    }

    public static Response getBiometricConfiguration(final String sharedKey,
                                                     final DeviceIdModel deviceId) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("origin", getBaseUrl(UrlType.SECURE_SERVICE))
                .body(deviceId)
                .when()
                .post("/app/secure/api/session/biometric/config/get");
    }

    public static Response getAuthenticationType(final String sharedKey) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("origin", getBaseUrl(UrlType.SECURE_SERVICE))
                .when()
                .post("/app/secure/api/session/authentication/info");
    }

    public static Response enrolDeviceBiometricWithOtp(final String token,
                                                       final String sharedKey,
                                                       final EnrolBiometricModel enrolBiometricModel) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", token))
                .header("origin", getBaseUrl(UrlType.SECURE_SERVICE))
                .body(enrolBiometricModel)
                .when()
                .post("/app/secure/api/session/biometric/enrol/start");
    }

    public static Response verifyBiometricEnrolmentWithOtp(final String token,
                                                           final String sharedKey,
                                                           final VerificationBiometricModel verificationBiometricModel) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", token))
                .header("origin", getBaseUrl(UrlType.SECURE_SERVICE))
                .body(verificationBiometricModel)
                .when()
                .post("/app/secure/api/session/biometric/enrol/verify");
    }


}
