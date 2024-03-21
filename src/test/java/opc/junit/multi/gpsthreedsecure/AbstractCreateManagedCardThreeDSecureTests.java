package opc.junit.multi.gpsthreedsecure;

import opc.enums.opc.CountryCode;
import opc.enums.opc.IdentityType;
import opc.enums.opc.LimitInterval;
import opc.enums.opc.ManagedCardMode;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.secure.SecureHelper;
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

@Tag(MultiTags.GPS_THREED_SECURE)
public abstract class AbstractCreateManagedCardThreeDSecureTests extends BaseGpsThreeDSecureSetup {

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

    @Test
    public void ThreeDSCreatePrepaidMC_PrimaryBiometrics_Success() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreatePrepaidManagedCardModel(
                    getIdentityPrepaidManagedCardProfileId(), user.getLeft()).build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("cardholderMobileNumber", equalTo(userMobile))
            .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
            .body("threeDSecureAuthConfig.primaryChannel", equalTo("BIOMETRICS"))
            .body("threeDSecureAuthConfig.fallbackChannel", equalTo("OTP_SMS"));
    }

    @Test
    public void ThreeDSCreateDebitMC_PrimaryBiometrics_Success() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getThreeDSIdentityToken());

        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreateDebitManagedCardModel(
                    getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId).build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("cardholderMobileNumber", equalTo(userMobile))
            .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
            .body("threeDSecureAuthConfig.primaryChannel", equalTo("BIOMETRICS"))
            .body("threeDSecureAuthConfig.fallbackChannel", equalTo("OTP_SMS"));
    }


    @Test
    public void ThreeDSCreatePrepaidMC_CrossIdentity_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

//        Create third party identity
        final String secondIdentityToken;
        if (getIdentityType().equals(IdentityType.CONSUMER)) {
            secondIdentityToken = ConsumersHelper.createEnrolledVerifiedConsumer(threeDSConsumerProfileId, secretKey).getRight();
        } else {
            secondIdentityToken = CorporatesHelper.createEnrolledVerifiedCorporate(threeDSCorporateProfileId, secretKey).getRight();
        }

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC by third party identity
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreatePrepaidManagedCardModel(
                    getIdentityPrepaidManagedCardProfileId(), user.getLeft()).build(), secretKey,
                secondIdentityToken, Optional.empty())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("LINKED_USER_NOT_FOUND"));
    }

    @Test
    public void ThreeDSCreateDebitMC_CrossIdentity_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

