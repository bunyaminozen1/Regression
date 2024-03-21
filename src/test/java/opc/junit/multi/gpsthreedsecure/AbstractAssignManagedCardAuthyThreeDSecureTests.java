package opc.junit.multi.gpsthreedsecure;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.IdentityType;
import opc.enums.opc.InstrumentType;
import opc.enums.opc.LimitInterval;
import opc.enums.opc.ManagedCardMode;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.managedcards.AssignManagedCardModel;
import opc.models.multi.managedcards.ThreeDSecureAuthConfigModel;
import opc.models.multi.users.UsersModel;
import opc.models.testmodels.UnassignedManagedCardDetails;
import opc.services.admin.AdminService;
import opc.services.multi.ManagedCardsService;
import opc.services.multi.UsersService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static opc.enums.opc.InstrumentType.PHYSICAL;
import static opc.enums.opc.InstrumentType.VIRTUAL;
import static opc.enums.opc.ManagedCardMode.DEBIT_MODE;
import static opc.enums.opc.ManagedCardMode.PREPAID_MODE;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


@Tag(MultiTags.GPS_THREED_SECURE_AUTHY)
public abstract class AbstractAssignManagedCardAuthyThreeDSecureTests extends BaseGpsThreeDSecureSetup {
    protected abstract String getBiometricIdentityToken();

    protected abstract String getAuthyIdentityToken();

    protected abstract IdentityType getIdentityType();

    protected abstract List<UnassignedManagedCardDetails> getBiometricIdentityUnassignedCards();

    protected abstract List<UnassignedManagedCardDetails> getAuthyIdentityUnassignedCards();

    @BeforeAll
    public static void setup() {
        final String adminToken = AdminService.loginAdmin();
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 100000);

        AdminHelper.setProgrammeAuthyChallengeLimit(threeDSApp.getProgrammeId(), resetCount,
                adminToken);
    }

    /**
     * Test cases for 3ds with TWILIO_AUTHY
     * Ticket: <a href="https://weavr-payments.atlassian.net/browse/DEV-4262">...</a>
     * The main ticket for 3ds: <a href="https://weavr-payments.atlassian.net/browse/DEV-3575">...</a>
     *
     * AUTHY should be enabled on each level:
     * - programme
     * - identity profile
     *
     * The main case:
     * 1. create enrolled for AUTHY consumer/corporate
     * 2. create enrolled for AUTHY User with mobile number linked to the identity
     * 3. enrol User for AUTHY
     * 4. Call API with threeDSecureAuthConfig in payload
     */

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignPrepaidMC_PrimaryAuthy_Success(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, PREPAID_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, getAuthyIdentityToken());
        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("TWILIO_AUTHY")
                                .setFallbackChannel("OTP_SMS")
                                .build())
                        .build();

        final ValidatableResponse assignResponse =
                ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                        .then()
                        .statusCode(SC_OK)
                        .body("cardholderMobileNumber", equalTo(userMobile))
                        .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
                        .body("threeDSecureAuthConfig.primaryChannel", equalTo(assignManagedCardModel.getThreeDSecureAuthConfig().getPrimaryChannel()))
                        .body("threeDSecureAuthConfig.fallbackChannel", equalTo(assignManagedCardModel.getThreeDSecureAuthConfig().getFallbackChannel()));

