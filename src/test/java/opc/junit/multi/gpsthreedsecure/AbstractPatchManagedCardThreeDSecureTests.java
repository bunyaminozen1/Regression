package opc.junit.multi.gpsthreedsecure;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.IdentityType;
import opc.enums.opc.LimitInterval;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.PatchManagedCardModel;
import opc.models.multi.managedcards.ThreeDSecureAuthConfigModel;
import opc.models.multi.users.UsersModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.admin.AdminService;
import opc.services.multi.ManagedCardsService;
import opc.services.multi.UsersService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Tag(MultiTags.GPS_THREED_SECURE)
public abstract class AbstractPatchManagedCardThreeDSecureTests extends BaseGpsThreeDSecureSetup {

    protected abstract String getThreeDSIdentityToken();

    protected abstract String getThreeDSIdentityCurrency();

    protected abstract String getIdentityToken();

    protected abstract String getIdentityCurrency();

    protected abstract String getIdentityPrepaidManagedCardProfileId();

    protected abstract String getIdentityDebitManagedCardProfileId();

    protected abstract String getIdentityManagedAccountProfileId();

    protected abstract IdentityType getIdentityType();

    @BeforeAll
    public static void setup() {

        final String adminToken = AdminService.loginAdmin();
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 100000);

        AdminHelper.setProgrammeAuthyChallengeLimit(threeDSApp.getProgrammeId(), resetCount,
            adminToken);
    }

    /**
     * Test cases for 3ds with BIOMETRICS
     * Ticket: https://weavr-payments.atlassian.net/browse/DEV-3575
     *
     * BIOMETRICS should be enabled on each level:
     * - programme
     * - identity profile
     *
     * The main case:
     * 1. create enrolled for BIOMETRICS consumer/corporate
     * 2. create enrolled for BIOMETRICS User with mobile number linked to the identity
     * 3. enrol User for BIOMETRICS
     * 4. Call API with threeDSecureAuthConfig in payload
     */

    @ParameterizedTest
    @ValueSource(strings = {"DEBIT_MODE", "PREPAID_MODE"})
    public void ThreeDSPatchMC_SetPrimaryOtpSms_Success(
        String managedCardMode) {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        final ManagedCardDetails managedCard = createManagedCardBiometric(managedCardMode, user,
            userMobile);

//    Update MC
        final ValidatableResponse responseUpdate = ManagedCardsService.patchManagedCard(
                PatchManagedCardModel.builder().setThreeDSecureAuthConfig(
                    ThreeDSecureAuthConfigModel.builder()
                        .setLinkedUserId(user.getLeft())
                        .setPrimaryChannel("OTP_SMS")
                        .build()).build(),
                secretKey, managedCard.getManagedCardId(), getThreeDSIdentityToken())
            .then()
            .statusCode(SC_OK)
            .body("cardholderMobileNumber", equalTo(userMobile))
            .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
            .body("threeDSecureAuthConfig.primaryChannel", equalTo("OTP_SMS"))
            .body("threeDSecureAuthConfig.fallbackChannel", nullValue());

//    Verify changes
        final Response responseUpdatedManagedCard = ManagedCardsHelper.getManagedCard(secretKey,
            managedCard.getManagedCardId(), getThreeDSIdentityToken());

        assertEquals(
            responseUpdatedManagedCard.jsonPath().getString("threeDSecureAuthConfig.primaryChannel"),
            responseUpdate.extract().jsonPath().getString("threeDSecureAuthConfig.primaryChannel"));

        assertEquals(
            responseUpdatedManagedCard.jsonPath().getString("threeDSecureAuthConfig.fallbackChannel"),
            responseUpdate.extract().jsonPath().getString("threeDSecureAuthConfig.fallbackChannel"));

        assertNotEquals(managedCard.getThreeDSecureAuthConfig().getPrimaryChannel(),
            responseUpdatedManagedCard.jsonPath().getString("threeDSecureAuthConfig.primaryChannel"));
        assertNotEquals(managedCard.getThreeDSecureAuthConfig().getFallbackChannel(),
            responseUpdatedManagedCard.jsonPath().getString("threeDSecureAuthConfig.fallbackChannel"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEBIT_MODE", "PREPAID_MODE"})
    public void ThreeDSPatchMC_SetPrimaryBiometrics_Success(String managedCardMode) {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        final ManagedCardDetails managedCard = createManagedCardOtpSms(managedCardMode, user,
            userMobile);

//    Update MC
        final ValidatableResponse responseUpdate = ManagedCardsService.patchManagedCard(
                PatchManagedCardModel.builder().setThreeDSecureAuthConfig(
                    ThreeDSecureAuthConfigModel.builder()
                        .setLinkedUserId(user.getLeft())
                        .setPrimaryChannel("BIOMETRICS")
                        .build()).build(),
                secretKey, managedCard.getManagedCardId(), getThreeDSIdentityToken())
            .then()
            .statusCode(SC_OK)
            .body("cardholderMobileNumber", equalTo(userMobile))
            .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
            .body("threeDSecureAuthConfig.primaryChannel", equalTo("BIOMETRICS"))
            .body("threeDSecureAuthConfig.fallbackChannel", equalTo("OTP_SMS"));

//    Verify changes
        final Response responseUpdatedManagedCard = ManagedCardsHelper.getManagedCard(secretKey,
            managedCard.getManagedCardId(), getThreeDSIdentityToken());

        assertEquals(
            responseUpdatedManagedCard.jsonPath().getString("threeDSecureAuthConfig.primaryChannel"),
            responseUpdate.extract().jsonPath().getString("threeDSecureAuthConfig.primaryChannel"));

        assertEquals(
            responseUpdatedManagedCard.jsonPath().getString("threeDSecureAuthConfig.fallbackChannel"),
            responseUpdate.extract().jsonPath().getString("threeDSecureAuthConfig.fallbackChannel"));

        assertNotEquals(managedCard.getThreeDSecureAuthConfig().getPrimaryChannel(),
            responseUpdatedManagedCard.jsonPath().getString("threeDSecureAuthConfig.primaryChannel"));
        assertNotEquals(managedCard.getThreeDSecureAuthConfig().getFallbackChannel(),
            responseUpdatedManagedCard.jsonPath().getString("threeDSecureAuthConfig.fallbackChannel"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEBIT_MODE", "PREPAID_MODE"})
    public void ThreeDSPatchMC_NewMobileNumberWithoutNewUser_Conflict(
        String managedCardMode) {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC
        final ManagedCardDetails managedCard = createManagedCardBiometric(managedCardMode, user,
            userMobile);

//    Update MC
        PatchManagedCardModel patchModel = PatchManagedCardModel.builder()
            .setCardholderMobileNumber(String.format("+356%s", RandomStringUtils.randomNumeric(8)))
            .build();

        ManagedCardsService.patchManagedCard(patchModel, secretKey,
                managedCard.getManagedCardId(), getThreeDSIdentityToken())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("MOBILE_NUMBER_ALREADY_EXISTS"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEBIT_MODE", "PREPAID_MODE"})
    public void ThreeDSPatchMC_SetNewMobileNumberNewUser_Success(
        String managedCardMode) {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC
        final ManagedCardDetails managedCard = createManagedCardBiometric(managedCardMode, user,
            userMobile);

//    create enrolled secondUser linked to Identity
        final UsersModel secondUsersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> secondUser = UsersHelper.createEnrolledUser(secondUsersModel,
            secretKey,
            getThreeDSIdentityToken());

        final String secondUserMobile = String.format("+356%s",
            secondUsersModel.getMobile().getNumber());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(secondUser.getLeft(), sharedKey, secretKey, secondUser.getRight());

//    Update MC
        PatchManagedCardModel patchModel = PatchManagedCardModel.builder()
            .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                .setLinkedUserId(secondUser.getLeft())
                .setPrimaryChannel("BIOMETRICS")
                .build())
            .setCardholderMobileNumber(secondUserMobile)
            .build();

        ManagedCardsService.patchManagedCard(patchModel, secretKey,
                managedCard.getManagedCardId(), getThreeDSIdentityToken())
            .then()
            .statusCode(SC_OK)
            .body("cardholderMobileNumber", equalTo(secondUserMobile))
            .body("threeDSecureAuthConfig.linkedUserId", equalTo(secondUser.getLeft()))
            .body("threeDSecureAuthConfig.primaryChannel", equalTo("BIOMETRICS"))
            .body("threeDSecureAuthConfig.fallbackChannel", equalTo("OTP_SMS"));

//    Verify changes
        final Response responseUpdatedManagedCard = ManagedCardsHelper.getManagedCard(secretKey,
            managedCard.getManagedCardId(), getThreeDSIdentityToken());

        assertEquals(responseUpdatedManagedCard.jsonPath().getString("cardholderMobileNumber"),
            secondUserMobile);

        assertEquals(
            responseUpdatedManagedCard.jsonPath().getString("threeDSecureAuthConfig.linkedUserId"),
            secondUser.getLeft());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEBIT_MODE", "PREPAID_MODE"})
    public void ThreeDSPatchMC_NewUser_Success(String managedCardMode) {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC
        final ManagedCardDetails managedCard = createManagedCardBiometric(managedCardMode, user,
            userMobile);

//    create enrolled secondUser linked to Identity
        final UsersModel secondUsersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> secondUser = UsersHelper.createEnrolledUser(secondUsersModel,
            secretKey,
            getThreeDSIdentityToken());

        final String secondUserMobile = String.format("+356%s",
            secondUsersModel.getMobile().getNumber());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(secondUser.getLeft(), sharedKey, secretKey, secondUser.getRight());

//    Update MC
        PatchManagedCardModel patchModel = PatchManagedCardModel.builder()
            .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                .setLinkedUserId(secondUser.getLeft())
                .setPrimaryChannel("BIOMETRICS")
                .build())
            .build();

        ManagedCardsService.patchManagedCard(patchModel, secretKey,
                managedCard.getManagedCardId(), getThreeDSIdentityToken())
            .then()
            .statusCode(SC_OK)
            .body("cardholderMobileNumber", equalTo(secondUserMobile))
            .body("threeDSecureAuthConfig.linkedUserId", equalTo(secondUser.getLeft()))
            .body("threeDSecureAuthConfig.primaryChannel", equalTo("BIOMETRICS"))
            .body("threeDSecureAuthConfig.fallbackChannel", equalTo("OTP_SMS"));

//    Verify changes
        final Response responseUpdatedManagedCard = ManagedCardsHelper.getManagedCard(secretKey,
            managedCard.getManagedCardId(), getThreeDSIdentityToken());

        assertEquals(responseUpdatedManagedCard.jsonPath().getString("cardholderMobileNumber"),
            secondUserMobile);

        assertEquals(
            responseUpdatedManagedCard.jsonPath().getString("threeDSecureAuthConfig.linkedUserId"),
            secondUser.getLeft());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEBIT_MODE", "PREPAID_MODE"})
    public void ThreeDSPatchMC_TwoMobileNumbers_Conflict(String managedCardMode) {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        final ManagedCardDetails managedCard = createManagedCardOtpSms(managedCardMode, user,
            userMobile);

//    Update MC
        ManagedCardsService.patchManagedCard(
                PatchManagedCardModel.builder()
                    .setCardholderMobileNumber(String.format("+356%s", RandomStringUtils.randomNumeric(8)))
                    .setThreeDSecureAuthConfig(
                        ThreeDSecureAuthConfigModel.builder()
                            .setLinkedUserId(user.getLeft())
                            .setPrimaryChannel("BIOMETRICS")
                            .build()).build(),
                secretKey, managedCard.getManagedCardId(), getThreeDSIdentityToken())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("MORE_THAN_ONE_MOBILE_NUMBER_PROVIDED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEBIT_MODE", "PREPAID_MODE"})
    public void ThreeDSPatchMC_NotEnrolledForBiometrics_Conflict(String managedCardMode) {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

//    Create MC with linkedUserId = userId
        final String managedCard = createDefaultManagedCard(managedCardMode, getThreeDSIdentityToken());

//    Update MC
        ManagedCardsService.patchManagedCard(
                PatchManagedCardModel.builder().setThreeDSecureAuthConfig(
                    ThreeDSecureAuthConfigModel.builder()
                        .setLinkedUserId(user.getLeft())
                        .setPrimaryChannel("BIOMETRICS")
                        .build()).build(),
                secretKey, managedCard, getThreeDSIdentityToken())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("USER_NOT_ENROLLED_FOR_BIOMETRICS"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEBIT_MODE", "PREPAID_MODE"})
    public void ThreeDSPatchMC_WrongUserId_NotFound(String managedCardMode) {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        final ManagedCardDetails managedCard = createManagedCardOtpSms(managedCardMode, user,
            userMobile);

//    Update MC
        String userId = RandomStringUtils.randomNumeric(16);

        ManagedCardsService.patchManagedCard(
                PatchManagedCardModel.DefaultThreeDSecurePatchManagedCardModelPrimaryOTP(
                    userId).build(), secretKey, managedCard.getManagedCardId(),
                getThreeDSIdentityToken())
            .then()
            .statusCode(SC_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEBIT_MODE", "PREPAID_MODE"})
    public void ThreeDSPatchMC_BiometricNotSupported_Conflict(String managedCardMode) {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getIdentityToken());

//    Create MC with linkedUserId = userId
        final String managedCard = createDefaultManagedCard(managedCardMode, getIdentityToken());

//    Update MC
        ManagedCardsService.patchManagedCard(
                PatchManagedCardModel.builder().setThreeDSecureAuthConfig(
                    ThreeDSecureAuthConfigModel.builder()
                        .setLinkedUserId(user.getLeft())
                        .setPrimaryChannel("BIOMETRICS")
                        .build()).build(),
                secretKey, managedCard, getIdentityToken())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("BIOMETRIC_AUTHENTICATION_NOT_SUPPORTED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEBIT_MODE", "PREPAID_MODE"})
    public void ThreeDSPatchMC_UserInactive_Conflict(String managedCardMode) {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        final ManagedCardDetails managedCard = createManagedCardOtpSms(managedCardMode, user,
            userMobile);

        UsersService.deactivateUser(secretKey, user.getLeft(), getThreeDSIdentityToken())
            .then()
            .statusCode(SC_NO_CONTENT);

//    Update MC
        ManagedCardsService.patchManagedCard(
                PatchManagedCardModel.builder().setThreeDSecureAuthConfig(
                    ThreeDSecureAuthConfigModel.builder()
                        .setLinkedUserId(user.getLeft())
                        .setPrimaryChannel("BIOMETRICS")
                        .build()).build(),
                secretKey, managedCard.getManagedCardId(), getThreeDSIdentityToken())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("USER_INACTIVE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEBIT_MODE", "PREPAID_MODE"})
    public void ThreeDSPatchMC_InvalidChannel_Conflict(String managedCardMode) {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        final ManagedCardDetails managedCard = createManagedCardOtpSms(managedCardMode, user,
            userMobile);

//    Create MC with linkedUserId = userId
        ManagedCardsService.patchManagedCard(
                PatchManagedCardModel.builder().setThreeDSecureAuthConfig(
                        ThreeDSecureAuthConfigModel.builder()
                            .setLinkedUserId(user.getLeft())
                            .setPrimaryChannel("OTP_SMS")
                            .setFallbackChannel("OTP_SMS")
                            .build())
                    .build(),
                secretKey, managedCard.getManagedCardId(), getThreeDSIdentityToken())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("INVALID_CHANNELS_SELECTED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEBIT_MODE", "PREPAID_MODE"})
    public void ThreeDSPatchMC_InvalidPrimaryChannel_BadRequest(String managedCardMode) {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        final ManagedCardDetails managedCard = createManagedCardOtpSms(managedCardMode, user,
            userMobile);

//    Create MC with linkedUserId = userId
        ManagedCardsService.patchManagedCard(
                PatchManagedCardModel.builder().setThreeDSecureAuthConfig(
                        ThreeDSecureAuthConfigModel.builder()
                            .setLinkedUserId(user.getLeft())
                            .setPrimaryChannel("BIOMETRIC")
                            .setFallbackChannel("OTP_SMS")
                            .build())
                    .build(),
                secretKey, managedCard.getManagedCardId(), getThreeDSIdentityToken())
            .then()
            .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEBIT_MODE", "PREPAID_MODE"})
    public void ThreeDSPatchMC_InvalidFallbackChannel_BadRequest(String managedCardMode) {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        final ManagedCardDetails managedCard = createManagedCardOtpSms(managedCardMode, user,
            userMobile);

//    Create MC with linkedUserId = userId
        ManagedCardsService.patchManagedCard(
                PatchManagedCardModel.builder().setThreeDSecureAuthConfig(
                        ThreeDSecureAuthConfigModel.builder()
                            .setLinkedUserId(user.getLeft())
                            .setPrimaryChannel("BIOMETRICS")
                            .setFallbackChannel("BIOMETRICS")
                            .build())
                    .build(),
                secretKey, managedCard.getManagedCardId(), getThreeDSIdentityToken())
            .then()
            .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEBIT_MODE", "PREPAID_MODE"})
    public void ThreeDSPatchMC_FallbackChannelNotSelected_Success(
        String managedCardMode) {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        final ManagedCardDetails managedCard = createManagedCardBiometric(managedCardMode, user,
            userMobile);

//    Create MC with linkedUserId = userId
        ManagedCardsService.patchManagedCard(
                PatchManagedCardModel.builder().setThreeDSecureAuthConfig(
                        ThreeDSecureAuthConfigModel.builder()
                            .setLinkedUserId(user.getLeft())
                            .setPrimaryChannel("BIOMETRICS")
                            .build())
                    .build(),
                secretKey, managedCard.getManagedCardId(), getThreeDSIdentityToken())
            .then()
            .statusCode(SC_OK)
            .body("cardholderMobileNumber", equalTo(userMobile))
            .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
            .body("threeDSecureAuthConfig.primaryChannel", equalTo("BIOMETRICS"))
            .body("threeDSecureAuthConfig.fallbackChannel", equalTo("OTP_SMS"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEBIT_MODE", "PREPAID_MODE"})
    public void ThreeDSPatchMC_MobileNumberNotVerified_Success(String managedCardMode) {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey,
            getThreeDSIdentityToken());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        final String managedCard = createDefaultManagedCard(managedCardMode, getThreeDSIdentityToken());

//    Update MC
        ManagedCardsService.patchManagedCard(
                PatchManagedCardModel.builder().setThreeDSecureAuthConfig(
                    ThreeDSecureAuthConfigModel.builder()
                        .setLinkedUserId(user.getLeft())
                        .setPrimaryChannel("OTP_SMS")
                        .build()).build(),
                secretKey, managedCard, getThreeDSIdentityToken())
            .then()
            .statusCode(SC_OK)
            .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
            .body("threeDSecureAuthConfig.primaryChannel", equalTo("OTP_SMS"))
            .body("threeDSecureAuthConfig.fallbackChannel", nullValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEBIT_MODE", "PREPAID_MODE"})
    public void ThreeDSPatchMC_NoMobileNumber_Conflict(String managedCardMode) {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.builder()
            .setName(RandomStringUtils.randomAlphabetic(6))
            .setSurname(RandomStringUtils.randomAlphabetic(6))
            .setEmail(String.format("%s@weavrusertest.io", RandomStringUtils.randomAlphabetic(10)))
            .build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey,
            getThreeDSIdentityToken());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        final String managedCard = createDefaultManagedCard(managedCardMode, getThreeDSIdentityToken());

//    Update MC
        ManagedCardsService.patchManagedCard(PatchManagedCardModel.builder().setThreeDSecureAuthConfig(
                    ThreeDSecureAuthConfigModel.builder()
                        .setLinkedUserId(user.getLeft())
                        .setPrimaryChannel("BIOMETRICS")
                        .setFallbackChannel("OTP_SMS")
                        .build()).build(),
                secretKey, managedCard, getThreeDSIdentityToken())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("USER_MOBILE_NUMBER_DOES_NOT_EXIST"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEBIT_MODE", "PREPAID_MODE"})
    public void ThreeDSPatchMC_CrossIdentity_NotFound(
        String managedCardMode) {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//        Create third party identity
        final String secondIdentityToken;
        if (getIdentityType().equals(IdentityType.CONSUMER)) {
            secondIdentityToken = ConsumersHelper.createEnrolledVerifiedConsumer(threeDSConsumerProfileId, secretKey).getRight();
        } else {
            secondIdentityToken = CorporatesHelper.createEnrolledVerifiedCorporate(threeDSCorporateProfileId, secretKey).getRight();
        }

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        final ManagedCardDetails managedCard = createManagedCardBiometric(managedCardMode, user,
            userMobile);

//    Update MC by third party identity
        ManagedCardsService.patchManagedCard(
                PatchManagedCardModel.builder().setThreeDSecureAuthConfig(
                    ThreeDSecureAuthConfigModel.builder()
                        .setLinkedUserId(user.getLeft())
                        .setPrimaryChannel("OTP_SMS")
                        .build()).build(),
                secretKey, managedCard.getManagedCardId(), secondIdentityToken)
            .then()
            .statusCode(SC_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEBIT_MODE", "PREPAID_MODE"})
    public void ThreeDSPatchMC_UpdateInvalidLinkedUserId_BadRequest(String managedCardMode) {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        final ManagedCardDetails managedCard = createManagedCardBiometric(managedCardMode, user,
            userMobile);

//    Update MC with invalid linked user id
        ManagedCardsService.patchManagedCard(
                PatchManagedCardModel.builder().setThreeDSecureAuthConfig(
                    ThreeDSecureAuthConfigModel.builder()
                        .setLinkedUserId(RandomStringUtils.randomAlphabetic(8))
                        .setPrimaryChannel("OTP_SMS")
                        .build()).build(),
                secretKey, managedCard.getManagedCardId(), getThreeDSIdentityToken())
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("message", equalTo("request.threeDSecureAuthConfig.linkedUserId: must match \"^[0-9]+$\""));
    }

    protected static ManagedCardDetails createPrepaidManagedCardPrimaryBiometrics(
        final String managedCardProfileId,
        final String userId,
        final String authenticationToken,
        final String userMobileNumber) {
        final CreateManagedCardModel createManagedCardModel =
            CreateManagedCardModel.ThreeDSecureCreatePrepaidManagedCardModel(
                managedCardProfileId, userId).build();

        final ValidatableResponse response =
            ManagedCardsService.createManagedCard(createManagedCardModel, secretKey,
                    authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("cardholderMobileNumber", equalTo(userMobileNumber))
                .body("threeDSecureAuthConfig.linkedUserId", equalTo(userId))
                .body("threeDSecureAuthConfig.primaryChannel", equalTo("BIOMETRICS"))
                .body("threeDSecureAuthConfig.fallbackChannel", equalTo("OTP_SMS"));

        return ManagedCardDetails.builder()
            .setManagedCardId(response.extract().jsonPath().getString("id"))
            .setCardholderMobileNumber(
                response.extract().jsonPath().getString("cardholderMobileNumber"))
            .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                .setLinkedUserId(
                    response.extract().jsonPath().getString("threeDSecureAuthConfig.linkedUserId"))
                .setPrimaryChannel(
                    response.extract().jsonPath().getString("threeDSecureAuthConfig.primaryChannel"))
                .setFallbackChannel(
                    response.extract().jsonPath().getString("threeDSecureAuthConfig.fallbackChannel"))
                .build())
            .build();
    }

    protected static ManagedCardDetails createDebitManagedCardPrimaryBiometrics(
        final String managedCardProfileId,
        final String managedAccountProfileId,
        final String currency,
        final String userId,
        final String authenticationToken,
        final String userMobileNumber) {

        final String accountId = ManagedAccountsHelper.createManagedAccount(managedAccountProfileId,
            currency, secretKey, authenticationToken);

        final CreateManagedCardModel createManagedCardModel =
            CreateManagedCardModel.ThreeDSecureCreateDebitManagedCardModel(
                managedCardProfileId, userId, accountId).build();

        final ValidatableResponse response =
            ManagedCardsService.createManagedCard(createManagedCardModel, secretKey,
                    authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("cardholderMobileNumber", equalTo(userMobileNumber))
                .body("threeDSecureAuthConfig.linkedUserId", equalTo(userId))
                .body("threeDSecureAuthConfig.primaryChannel", equalTo("BIOMETRICS"))
                .body("threeDSecureAuthConfig.fallbackChannel", equalTo("OTP_SMS"))
                .body("mode", equalTo("DEBIT_MODE"));

        return ManagedCardDetails.builder()
            .setManagedCardId(response.extract().jsonPath().getString("id"))
            .setCardholderMobileNumber(
                response.extract().jsonPath().getString("cardholderMobileNumber"))
            .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                .setLinkedUserId(
                    response.extract().jsonPath().getString("threeDSecureAuthConfig.linkedUserId"))
                .setPrimaryChannel(
                    response.extract().jsonPath().getString("threeDSecureAuthConfig.primaryChannel"))
                .setFallbackChannel(
                    response.extract().jsonPath().getString("threeDSecureAuthConfig.fallbackChannel"))
                .build())
            .build();
    }

    protected static ManagedCardDetails createPrepaidManagedCardPrimaryOtpSms(
        final String managedCardProfileId,
        final String userId,
        final String authenticationToken,
        final String userMobileNumber) {
        final CreateManagedCardModel createManagedCardModel =
            CreateManagedCardModel.ThreeDSecureCreatePrepaidManagedCardModelPrimaryOTP(
                managedCardProfileId, userId).build();

        final ValidatableResponse response =
            ManagedCardsService.createManagedCard(createManagedCardModel, secretKey,
                    authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("cardholderMobileNumber", equalTo(userMobileNumber))
                .body("threeDSecureAuthConfig.linkedUserId", equalTo(userId))
                .body("threeDSecureAuthConfig.primaryChannel", equalTo("OTP_SMS"))
                .body("mode", equalTo("PREPAID_MODE"));

        return ManagedCardDetails.builder()
            .setManagedCardId(response.extract().jsonPath().getString("id"))
            .setCardholderMobileNumber(
                response.extract().jsonPath().getString("cardholderMobileNumber"))
            .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                .setLinkedUserId(
                    response.extract().jsonPath().getString("threeDSecureAuthConfig.linkedUserId"))
                .setPrimaryChannel(
                    response.extract().jsonPath().getString("threeDSecureAuthConfig.primaryChannel"))
                .setFallbackChannel(
                    response.extract().jsonPath().getString("threeDSecureAuthConfig.fallbackChannel"))
                .build())
            .build();
    }

    protected static ManagedCardDetails createDebitManagedCardPrimaryOtpSms(
        final String managedCardProfileId,
        final String managedAccountProfileId,
        final String currency,
        final String userId,
        final String authenticationToken,
        final String userMobileNumber) {

        final String accountId = ManagedAccountsHelper.createManagedAccount(managedAccountProfileId,
            currency, secretKey, authenticationToken);

        final CreateManagedCardModel createManagedCardModel =
            CreateManagedCardModel.ThreeDSecureCreateDebitManagedCardModelPrimaryOTP(
                managedCardProfileId, userId, accountId).build();

        final ValidatableResponse response =
            ManagedCardsService.createManagedCard(createManagedCardModel, secretKey,
                    authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("cardholderMobileNumber", equalTo(userMobileNumber))
                .body("threeDSecureAuthConfig.linkedUserId", equalTo(userId))
                .body("threeDSecureAuthConfig.primaryChannel", equalTo("OTP_SMS"))
                .body("mode", equalTo("DEBIT_MODE"));

        return ManagedCardDetails.builder()
            .setManagedCardId(response.extract().jsonPath().getString("id"))
            .setCardholderMobileNumber(
                response.extract().jsonPath().getString("cardholderMobileNumber"))
            .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                .setLinkedUserId(
                    response.extract().jsonPath().getString("threeDSecureAuthConfig.linkedUserId"))
                .setPrimaryChannel(
                    response.extract().jsonPath().getString("threeDSecureAuthConfig.primaryChannel"))
                .setFallbackChannel(
                    response.extract().jsonPath().getString("threeDSecureAuthConfig.fallbackChannel"))
                .build())
            .build();
    }

    private ManagedCardDetails createManagedCardBiometric(String managedCardMode,
        Pair<String, String> user, String userMobile) {
        ManagedCardDetails managedCard;
        if (managedCardMode.equals("PREPAID_MODE")) {
            managedCard = createPrepaidManagedCardPrimaryBiometrics(
                getIdentityPrepaidManagedCardProfileId(),
                user.getLeft(), getThreeDSIdentityToken(), userMobile);
        } else {
            managedCard = createDebitManagedCardPrimaryBiometrics(getIdentityDebitManagedCardProfileId(),
                getIdentityManagedAccountProfileId(),
                getThreeDSIdentityCurrency(), user.getLeft(),
                getThreeDSIdentityToken(), userMobile);
        }
        return managedCard;
    }

    private ManagedCardDetails createManagedCardOtpSms(String managedCardMode,
        Pair<String, String> user, String userMobile) {
        ManagedCardDetails managedCard;
        if (managedCardMode.equals("PREPAID_MODE")) {
            managedCard = createPrepaidManagedCardPrimaryOtpSms(
                getIdentityPrepaidManagedCardProfileId(),
                user.getLeft(), getThreeDSIdentityToken(), userMobile);
        } else {
            managedCard = createDebitManagedCardPrimaryOtpSms(getIdentityDebitManagedCardProfileId(),
                getIdentityManagedAccountProfileId(),
                getThreeDSIdentityCurrency(), user.getLeft(),
                getThreeDSIdentityToken(), userMobile);
        }
        return managedCard;
    }

    private String createDefaultManagedCard(String managedCardMode,
        final String authenticationToken) {
        String managedCardId;
        if (managedCardMode.equals("PREPAID_MODE")) {
            CreateManagedCardModel prepaidCardModel = CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(
                getIdentityPrepaidManagedCardProfileId(), getIdentityCurrency()).build();

            managedCardId = ManagedCardsHelper.createManagedCard(prepaidCardModel, secretKey,
                authenticationToken);
        } else {
            final String accountId = getManagedAccountId(authenticationToken);
            CreateManagedCardModel debitCardModel = CreateManagedCardModel.DefaultCreateDebitManagedCardModel(
                getIdentityDebitManagedCardProfileId(), accountId).build();
            managedCardId = ManagedCardsHelper.createManagedCard(debitCardModel, secretKey,
                authenticationToken);
        }
        return managedCardId;
    }

    private String getManagedAccountId(String authenticationToken) {
        return ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileId(),
            getIdentityCurrency(), secretKey, authenticationToken);
    }

}