//        Create third party identity
        final String secondIdentityToken;
        if (getIdentityType().equals(IdentityType.CONSUMER)) {
            secondIdentityToken = ConsumersHelper.createEnrolledVerifiedConsumer(threeDSConsumerProfileId, secretKey).getRight();
        } else {
            secondIdentityToken = CorporatesHelper.createEnrolledVerifiedCorporate(threeDSCorporateProfileId, secretKey).getRight();
        }

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC by third party identity
        final String accountId = getManagedAccountId(secondIdentityToken);

        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreateDebitManagedCardModel(
                    getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId).build(), secretKey,
                secondIdentityToken, Optional.empty())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("LINKED_USER_NOT_FOUND"));
    }

    @Test
    public void ThreeDSCreatePrepaidMC_PrimaryOTP_Success() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());
        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        CreateManagedCardModel cardModel = CreateManagedCardModel.ThreeDSecureCreatePrepaidManagedCardModelPrimaryOTP(
            getIdentityPrepaidManagedCardProfileId(), user.getLeft()).build();

        ManagedCardsService.createManagedCard(cardModel, secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("cardholderMobileNumber", equalTo(userMobile))
            .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
            .body("threeDSecureAuthConfig.primaryChannel", equalTo("OTP_SMS"));
    }

    @Test
    public void ThreeDSCreateDebitMC_PrimaryOTP_Success() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());
        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getThreeDSIdentityToken());

        CreateManagedCardModel cardModel = CreateManagedCardModel.ThreeDSecureCreateDebitManagedCardModelPrimaryOTP(
            getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId).build();

        ManagedCardsService.createManagedCard(cardModel, secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("cardholderMobileNumber", equalTo(userMobile))
            .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
            .body("threeDSecureAuthConfig.primaryChannel", equalTo("OTP_SMS"));
    }

    @Test
    public void ThreeDSCreatePrepaidMC_WithoutThreeDSConfig_Success() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC
        final String mobileNumber = String.format("+356%s", RandomStringUtils.randomNumeric(8));
        CreateManagedCardModel cardModel = CreateManagedCardModel.DefaultCreateManagedCardModel(
                getIdentityPrepaidManagedCardProfileId())
            .setCardholderMobileNumber(mobileNumber)
            .setMode(ManagedCardMode.PREPAID_MODE.name())
            .setCurrency(getThreeDSIdentityCurrency())
            .build();

        ManagedCardsService.createManagedCard(cardModel, secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("cardholderMobileNumber", equalTo(mobileNumber))
            .body("threeDSecureAuthConfig", nullValue());
    }

    @Test
    public void ThreeDSCreateDebitMC_WithoutThreeDSConfig_Success() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC
        final String accountId = getManagedAccountId(getThreeDSIdentityToken());
        final String mobileNumber = String.format("+356%s", RandomStringUtils.randomNumeric(8));
        CreateManagedCardModel cardModel = CreateManagedCardModel.DefaultCreateManagedCardModel(
                getIdentityDebitManagedCardProfileId())
            .setCardholderMobileNumber(mobileNumber)
            .setMode(ManagedCardMode.DEBIT_MODE.name())
            .setParentManagedAccountId(accountId)
            .setCurrency(getThreeDSIdentityCurrency())
            .build();

        ManagedCardsService.createManagedCard(cardModel, secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("cardholderMobileNumber", equalTo(mobileNumber))
            .body("threeDSecureAuthConfig", nullValue());
    }

    @Test
    public void ThreeDSCreatePrepaidMC_TwoMobileNumbers_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreatePrepaidManagedCardModel(
                        getIdentityPrepaidManagedCardProfileId(), user.getLeft())
                    .setCardholderMobileNumber(String.format("+356%s", RandomStringUtils.randomNumeric(8)))
                    .build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("MORE_THAN_ONE_MOBILE_NUMBER_PROVIDED"));
    }

    @Test
    public void ThreeDSCreateDebitMC_TwoMobileNumbers_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getThreeDSIdentityToken());

        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreateDebitManagedCardModel(
                        getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId)
                    .setCardholderMobileNumber(String.format("+356%s", RandomStringUtils.randomNumeric(8)))
                    .build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("MORE_THAN_ONE_MOBILE_NUMBER_PROVIDED"));
    }

    @Test
    public void ThreeDSCreatePrepaidMC_NoThreeDSConfigNoMobileNumber_Conflict() {
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
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("THREEDS_DETAILS_NOT_PROVIDED"));
    }

    @Test
    public void ThreeDSCreateDebitMC_NoThreeDSConfigNoMobileNumber_Conflict() {
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

//    Create MC
        final String accountId = getManagedAccountId(getThreeDSIdentityToken());
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
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("THREEDS_DETAILS_NOT_PROVIDED"));
    }

    @Test
    public void ThreeDSCreatePrepaidMC_NotEnrolledForBiometrics_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