//    Verify threeDSecureAuthConfig for assigned MC
        final Response getAssignedManagedCard = ManagedCardsHelper.getManagedCard(secretKey,
                assignResponse.extract().jsonPath().getString("id"), getAuthyIdentityToken());

        assertEquals(
                getAssignedManagedCard.jsonPath().getString("cardholderMobileNumber"),
                userMobile);
        assertEquals(
                getAssignedManagedCard.jsonPath().getString("threeDSecureAuthConfig.linkedUserId"),
                user.getLeft());
        assertEquals(
                getAssignedManagedCard.jsonPath().getString("threeDSecureAuthConfig.primaryChannel"),
                assignManagedCardModel.getThreeDSecureAuthConfig().getPrimaryChannel());
        assertEquals(
                getAssignedManagedCard.jsonPath().getString("threeDSecureAuthConfig.fallbackChannel"),
                assignManagedCardModel.getThreeDSecureAuthConfig().getFallbackChannel());
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignPrepaidMC_PrimaryAuthyWithoutFallback_Success(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, PREPAID_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, getAuthyIdentityToken());
        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("TWILIO_AUTHY")
                                .build())
                        .build();

        final ValidatableResponse assignResponse =
                ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                        .then()
                        .statusCode(SC_OK)
                        .body("cardholderMobileNumber", equalTo(userMobile))
                        .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
                        .body("threeDSecureAuthConfig.primaryChannel", equalTo(assignManagedCardModel.getThreeDSecureAuthConfig().getPrimaryChannel()))
                        .body("threeDSecureAuthConfig.fallbackChannel", equalTo("OTP_SMS"));

