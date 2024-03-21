package opc.junit.multi.gpsthreedsecure;

import opc.enums.opc.CountryCode;
import opc.enums.opc.IdentityType;
import opc.enums.opc.LimitInterval;
import opc.enums.opc.ManagedCardMode;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.ThreeDSecureAuthConfigModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.AddressModel;
import opc.services.admin.AdminService;
import opc.services.multi.ManagedCardsService;
import opc.services.multi.UsersService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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


@Tag(MultiTags.GPS_THREED_SECURE_AUTHY)
public abstract class AbstractCreateManagedCardAuthyThreeDSecureTests extends BaseGpsThreeDSecureSetup {
    protected abstract String getBiometricIdentityToken();

    protected abstract String getAuthyIdentityToken();

    protected abstract String getAuthyIdentityCurrency();

    protected abstract String getIdentityPrepaidManagedCardProfileId();

    protected abstract String getIdentityDebitManagedCardProfileId();

    protected abstract String getIdentityManagedAccountProfileId();

    protected abstract IdentityType getIdentityType();

    @BeforeAll
    public static void setup() {

        final String adminToken = AdminService.loginAdmin();
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 100000);

        AdminHelper.resetProgrammeAuthyLimitsCounter(programmeId, adminToken);
        AdminHelper.setProgrammeAuthyChallengeLimit(programmeId, resetCount,
                adminToken);
    }

    /**
     * Test cases for 3ds with TWILIO_AUTHY
     * Ticket: https://weavr-payments.atlassian.net/browse/DEV-4262
     * The main ticket for 3ds: https://weavr-payments.atlassian.net/browse/DEV-3575
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

    @Test
    public void ThreeDSAuthyCreatePrepaidMC_PrimaryAuthy_Success() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getAuthyIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//    Create MC with linkedUserId = userId
        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreatePrepaidManagedCardModel(
                                getIdentityPrepaidManagedCardProfileId(), user.getLeft()).build(), secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("cardholderMobileNumber", equalTo(userMobile))
                .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
                .body("threeDSecureAuthConfig.primaryChannel", equalTo("TWILIO_AUTHY"))
                .body("threeDSecureAuthConfig.fallbackChannel", equalTo("OTP_SMS"));
    }

    @Test
    public void ThreeDSAuthyCreateDebitMC_PrimaryAuthy_Success() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getAuthyIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getAuthyIdentityToken());

        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreateDebitManagedCardModel(
                                getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId).build(), secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("cardholderMobileNumber", equalTo(userMobile))
                .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
                .body("threeDSecureAuthConfig.primaryChannel", equalTo("TWILIO_AUTHY"))
                .body("threeDSecureAuthConfig.fallbackChannel", equalTo("OTP_SMS"));
    }

    @Test
    public void ThreeDSAuthyCreatePrepaidMC_PrimaryOTP_Success() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getAuthyIdentityToken());
        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//    Create MC with linkedUserId = userId
        CreateManagedCardModel cardModel = CreateManagedCardModel.ThreeDSecureCreatePrepaidManagedCardModelPrimaryOTP(
                getIdentityPrepaidManagedCardProfileId(), user.getLeft()).build();

        ManagedCardsService.createManagedCard(cardModel, secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("cardholderMobileNumber", equalTo(userMobile))
                .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
                .body("threeDSecureAuthConfig.primaryChannel", equalTo("OTP_SMS"));
    }

    @Test
    public void ThreeDSAuthyCreateDebitMC_PrimaryOTP_Success() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getAuthyIdentityToken());
        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getAuthyIdentityToken());

        CreateManagedCardModel cardModel = CreateManagedCardModel.ThreeDSecureCreateDebitManagedCardModelPrimaryOTP(
                getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId).build();

        ManagedCardsService.createManagedCard(cardModel, secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("cardholderMobileNumber", equalTo(userMobile))
                .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
                .body("threeDSecureAuthConfig.primaryChannel", equalTo("OTP_SMS"));
    }

    @Test
    public void ThreeDSAuthyCreatePrepaidMC_WithoutThreeDSConfig_Success() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//    Create MC
        final String mobileNumber = String.format("+356%s", RandomStringUtils.randomNumeric(8));
        CreateManagedCardModel cardModel = CreateManagedCardModel.DefaultCreateManagedCardModel(
                        getIdentityPrepaidManagedCardProfileId())
                .setCardholderMobileNumber(mobileNumber)
                .setMode(ManagedCardMode.PREPAID_MODE.name())
                .setCurrency(getAuthyIdentityCurrency())
                .build();

        ManagedCardsService.createManagedCard(cardModel, secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("cardholderMobileNumber", equalTo(mobileNumber))
                .body("threeDSecureAuthConfig", nullValue());
    }

    @Test
    public void ThreeDSAuthyCreateDebitMC_WithoutThreeDSConfig_Success() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//    Create MC
        final String accountId = getManagedAccountId(getAuthyIdentityToken());
        final String mobileNumber = String.format("+356%s", RandomStringUtils.randomNumeric(8));
        CreateManagedCardModel cardModel = CreateManagedCardModel.DefaultCreateManagedCardModel(
                        getIdentityDebitManagedCardProfileId())
                .setCardholderMobileNumber(mobileNumber)
                .setMode(ManagedCardMode.DEBIT_MODE.name())
                .setParentManagedAccountId(accountId)
                .setCurrency(getAuthyIdentityCurrency())
                .build();

        ManagedCardsService.createManagedCard(cardModel, secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("cardholderMobileNumber", equalTo(mobileNumber))
                .body("threeDSecureAuthConfig", nullValue());
    }

    @Test
    public void ThreeDSAuthyCreatePrepaidMC_CrossIdentity_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getAuthyIdentityToken());

//        Create third party identity
        final String secondIdentityToken;
        if (getIdentityType().equals(IdentityType.CONSUMER)) {
            secondIdentityToken = ConsumersHelper.createEnrolledVerifiedConsumer(consumerProfileId, secretKey).getRight();
        } else {
            secondIdentityToken = CorporatesHelper.createEnrolledVerifiedCorporate(corporateProfileId, secretKey).getRight();
        }

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//    Create MC by third party identity
        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreatePrepaidManagedCardModel(
                                getIdentityPrepaidManagedCardProfileId(), user.getLeft()).build(), secretKey,
                        secondIdentityToken, Optional.empty())
                .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("LINKED_USER_NOT_FOUND"));
    }

    @Test
    public void ThreeDSAuthyCreateDebitMC_CrossIdentity_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getAuthyIdentityToken());

//        Create third party identity
        final String secondIdentityToken;
        if (getIdentityType().equals(IdentityType.CONSUMER)) {
            secondIdentityToken = ConsumersHelper.createEnrolledVerifiedConsumer(consumerProfileId, secretKey).getRight();
        } else {
            secondIdentityToken = CorporatesHelper.createEnrolledVerifiedCorporate(corporateProfileId, secretKey).getRight();
        }

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//    Create MC by third party identity
        final String accountId = getManagedAccountId(secondIdentityToken);

        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreateDebitManagedCardModel(
                                getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId).build(), secretKey,
                        secondIdentityToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("LINKED_USER_NOT_FOUND"));
    }

    @Test
    public void ThreeDSAuthyCreatePrepaidMC_TwoMobileNumbers_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//    Create MC with linkedUserId = userId
        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreatePrepaidManagedCardModel(
                                        getIdentityPrepaidManagedCardProfileId(), user.getLeft())
                                .setCardholderMobileNumber(String.format("+356%s", RandomStringUtils.randomNumeric(8)))
                                .build(), secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MORE_THAN_ONE_MOBILE_NUMBER_PROVIDED"));
    }

    @Test
    public void ThreeDSAuthyCreateDebitMC_TwoMobileNumbers_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getAuthyIdentityToken());

        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreateDebitManagedCardModel(
                                        getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId)
                                .setCardholderMobileNumber(String.format("+356%s", RandomStringUtils.randomNumeric(8)))
                                .build(), secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MORE_THAN_ONE_MOBILE_NUMBER_PROVIDED"));
    }

    @Test
    public void ThreeDSAuthyCreatePrepaidMC_NoThreeDSConfigNoMobileNumber_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.builder()
                .setName(RandomStringUtils.randomAlphabetic(6))
                .setSurname(RandomStringUtils.randomAlphabetic(6))
                .setEmail(String.format("%s@weavrusertest.io", RandomStringUtils.randomAlphabetic(10)))
                .build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey,
                getAuthyIdentityToken());

//    Create MC
        CreateManagedCardModel cardModel = CreateManagedCardModel.builder()
                .setProfileId(getIdentityPrepaidManagedCardProfileId())
                .setFriendlyName(RandomStringUtils.randomAlphabetic(5))
                .setNameOnCard(String.format("%s-%s", RandomStringUtils.randomAlphabetic(5),
                        RandomStringUtils.randomAlphabetic(5)))
                .setBillingAddress(AddressModel.DefaultAddressModel().setCountry(CountryCode.MT).build())
                .setCurrency("EUR")
                .setMode(ManagedCardMode.PREPAID_MODE.name())
                .build();
        ManagedCardsService.createManagedCard(cardModel, secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("THREEDS_DETAILS_NOT_PROVIDED"));
    }

    @Test
    public void ThreeDSAuthyCreateDebitMC_NoThreeDSConfigNoMobileNumber_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.builder()
                .setName(RandomStringUtils.randomAlphabetic(6))
                .setSurname(RandomStringUtils.randomAlphabetic(6))
                .setEmail(String.format("%s@weavrusertest.io", RandomStringUtils.randomAlphabetic(10)))
                .build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey,
                getAuthyIdentityToken());

//    Create MC
        final String accountId = getManagedAccountId(getAuthyIdentityToken());
        CreateManagedCardModel cardModel = CreateManagedCardModel.builder()
                .setProfileId(getIdentityDebitManagedCardProfileId())
                .setFriendlyName(RandomStringUtils.randomAlphabetic(5))
                .setNameOnCard(String.format("%s-%s", RandomStringUtils.randomAlphabetic(5),
                        RandomStringUtils.randomAlphabetic(5)))
                .setBillingAddress(AddressModel.DefaultAddressModel().setCountry(CountryCode.MT).build())
                .setCurrency("EUR")
                .setMode(ManagedCardMode.DEBIT_MODE.name())
                .setParentManagedAccountId(accountId)
                .build();
        ManagedCardsService.createManagedCard(cardModel, secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("THREEDS_DETAILS_NOT_PROVIDED"));
    }

    @Test
    public void ThreeDSAuthyCreatePrepaidMC_NotEnrolledForAuthy_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getAuthyIdentityToken());

//    Create MC with linkedUserId = userId
        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreatePrepaidManagedCardModel(
                                getIdentityPrepaidManagedCardProfileId(), user.getLeft()).build(), secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("USER_NOT_ENROLLED_FOR_TWILIO_AUTHY"));
    }

    @Test
    public void ThreeDSAuthyCreateDebitMC_NotEnrolledForAuthy_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getAuthyIdentityToken());

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getAuthyIdentityToken());
        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreateDebitManagedCardModel(
                                getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId).build(), secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("USER_NOT_ENROLLED_FOR_TWILIO_AUTHY"));
    }

    @Test
    public void ThreeDSAuthyCreatePrepaidMC_WrongUserId_NotFound() {
        String userId = RandomStringUtils.randomNumeric(16);

        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreatePrepaidManagedCardModel(
                                getIdentityPrepaidManagedCardProfileId(), userId).build(), secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ThreeDSAuthyCreateDebitMC_WrongUserId_NotFound() {
        String userId = RandomStringUtils.randomNumeric(16);

        final String accountId = getManagedAccountId(getAuthyIdentityToken());
        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreateDebitManagedCardModel(
                                getIdentityDebitManagedCardProfileId(), userId, accountId).build(), secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ThreeDSAuthyCreatePrepaidMC_AuthyNotSupported_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getBiometricIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//    Create MC with linkedUserId = userId
        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreatePrepaidManagedCardModel(
                                getIdentityPrepaidManagedCardProfileId(), user.getLeft()).build(), secretKey,
                        getBiometricIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TWILIO_AUTHY_AUTHENTICATION_NOT_SUPPORTED"));
    }

    @Test
    public void ThreeDSAuthyCreateDebitMC_AuthyNotSupported_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getBiometricIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getBiometricIdentityToken());
        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreateDebitManagedCardModel(
                                getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId).build(), secretKey,
                        getBiometricIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TWILIO_AUTHY_AUTHENTICATION_NOT_SUPPORTED"));
    }

    @Test
    public void ThreeDSAuthyCreatePrepaidMC_UserInactive_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

        UsersService.deactivateUser(secretKey, user.getLeft(), getAuthyIdentityToken())
                .then()
                .statusCode(SC_NO_CONTENT);

//    Create MC with linkedUserId = userId
        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreatePrepaidManagedCardModel(
                                getIdentityPrepaidManagedCardProfileId(), user.getLeft()).build(), secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("USER_INACTIVE"));
    }

    @Test
    public void ThreeDSAuthyCreateDebitMC_UserInactive_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

        UsersService.deactivateUser(secretKey, user.getLeft(), getAuthyIdentityToken())
                .then()
                .statusCode(SC_NO_CONTENT);

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getAuthyIdentityToken());
        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreateDebitManagedCardModel(
                                getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId).build(), secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("USER_INACTIVE"));
    }

    @Test
    public void ThreeDSAuthyCreatePrepaidMC_InvalidChannel_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//    Create MC with linkedUserId = userId
        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreatePrepaidManagedCardModel(
                                        getIdentityPrepaidManagedCardProfileId(), user.getLeft())
                                .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                        .setLinkedUserId(user.getLeft())
                                        .setPrimaryChannel("OTP_SMS")
                                        .setFallbackChannel("OTP_SMS")
                                        .build())
                                .build(), secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVALID_CHANNELS_SELECTED"));
    }

    @Test
    public void ThreeDSAuthyCreateDebitMC_InvalidChannel_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getAuthyIdentityToken());
        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreateDebitManagedCardModel(
                                        getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId)
                                .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                        .setLinkedUserId(user.getLeft())
                                        .setPrimaryChannel("OTP_SMS")
                                        .setFallbackChannel("OTP_SMS")
                                        .build())
                                .build(), secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVALID_CHANNELS_SELECTED"));
    }

    @Test
    public void ThreeDSAuthyCreatePrepaidMC_InvalidFallbackChannel_BadRequest() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//    Create MC with linkedUserId = userId
        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreatePrepaidManagedCardModel(
                                        getIdentityPrepaidManagedCardProfileId(), user.getLeft())
                                .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                        .setLinkedUserId(user.getLeft())
                                        .setPrimaryChannel("TWILIO_AUTHY")
                                        .setFallbackChannel("TWILIO_AUTHY")
                                        .build())
                                .build(), secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ThreeDSAuthyCreateDebitMC_InvalidFallbackChannel_BadRequest() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getAuthyIdentityToken());
        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreateDebitManagedCardModel(
                                        getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId)
                                .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                        .setLinkedUserId(user.getLeft())
                                        .setPrimaryChannel("OTP_SMS")
                                        .setFallbackChannel("TWILIO_AUTHY")
                                        .build())
                                .build(), secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ThreeDSAuthyCreatePrepaidMC_InvalidPrimaryChannel_BadRequest() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//    Create MC with linkedUserId = userId
        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreatePrepaidManagedCardModel(
                                        getIdentityPrepaidManagedCardProfileId(), user.getLeft())
                                .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                        .setLinkedUserId(user.getLeft())
                                        .setPrimaryChannel("AUTHY")
                                        .setFallbackChannel("OTP_SMS")
                                        .build())
                                .build(), secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ThreeDSAuthyCreateDebitMC_InvalidPrimaryChannel_BadRequest() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getAuthyIdentityToken());
        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreateDebitManagedCardModel(
                                        getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId)
                                .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                        .setLinkedUserId(user.getLeft())
                                        .setPrimaryChannel("AUTHY")
                                        .setFallbackChannel("OTP_SMS")
                                        .build())
                                .build(), secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ThreeDSAuthyCreatePrepaidMC_FallbackChannelNotSelected_Success() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getAuthyIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//    Create MC with linkedUserId = userId
        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreatePrepaidManagedCardModel(
                                        getIdentityPrepaidManagedCardProfileId(), user.getLeft())
                                .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                        .setLinkedUserId(user.getLeft())
                                        .setPrimaryChannel("TWILIO_AUTHY")
                                        .build())
                                .build(), secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("cardholderMobileNumber", equalTo(userMobile))
                .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
                .body("threeDSecureAuthConfig.primaryChannel", equalTo("TWILIO_AUTHY"))
                .body("threeDSecureAuthConfig.fallbackChannel", equalTo("OTP_SMS"));
    }

    @Test
    public void ThreeDSAuthyCreateDebitMC_FallbackChannelNotSelected_Success() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                getAuthyIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getAuthyIdentityToken());
        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreateDebitManagedCardModel(
                                        getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId)
                                .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                                        .setLinkedUserId(user.getLeft())
                                        .setPrimaryChannel("TWILIO_AUTHY")
                                        .build())
                                .build(), secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("cardholderMobileNumber", equalTo(userMobile))
                .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
                .body("threeDSecureAuthConfig.primaryChannel", equalTo("TWILIO_AUTHY"))
                .body("threeDSecureAuthConfig.fallbackChannel", equalTo("OTP_SMS"));
    }

    @Test
    public void ThreeDSAuthyCreatePrepaidMC_MobileNumberNotVerified_Success() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey,
                getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//    Create MC with linkedUserId = userId
        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreatePrepaidManagedCardModel(
                                getIdentityPrepaidManagedCardProfileId(), user.getLeft()).build(), secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
            .statusCode(SC_OK)
            .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
            .body("threeDSecureAuthConfig.primaryChannel", equalTo("TWILIO_AUTHY"))
            .body("threeDSecureAuthConfig.fallbackChannel", equalTo("OTP_SMS"));
    }

    @Test
    public void ThreeDSAuthyCreateDebitMC_MobileNumberNotVerified_Success() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey,
                getAuthyIdentityToken());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getAuthyIdentityToken());
        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreateDebitManagedCardModel(
                                getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId).build(), secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
            .statusCode(SC_OK)
            .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
            .body("threeDSecureAuthConfig.primaryChannel", equalTo("TWILIO_AUTHY"))
            .body("threeDSecureAuthConfig.fallbackChannel", equalTo("OTP_SMS"));
    }

    @Test
    public void ThreeDSAuthyCreatePrepaidMC_NoMobileNumber_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.builder()
                .setName(RandomStringUtils.randomAlphabetic(6))
                .setSurname(RandomStringUtils.randomAlphabetic(6))
                .setEmail(String.format("%s@weavrusertest.io", RandomStringUtils.randomAlphabetic(10)))
                .build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey,
                getAuthyIdentityToken());

//    Create MC with linkedUserId = userId
        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreatePrepaidManagedCardModel(
                                getIdentityPrepaidManagedCardProfileId(), user.getLeft()).build(), secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("USER_MOBILE_NUMBER_DOES_NOT_EXIST"));
    }

    @Test
    public void ThreeDSAuthyCreateDebitMC_NoMobileNumber_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.builder()
                .setName(RandomStringUtils.randomAlphabetic(6))
                .setSurname(RandomStringUtils.randomAlphabetic(6))
                .setEmail(String.format("%s@weavrusertest.io", RandomStringUtils.randomAlphabetic(10)))
                .build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey,
                getAuthyIdentityToken());

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getAuthyIdentityToken());
        ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreateDebitManagedCardModel(
                                getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId).build(), secretKey,
                        getAuthyIdentityToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("USER_MOBILE_NUMBER_DOES_NOT_EXIST"));
    }

    @Test
    public void ThreeDSAuthyCreatePrepaidMC_InvalidUserIdProvided_BadRequest() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getAuthyIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//    Enrol device for AUTHY
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
            user.getRight());

//    Create MC with invalid linked user id
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.AuthyThreeDSecureCreatePrepaidManagedCardModel(
                    getIdentityPrepaidManagedCardProfileId(), RandomStringUtils.randomAlphabetic(8)).build(), secretKey,
                getAuthyIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("message", equalTo("request.threeDSecureAuthConfig.linkedUserId: must match \"^[0-9]+$\""));
    }

    private String getManagedAccountId(String authenticationToken) {
        return ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileId(),
                getAuthyIdentityCurrency(), secretKey, authenticationToken);
    }

}