//    Create MC with linkedUserId = userId
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreatePrepaidManagedCardModel(
                    getIdentityPrepaidManagedCardProfileId(), user.getLeft()).build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("USER_NOT_ENROLLED_FOR_BIOMETRICS"));
    }

    @Test
    public void ThreeDSCreateDebitMC_NotEnrolledForBiometrics_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getThreeDSIdentityToken());
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreateDebitManagedCardModel(
                    getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId).build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("USER_NOT_ENROLLED_FOR_BIOMETRICS"));
    }

    @Test
    public void ThreeDSCreatePrepaidMC_WrongUserId_NotFound() {
        String userId = RandomStringUtils.randomNumeric(16);

        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreatePrepaidManagedCardModel(
                    getIdentityPrepaidManagedCardProfileId(), userId).build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ThreeDSCreateDebitMC_WrongUserId_NotFound() {
        String userId = RandomStringUtils.randomNumeric(16);

        final String accountId = getManagedAccountId(getThreeDSIdentityToken());
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreateDebitManagedCardModel(
                    getIdentityDebitManagedCardProfileId(), userId, accountId).build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ThreeDSCreatePrepaidMC_IdentityProfileWithoutBiometric_BiometricNotSupported() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getIdentityToken());

//    Create MC with linkedUserId = userId
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreatePrepaidManagedCardModel(
                    getIdentityPrepaidManagedCardProfileId(), user.getLeft()).build(), secretKey,
                getIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("BIOMETRIC_AUTHENTICATION_NOT_SUPPORTED"));
    }

    @Test
    public void ThreeDSCreateDebitMC_IdentityProfileWithoutBiometric_BiometricNotSupported() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getIdentityToken());

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getIdentityToken());
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreateDebitManagedCardModel(
                    getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId).build(), secretKey,
                getIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("BIOMETRIC_AUTHENTICATION_NOT_SUPPORTED"));
    }

    @Test
    public void ThreeDSCreatePrepaidMC_DeactivateUser_UserInactive() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

        UsersService.deactivateUser(secretKey, user.getLeft(), getThreeDSIdentityToken())
            .then()
            .statusCode(SC_NO_CONTENT);