//    Verify threeDSecureAuthConfig for assigned MC
        final Response getAssignedManagedCard = ManagedCardsHelper.getManagedCard(secretKey,
                assignResponse.extract().jsonPath().getString("id"), getAuthyIdentityToken());

        assertEquals(
                getAssignedManagedCard.jsonPath().getString("cardholderMobileNumber"),
                userMobile);
        assertEquals(
                getAssignedManagedCard.jsonPath().getString("threeDSecureAuthConfig.linkedUserId"),
                user.getLeft());
        assertEquals(
                getAssignedManagedCard.jsonPath().getString("threeDSecureAuthConfig.primaryChannel"),
                assignManagedCardModel.getThreeDSecureAuthConfig().getPrimaryChannel());
        assertEquals(
                "OTP_SMS",
                getAssignedManagedCard.jsonPath().getString("threeDSecureAuthConfig.fallbackChannel"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignPrepaidMC_PrimaryOtpSms_Success(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, PREPAID_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, getAuthyIdentityToken());
        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("OTP_SMS")
                                .build())
                        .build();

        final ValidatableResponse assignResponse =
                ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                        .then()
                        .statusCode(SC_OK)
                        .body("cardholderMobileNumber", equalTo(userMobile))
                        .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
                        .body("threeDSecureAuthConfig.primaryChannel", equalTo(assignManagedCardModel.getThreeDSecureAuthConfig().getPrimaryChannel()))
                        .body("threeDSecureAuthConfig.fallbackChannel", equalTo(assignManagedCardModel.getThreeDSecureAuthConfig().getFallbackChannel()));

//    Verify threeDSecureAuthConfig for assigned MC
        final Response getAssignedManagedCard = ManagedCardsHelper.getManagedCard(secretKey,
                assignResponse.extract().jsonPath().getString("id"), getAuthyIdentityToken());

        assertEquals(
                getAssignedManagedCard.jsonPath().getString("cardholderMobileNumber"),
                userMobile);
        assertEquals(
                getAssignedManagedCard.jsonPath().getString("threeDSecureAuthConfig.linkedUserId"),
                user.getLeft());
        assertEquals(
                getAssignedManagedCard.jsonPath().getString("threeDSecureAuthConfig.primaryChannel"),
                assignManagedCardModel.getThreeDSecureAuthConfig().getPrimaryChannel());
        assertEquals(
                getAssignedManagedCard.jsonPath().getString("threeDSecureAuthConfig.fallbackChannel"),
                assignManagedCardModel.getThreeDSecureAuthConfig().getFallbackChannel());
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignPrepaidMC_WithoutThreeDSConfig_Success(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, PREPAID_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, getAuthyIdentityToken());
        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setCardholderMobileNumber(userMobile)
                        .build();

        final ValidatableResponse assignResponse =
                ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                        .then()
                        .statusCode(SC_OK)
                        .body("cardholderMobileNumber", equalTo(userMobile))
                        .body("threeDSecureAuthConfig", nullValue());

//    Verify threeDSecureAuthConfig for assigned MC
        final Response getAssignedManagedCard = ManagedCardsHelper.getManagedCard(secretKey,
                assignResponse.extract().jsonPath().getString("id"), getAuthyIdentityToken());

        assertEquals(
                getAssignedManagedCard.jsonPath().getString("cardholderMobileNumber"),
                userMobile);
        assertNull(
                getAssignedManagedCard.jsonPath().getString("threeDSecureAuthConfig"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignPrepaidMC_CrossIdentity_Conflict(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, PREPAID_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//        Create third party identity
        final String secondIdentityToken;
        if (getIdentityType().equals(IdentityType.CONSUMER)) {
            secondIdentityToken = ConsumersHelper.createEnrolledVerifiedConsumer(threeDSConsumerProfileId, secretKey).getRight();
        } else {
            secondIdentityToken = CorporatesHelper.createEnrolledVerifiedCorporate(threeDSCorporateProfileId, secretKey).getRight();
        }

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("TWILIO_AUTHY")
                                .setFallbackChannel("OTP_SMS")
                                .build())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, secondIdentityToken)
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("LINKED_USER_NOT_FOUND"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignPrepaidMC_TwoMobileNumbers_Conflict(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, PREPAID_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setCardholderMobileNumber(String.format("+356%s", RandomStringUtils.randomNumeric(8)))
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("OTP_SMS")
                                .build())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MORE_THAN_ONE_MOBILE_NUMBER_PROVIDED"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignPrepaidMC_NoThreeDSConfigNoMobileNumber_Conflict(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, PREPAID_MODE);

        final UsersModel usersModel = UsersModel.builder()
                .setName(RandomStringUtils.randomAlphabetic(6))
                .setSurname(RandomStringUtils.randomAlphabetic(6))
                .setEmail(String.format("%s@weavrusertest.io", RandomStringUtils.randomAlphabetic(10)))
                .build();

        UsersHelper.createAuthenticatedUser(usersModel, secretKey, getAuthyIdentityToken());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("THREEDS_DETAILS_NOT_PROVIDED"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignPrepaidMC_NotEnrolledForAuthy_Conflict(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, PREPAID_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, getAuthyIdentityToken());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("TWILIO_AUTHY")
                                .build())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("USER_NOT_ENROLLED_FOR_TWILIO_AUTHY"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignPrepaidMC_WrongUserId_Conflict(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, PREPAID_MODE);

        String userId = RandomStringUtils.randomNumeric(16);

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(userId)
                                .setPrimaryChannel("TWILIO_AUTHY")
                                .build())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignPrepaidMC_AuthyNotSupported_Conflict(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getBiometricIdentityUnassignedCards(), instrumentType, PREPAID_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getBiometricIdentityToken());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("TWILIO_AUTHY")
                                .build())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getBiometricIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TWILIO_AUTHY_AUTHENTICATION_NOT_SUPPORTED"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignPrepaidMC_UserInactive_Conflict(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, PREPAID_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

        UsersService.deactivateUser(secretKey, user.getLeft(), getAuthyIdentityToken())
                .then()
                .statusCode(SC_NO_CONTENT);
//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("TWILIO_AUTHY")
                                .build())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("USER_INACTIVE"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignPrepaidMC_InvalidChannel_Conflict(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, PREPAID_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());
//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("OTP_SMS")
                                .setFallbackChannel("OTP_SMS")
                                .build())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVALID_CHANNELS_SELECTED"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignPrepaidMC_InvalidPrimaryChannel_BadRequest(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, PREPAID_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("AUTHY")
                                .setFallbackChannel("OTP_SMS")
                                .build())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignPrepaidMC_InvalidFallbackChannel_BadRequest(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, PREPAID_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("TWILIO_AUTHY")
                                .setFallbackChannel("TWILIO_AUTHY")
                                .build())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignPrepaidMC_MobileNumberNotVerified_Success(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, PREPAID_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("TWILIO_AUTHY")
                                .setFallbackChannel("OTP_SMS")
                                .build())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                .then()
                .statusCode(SC_OK)
                .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
                .body("threeDSecureAuthConfig.primaryChannel", equalTo(assignManagedCardModel.getThreeDSecureAuthConfig().getPrimaryChannel()))
                .body("threeDSecureAuthConfig.fallbackChannel", equalTo(assignManagedCardModel.getThreeDSecureAuthConfig().getFallbackChannel()));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignPrepaidMC_NoMobileNumber_Conflict(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, PREPAID_MODE);

        final UsersModel usersModel = UsersModel.builder()
                .setName(RandomStringUtils.randomAlphabetic(6))
                .setSurname(RandomStringUtils.randomAlphabetic(6))
                .setEmail(String.format("%s@weavrusertest.io", RandomStringUtils.randomAlphabetic(10)))
                .build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, getAuthyIdentityToken());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("TWILIO_AUTHY")
                                .build())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("USER_MOBILE_NUMBER_DOES_NOT_EXIST"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignDebitMC_PrimaryAuthy_Success(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, DEBIT_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, getAuthyIdentityToken());
        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("TWILIO_AUTHY")
                                .setFallbackChannel("OTP_SMS")
                                .build())
                        .build();

        final ValidatableResponse assignResponse =
                ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                        .then()
                        .statusCode(SC_OK)
                        .body("cardholderMobileNumber", equalTo(userMobile))
                        .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
                        .body("threeDSecureAuthConfig.primaryChannel", equalTo(assignManagedCardModel.getThreeDSecureAuthConfig().getPrimaryChannel()))
                        .body("threeDSecureAuthConfig.fallbackChannel", equalTo(assignManagedCardModel.getThreeDSecureAuthConfig().getFallbackChannel()));

//    Verify threeDSecureAuthConfig for assigned MC
        final Response getAssignedManagedCard = ManagedCardsHelper.getManagedCard(secretKey,
                assignResponse.extract().jsonPath().getString("id"), getAuthyIdentityToken());

        assertEquals(
                getAssignedManagedCard.jsonPath().getString("cardholderMobileNumber"),
                userMobile);
        assertEquals(
                getAssignedManagedCard.jsonPath().getString("threeDSecureAuthConfig.linkedUserId"),
                user.getLeft());
        assertEquals(
                getAssignedManagedCard.jsonPath().getString("threeDSecureAuthConfig.primaryChannel"),
                assignManagedCardModel.getThreeDSecureAuthConfig().getPrimaryChannel());
        assertEquals(
                getAssignedManagedCard.jsonPath().getString("threeDSecureAuthConfig.fallbackChannel"),
                assignManagedCardModel.getThreeDSecureAuthConfig().getFallbackChannel());
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignDebitMC_PrimaryAuthyWithoutFallback_Success(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, DEBIT_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, getAuthyIdentityToken());
        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("TWILIO_AUTHY")
                                .build())
                        .build();

        final ValidatableResponse assignResponse =
                ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                        .then()
                        .statusCode(SC_OK)
                        .body("cardholderMobileNumber", equalTo(userMobile))
                        .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
                        .body("threeDSecureAuthConfig.primaryChannel", equalTo(assignManagedCardModel.getThreeDSecureAuthConfig().getPrimaryChannel()))
                        .body("threeDSecureAuthConfig.fallbackChannel", equalTo("OTP_SMS"));