//    Create MC with linkedUserId = userId
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreatePrepaidManagedCardModel(
                    getIdentityPrepaidManagedCardProfileId(), user.getLeft()).build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("USER_INACTIVE"));
    }

    @Test
    public void ThreeDSCreateDebitMC_DeactivateUser_UserInactive() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

        UsersService.deactivateUser(secretKey, user.getLeft(), getThreeDSIdentityToken())
            .then()
            .statusCode(SC_NO_CONTENT);

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getThreeDSIdentityToken());
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreateDebitManagedCardModel(
                    getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId).build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("USER_INACTIVE"));
    }

    @Test
    public void ThreeDSCreatePrepaidMC_InvalidChannels_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreatePrepaidManagedCardModel(
                        getIdentityPrepaidManagedCardProfileId(), user.getLeft())
                    .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                        .setLinkedUserId(user.getLeft())
                        .setPrimaryChannel("OTP_SMS")
                        .setFallbackChannel("OTP_SMS")
                        .build())
                    .build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("INVALID_CHANNELS_SELECTED"));
    }

    @Test
    public void ThreeDSCreateDebitMC_InvalidChannels_Conflict() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getThreeDSIdentityToken());
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreateDebitManagedCardModel(
                        getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId)
                    .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                        .setLinkedUserId(user.getLeft())
                        .setPrimaryChannel("OTP_SMS")
                        .setFallbackChannel("OTP_SMS")
                        .build())
                    .build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("INVALID_CHANNELS_SELECTED"));
    }

    @Test
    public void ThreeDSCreatePrepaidMC_InvalidPrimaryChannel_BadRequest() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreatePrepaidManagedCardModel(
                        getIdentityPrepaidManagedCardProfileId(), user.getLeft())
                    .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                        .setLinkedUserId(user.getLeft())
                        .setPrimaryChannel("BIOMETRIC")
                        .setFallbackChannel("OTP_SMS")
                        .build())
                    .build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ThreeDSCreateDebitMC_InvalidPrimaryChannel_BadRequest() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getThreeDSIdentityToken());
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreateDebitManagedCardModel(
                        getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId)
                    .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                        .setLinkedUserId(user.getLeft())
                        .setPrimaryChannel("BIOMETRIC")
                        .setFallbackChannel("OTP_SMS")
                        .build())
                    .build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ThreeDSCreatePrepaidMC_InvalidFallbackChannel_BadRequest() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreatePrepaidManagedCardModel(
                        getIdentityPrepaidManagedCardProfileId(), user.getLeft())
                    .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                        .setLinkedUserId(user.getLeft())
                        .setPrimaryChannel("BIOMETRICS")
                        .setFallbackChannel("BIOMETRICS")
                        .build())
                    .build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ThreeDSCreateDebitMC_InvalidFallbackChannel_BadRequest() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getThreeDSIdentityToken());
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreateDebitManagedCardModel(
                        getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId)
                    .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                        .setLinkedUserId(user.getLeft())
                        .setPrimaryChannel("BIOMETRICS")
                        .setFallbackChannel("BIOMETRICS")
                        .build())
                    .build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ThreeDSCreatePrepaidMC_FallbackChannelNotSelected_Success() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreatePrepaidManagedCardModel(
                        getIdentityPrepaidManagedCardProfileId(), user.getLeft())
                    .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                        .setLinkedUserId(user.getLeft())
                        .setPrimaryChannel("BIOMETRICS")
                        .build())
                    .build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("cardholderMobileNumber", equalTo(userMobile))
            .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
            .body("threeDSecureAuthConfig.primaryChannel", equalTo("BIOMETRICS"))
            .body("threeDSecureAuthConfig.fallbackChannel", equalTo("OTP_SMS"));
    }

    @Test
    public void ThreeDSCreateDebitMC_FallbackChannelNotSelected_Success() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getThreeDSIdentityToken());
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreateDebitManagedCardModel(
                        getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId)
                    .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                        .setLinkedUserId(user.getLeft())
                        .setPrimaryChannel("BIOMETRICS")
                        .build())
                    .build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("cardholderMobileNumber", equalTo(userMobile))
            .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
            .body("threeDSecureAuthConfig.primaryChannel", equalTo("BIOMETRICS"))
            .body("threeDSecureAuthConfig.fallbackChannel", equalTo("OTP_SMS"));
    }

    @Test
    public void ThreeDSCreatePrepaidMC_MobileNumberNotVerified_Success() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey,
            getThreeDSIdentityToken());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreatePrepaidManagedCardModel(
                    getIdentityPrepaidManagedCardProfileId(), user.getLeft()).build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
            .body("threeDSecureAuthConfig.primaryChannel", equalTo("BIOMETRICS"))
            .body("threeDSecureAuthConfig.fallbackChannel", equalTo("OTP_SMS"));
    }

    @Test
    public void ThreeDSCreateDebitMC_MobileNumberNotVerified_Success() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey,
            getThreeDSIdentityToken());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//    Create MC with linkedUserId = userId
        final String accountId = getManagedAccountId(getThreeDSIdentityToken());
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreateDebitManagedCardModel(
                    getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId).build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("threeDSecureAuthConfig.linkedUserId", equalTo(user.getLeft()))
            .body("threeDSecureAuthConfig.primaryChannel", equalTo("BIOMETRICS"))
            .body("threeDSecureAuthConfig.fallbackChannel", equalTo("OTP_SMS"));
    }

    @Test
    public void ThreeDSCreatePrepaidMC_NoMobileNumber_Conflict() {
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
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreatePrepaidManagedCardModel(
                    getIdentityPrepaidManagedCardProfileId(), user.getLeft()).build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("USER_MOBILE_NUMBER_DOES_NOT_EXIST"));
    }

    @Test
    public void ThreeDSCreateDebitMC_NoMobileNumber_Conflict() {
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
        final String accountId = getManagedAccountId(getThreeDSIdentityToken());
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreateDebitManagedCardModel(
                    getIdentityDebitManagedCardProfileId(), user.getLeft(), accountId).build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("USER_MOBILE_NUMBER_DOES_NOT_EXIST"));
    }

    @Test
    public void ThreeDSCreatePrepaidMC_InvalidUserIdProvided_BadRequest() {
//    create enrolled User linked to Identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
            .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
            getThreeDSIdentityToken());

        final String userMobile = String.format("+356%s", usersModel.getMobile().getNumber());

//      Enrol device for Biometric
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

//      Create MC with invalid linked user id
        ManagedCardsService.createManagedCard(
                CreateManagedCardModel.ThreeDSecureCreatePrepaidManagedCardModel(
                    getIdentityPrepaidManagedCardProfileId(), RandomStringUtils.randomAlphabetic(8)).build(), secretKey,
                getThreeDSIdentityToken(), Optional.empty())
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("message", equalTo("request.threeDSecureAuthConfig.linkedUserId: must match \"^[0-9]+$\""));
    }

    private String getManagedAccountId(String authenticationToken) {
        return ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileId(),
            getIdentityCurrency(), secretKey, authenticationToken);
    }
}