//    Verify threeDSecureAuthConfig for assigned MC
        final Response getAssignedManagedCard = ManagedCardsHelper.getManagedCard(secretKey,
                assignResponse.extract().jsonPath().getString("id"), getAuthyIdentityToken());

        assertEquals(
                getAssignedManagedCard.jsonPath().getString("cardholderMobileNumber"),
                userMobile);
        assertEquals(
                getAssignedManagedCard.jsonPath().getString("threeDSecureAuthConfig.linkedUserId"),
                user.getLeft());
        assertEquals(
                getAssignedManagedCard.jsonPath().getString("threeDSecureAuthConfig.primaryChannel"),
                assignManagedCardModel.getThreeDSecureAuthConfig().getPrimaryChannel());
        assertEquals(
                "OTP_SMS",
                getAssignedManagedCard.jsonPath().getString("threeDSecureAuthConfig.fallbackChannel"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignDebitMC_PrimaryOtpSms_Success(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, DEBIT_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, getAuthyIdentityToken());
        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("OTP_SMS")
                                .build())
                        .build();

        final ValidatableResponse assignResponse =
                ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                        .then()
                        .statusCode(SC_OK)
                        .body("cardholderMobileNumber", equalTo(userMobile))
                        .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
                        .body("threeDSecureAuthConfig.primaryChannel", equalTo(assignManagedCardModel.getThreeDSecureAuthConfig().getPrimaryChannel()))
                        .body("threeDSecureAuthConfig.fallbackChannel", equalTo(assignManagedCardModel.getThreeDSecureAuthConfig().getFallbackChannel()));

//    Verify threeDSecureAuthConfig for assigned MC
        final Response getAssignedManagedCard = ManagedCardsHelper.getManagedCard(secretKey,
                assignResponse.extract().jsonPath().getString("id"), getAuthyIdentityToken());

        assertEquals(
                getAssignedManagedCard.jsonPath().getString("cardholderMobileNumber"),
                userMobile);
        assertEquals(
                getAssignedManagedCard.jsonPath().getString("threeDSecureAuthConfig.linkedUserId"),
                user.getLeft());
        assertEquals(
                getAssignedManagedCard.jsonPath().getString("threeDSecureAuthConfig.primaryChannel"),
                assignManagedCardModel.getThreeDSecureAuthConfig().getPrimaryChannel());
        assertEquals(
                getAssignedManagedCard.jsonPath().getString("threeDSecureAuthConfig.fallbackChannel"),
                assignManagedCardModel.getThreeDSecureAuthConfig().getFallbackChannel());
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignDebitMC_WithoutThreeDSConfig_Success(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, DEBIT_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, getAuthyIdentityToken());
        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setCardholderMobileNumber(userMobile)
                        .build();

        final ValidatableResponse assignResponse =
                ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                        .then()
                        .statusCode(SC_OK)
                        .body("cardholderMobileNumber", equalTo(userMobile))
                        .body("threeDSecureAuthConfig", nullValue());

//    Verify threeDSecureAuthConfig for assigned MC
        final Response getAssignedManagedCard = ManagedCardsHelper.getManagedCard(secretKey,
                assignResponse.extract().jsonPath().getString("id"), getAuthyIdentityToken());

        assertEquals(
                getAssignedManagedCard.jsonPath().getString("cardholderMobileNumber"),
                userMobile);
        assertNull(
                getAssignedManagedCard.jsonPath().getString("threeDSecureAuthConfig"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignDebitMC_CrossIdentity_Conflict(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, DEBIT_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//        Create third party identity
        final String secondIdentityToken;
        if (getIdentityType().equals(IdentityType.CONSUMER)) {
            secondIdentityToken = ConsumersHelper.createEnrolledVerifiedConsumer(threeDSConsumerProfileId, secretKey).getRight();
        } else {
            secondIdentityToken = CorporatesHelper.createEnrolledVerifiedCorporate(threeDSCorporateProfileId, secretKey).getRight();
        }

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("TWILIO_AUTHY")
                                .setFallbackChannel("OTP_SMS")
                                .build())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, secondIdentityToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("LINKED_USER_NOT_FOUND"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignDebitMC_TwoMobileNumbers_Conflict(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, DEBIT_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setCardholderMobileNumber(String.format("+356%s", RandomStringUtils.randomNumeric(8)))
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("OTP_SMS")
                                .build())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MORE_THAN_ONE_MOBILE_NUMBER_PROVIDED"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignDebitMC_NoThreeDSConfigNoMobileNumber_Conflict(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, DEBIT_MODE);

        final UsersModel usersModel = UsersModel.builder()
                .setName(RandomStringUtils.randomAlphabetic(6))
                .setSurname(RandomStringUtils.randomAlphabetic(6))
                .setEmail(String.format("%s@weavrusertest.io", RandomStringUtils.randomAlphabetic(10)))
                .build();

        UsersHelper.createAuthenticatedUser(usersModel, secretKey, getAuthyIdentityToken());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("THREEDS_DETAILS_NOT_PROVIDED"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignDebitMC_NotEnrolledForAuthy_Conflict(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, DEBIT_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, getAuthyIdentityToken());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("TWILIO_AUTHY")
                                .build())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("USER_NOT_ENROLLED_FOR_TWILIO_AUTHY"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignDebitMC_WrongUserId_NotFound(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, DEBIT_MODE);

        String userId = RandomStringUtils.randomNumeric(16);

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(userId)
                                .setPrimaryChannel("TWILIO_AUTHY")
                                .build())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignDebitMC_AuthyNotSupported_Conflict(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, DEBIT_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getBiometricIdentityToken());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("TWILIO_AUTHY")
                                .build())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getBiometricIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TWILIO_AUTHY_AUTHENTICATION_NOT_SUPPORTED"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignDebitMC_UserInactive_Conflict(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, DEBIT_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

        UsersService.deactivateUser(secretKey, user.getLeft(), getAuthyIdentityToken())
                .then()
                .statusCode(SC_NO_CONTENT);

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("TWILIO_AUTHY")
                                .build())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("USER_INACTIVE"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignDebitMC_InvalidChannel_Conflict(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, DEBIT_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("OTP_SMS")
                                .setFallbackChannel("OTP_SMS")
                                .build())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVALID_CHANNELS_SELECTED"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignDebitMC_InvalidPrimaryChannel_BadRequest(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, DEBIT_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("Authy")
                                .setFallbackChannel("OTP_SMS")
                                .build())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignDebitMC_InvalidFallbackChannel_BadRequest(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, DEBIT_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("OTP_SMS")
                                .setFallbackChannel("TWILIO_AUTHY")
                                .build())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignDebitMC_MobileNumberNotVerified_Success(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, DEBIT_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("OTP_SMS")
                                .build())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                .then()
            .statusCode(SC_OK)
            .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
            .body("threeDSecureAuthConfig.primaryChannel", equalTo(assignManagedCardModel.getThreeDSecureAuthConfig().getPrimaryChannel()))
            .body("threeDSecureAuthConfig.fallbackChannel", equalTo(assignManagedCardModel.getThreeDSecureAuthConfig().getFallbackChannel()));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void ThreeDSAuthyAssignDebitMC_NoMobileNumber_Conflict(final InstrumentType instrumentType) {
//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                getIdentityUnassignedCard(getAuthyIdentityUnassignedCards(), instrumentType, DEBIT_MODE);

        final UsersModel usersModel = UsersModel.builder()
                .setName(RandomStringUtils.randomAlphabetic(6))
                .setSurname(RandomStringUtils.randomAlphabetic(6))
                .setEmail(String.format("%s@weavrusertest.io", RandomStringUtils.randomAlphabetic(10)))
                .build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, getAuthyIdentityToken());

//      Assign MC
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.assignManagedCardWithoutMobileNumberModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                .setLinkedUserId(user.getLeft())
                                .setPrimaryChannel("OTP_SMS")
                                .build())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getAuthyIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("USER_MOBILE_NUMBER_DOES_NOT_EXIST"));
    }

    protected static List<InstrumentType> getInstrumentTypes() {
        return Arrays.asList(VIRTUAL, PHYSICAL);
    }

    private UnassignedManagedCardDetails getIdentityUnassignedCard(final List<UnassignedManagedCardDetails> identityUnassignedCards,
                                                                   final InstrumentType instrumentType,
                                                                   final ManagedCardMode managedCardMode) {//    get Unassigned cards
        final UnassignedManagedCardDetails unassignedCard =
                identityUnassignedCards.stream()
                        .filter(x -> x.getInstrumentType() == instrumentType
                                && x.getManagedCardMode() == managedCardMode
                                && x.getUnassignedCardResponseModel().isUnassigned())
                        .collect(Collectors.toList()).get(0);

        unassignedCard.getUnassignedCardResponseModel().setUnassigned(false);

        return unassignedCard;
    }
}
