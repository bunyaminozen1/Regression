package fpi.paymentrun.identities.buyers;

import commons.enums.Roles;
import commons.models.DateOfBirthModel;
import commons.models.MobileNumberModel;
import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.AdminUserModel;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.models.UpdateBuyerModel;
import fpi.paymentrun.models.UpdateCompanyModel;
import fpi.paymentrun.services.BuyersService;
import opc.enums.mailhog.MailHogSms;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.models.innovator.DeactivateIdentityModel;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.shared.AddressModel;
import opc.models.shared.EmailVerificationModel;
import opc.services.innovator.InnovatorService;
import opc.services.mailhog.MailhogService;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Tag(PluginsTags.PAYMENT_RUN_BUYERS)
public class UpdateBuyerTests extends BasePaymentRunSetup {
    /**
     * The following Buyer details are verified during due diligence (KYB) and cannot be updated via the API once the Buyer has been verified:
     * Admin User Name
     * Admin User Surname
     * Admin User Email
     * Admin User Mobile Country Code
     * Admin User Mobile Number
     * Date of Birth
     * Business Address
     */

    private CreateBuyerModel createBuyerModel;
    private String buyerId;
    private String buyerToken;
    private String buyerEmail;
    final private static int emailChangeBlockingLimit = 10;
    final private static int mobileChangeBlockingLimit = 10;
    final private static int mobileChangeLimit = 3;

    private final static String VERIFICATION_CODE = "123456";

    @BeforeEach
    public void Setup() {
        createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();

        final Pair<String, String> buyer = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);
        buyerId = buyer.getLeft();
        buyerToken = buyer.getRight();
        buyerEmail = createBuyerModel.getAdminUser().getEmail();
    }

    @Test
    public void UpdateBuyer_UpdateAllEntriesAdminRoleBuyer_Success() {
        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.defaultUpdateBuyerModel().build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("tag", equalTo(updateBuyerModel.getTag()))
                .body("adminUser.id", notNullValue())
                .body("adminUser.name", equalTo(updateBuyerModel.getAdminUser().getName()))
                .body("adminUser.surname", equalTo(updateBuyerModel.getAdminUser().getSurname()))
                .body("adminUser.roles[0]", equalTo(Roles.ADMIN.name()))
                .body("adminUser.email", equalTo(buyerEmail))
                .body("adminUser.mobile.countryCode", equalTo(updateBuyerModel.getAdminUser().getMobile().getCountryCode()))
                .body("adminUser.mobile.number", equalTo(updateBuyerModel.getAdminUser().getMobile().getNumber()))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true))
                .body("adminUser.mobileNumberVerified", equalTo(false))
                .body("adminUser.dateOfBirth.year", equalTo(updateBuyerModel.getAdminUser().getDateOfBirth().getYear()))
                .body("adminUser.dateOfBirth.month", equalTo(updateBuyerModel.getAdminUser().getDateOfBirth().getMonth()))
                .body("adminUser.dateOfBirth.day", equalTo(updateBuyerModel.getAdminUser().getDateOfBirth().getDay()))
                .body("adminUser.companyPosition", notNullValue())
                .body("company.name", equalTo(createBuyerModel.getCompany().getName()))
                .body("company.type", equalTo(createBuyerModel.getCompany().getType()))
                .body("company.registrationNumber", equalTo(createBuyerModel.getCompany().getRegistrationNumber()))
                .body("company.businessAddress.addressLine1", equalTo(updateBuyerModel.getCompany().getBusinessAddress().getAddressLine1()))
                .body("company.businessAddress.addressLine2", equalTo(updateBuyerModel.getCompany().getBusinessAddress().getAddressLine2()))
                .body("company.businessAddress.city", equalTo(updateBuyerModel.getCompany().getBusinessAddress().getCity()))
                .body("company.businessAddress.postCode", equalTo(updateBuyerModel.getCompany().getBusinessAddress().getPostCode()))
                .body("company.businessAddress.state", equalTo(updateBuyerModel.getCompany().getBusinessAddress().getState()))
                .body("company.businessAddress.country", equalTo(updateBuyerModel.getCompany().getBusinessAddress().getCountry()))
                .body("company.countryOfRegistration", equalTo(createBuyerModel.getCompany().getCountryOfRegistration()))
                .body("company.incorporatedOn.year", nullValue())
                .body("company.incorporatedOn.month", nullValue())
                .body("company.incorporatedOn.day", nullValue())
                .body("acceptedTerms", equalTo(createBuyerModel.isAcceptedTerms()))
                .body("baseCurrency", equalTo(updateBuyerModel.getBaseCurrency()))
                .body("supportedCurrencies", equalTo(createBuyerModel.getSupportedCurrencies()))
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void UpdateBuyer_AssignMultipleRoles_Success() {
        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.allRolesUpdateBuyerModel().build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.roles", equalTo(List.of(Roles.ADMIN.name(), Roles.CONTROLLER.name(), Roles.CREATOR.name())));
    }

    @Test
    public void UpdateBuyer_RemoveRoles_Success() {
        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.allRolesUpdateBuyerModel().build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.roles", equalTo(List.of(Roles.ADMIN.name(), Roles.CONTROLLER.name(), Roles.CREATOR.name())));

        final UpdateBuyerModel removeRolesModel = UpdateBuyerModel.adminRolesUpdateBuyerModel().build();

        BuyersService.updateBuyer(removeRolesModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.roles", equalTo(List.of(Roles.ADMIN.name())));
    }

    @Test
    public void UpdateBuyer_EmptyRoles_BadRequest() {
        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .roles(Collections.singletonList(""))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser","roles","0")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("roles"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @Test
    public void UpdateBuyer_InvalidRole_BadRequest() {
        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.allRolesUpdateBuyerModel()
                .adminUser(AdminUserModel.builder()
                        .roles(Collections.singletonList("abc"))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser","roles","0")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("roles"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @Test
    public void UpdateBuyer_NoAdminRoleInRoles_BadRequest() {
        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.allRolesUpdateBuyerModel()
                .adminUser(AdminUserModel.builder()
                        .roles(List.of(Roles.CONTROLLER.name(), Roles.CREATOR.name()))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("BUYER_ROLES_ADMIN_REQUIRED"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("adminUser.roles"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @Test
    public void UpdateBuyer_UpdateDateOfBirthNotAddedDuringCreate_Success() {
        final CreateBuyerModel createBuyerModel = CreateBuyerModel.defaultCreateBuyerModel()
                .adminUser(AdminUserModel.defaultAdminUserModel()
                        .dateOfBirth(null)
                        .build())
                .build();

        final Pair<String, String> buyer = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.defaultUpdateBuyerModel().build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("tag", equalTo(updateBuyerModel.getTag()))
                .body("adminUser.id", notNullValue())
                .body("adminUser.name", equalTo(updateBuyerModel.getAdminUser().getName()))
                .body("adminUser.surname", equalTo(updateBuyerModel.getAdminUser().getSurname()))
                .body("adminUser.roles[0]", equalTo(Roles.ADMIN.name()))
                .body("adminUser.email", equalTo(createBuyerModel.getAdminUser().getEmail()))
                .body("adminUser.mobile.countryCode", equalTo(updateBuyerModel.getAdminUser().getMobile().getCountryCode()))
                .body("adminUser.mobile.number", equalTo(updateBuyerModel.getAdminUser().getMobile().getNumber()))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true))
                .body("adminUser.mobileNumberVerified", equalTo(false))
                .body("adminUser.dateOfBirth.year", equalTo(updateBuyerModel.getAdminUser().getDateOfBirth().getYear()))
                .body("adminUser.dateOfBirth.month", equalTo(updateBuyerModel.getAdminUser().getDateOfBirth().getMonth()))
                .body("adminUser.dateOfBirth.day", equalTo(updateBuyerModel.getAdminUser().getDateOfBirth().getDay()))
                .body("adminUser.companyPosition", notNullValue())
                .body("company.name", equalTo(createBuyerModel.getCompany().getName()))
                .body("company.type", equalTo(createBuyerModel.getCompany().getType()))
                .body("company.registrationNumber", equalTo(createBuyerModel.getCompany().getRegistrationNumber()))
                .body("company.businessAddress.addressLine1", equalTo(updateBuyerModel.getCompany().getBusinessAddress().getAddressLine1()))
                .body("company.businessAddress.addressLine2", equalTo(updateBuyerModel.getCompany().getBusinessAddress().getAddressLine2()))
                .body("company.businessAddress.city", equalTo(updateBuyerModel.getCompany().getBusinessAddress().getCity()))
                .body("company.businessAddress.postCode", equalTo(updateBuyerModel.getCompany().getBusinessAddress().getPostCode()))
                .body("company.businessAddress.state", equalTo(updateBuyerModel.getCompany().getBusinessAddress().getState()))
                .body("company.businessAddress.country", equalTo(updateBuyerModel.getCompany().getBusinessAddress().getCountry()))
                .body("company.countryOfRegistration", equalTo(createBuyerModel.getCompany().getCountryOfRegistration()))
                .body("company.incorporatedOn.year", nullValue())
                .body("company.incorporatedOn.month", nullValue())
                .body("company.incorporatedOn.day", nullValue())
                .body("acceptedTerms", equalTo(createBuyerModel.isAcceptedTerms()))
                .body("baseCurrency", equalTo(updateBuyerModel.getBaseCurrency()))
                .body("supportedCurrencies", equalTo(createBuyerModel.getSupportedCurrencies()))
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void UpdateBuyer_SameEmail_NoChanges() {
        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .email(buyerEmail)
                        .build())
                .build();

        BuyersHelper.verifyEmail(updateBuyerModel.getAdminUser().getEmail(), secretKey);

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.email", equalTo(buyerEmail))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true));
    }

    @Test
    public void UpdateBuyer_OldEmailVerified_Success() {
        BuyersHelper.verifyEmail(createBuyerModel.getAdminUser().getEmail(), secretKey);

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.name", equalTo(createBuyerModel.getAdminUser().getName()))
                .body("adminUser.surname", equalTo(createBuyerModel.getAdminUser().getSurname()))
                .body("adminUser.email", equalTo(buyerEmail))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true))
                .body("adminUser.mobileNumberVerified", equalTo(false));
    }

    @Test
    public void UpdateBuyer_NewEmailVerified_Success() {
        BuyersHelper.verifyEmail(buyerEmail, secretKey);

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.email", equalTo(buyerEmail))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true))
                .body("adminUser.mobileNumberVerified", equalTo(false));

        BuyersService.verifyEmail(new EmailVerificationModel(updateBuyerModel.getAdminUser().getEmail(), TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        BuyersService.getBuyer(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.name", equalTo(createBuyerModel.getAdminUser().getName()))
                .body("adminUser.surname", equalTo(createBuyerModel.getAdminUser().getSurname()))
                .body("adminUser.email", equalTo(updateBuyerModel.getAdminUser().getEmail()))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true));
    }

    @Test
    public void UpdateBuyer_ChangeEmailTwiceWithinBlockingTime_Conflict() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.email", equalTo(createBuyerModel.getAdminUser().getEmail()))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true));

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TOO_FREQUENT_EMAIL_CHANGES"));
    }

    @Test
    public void UpdateBuyer_ChangeEmailTwiceAfterBlockingTime_Success() throws InterruptedException {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.email", equalTo(createBuyerModel.getAdminUser().getEmail()))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true));

        //on QA blocking time should be 5s
        TimeUnit.SECONDS.sleep(emailChangeBlockingLimit);

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.email", equalTo(createBuyerModel.getAdminUser().getEmail()))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true));
    }

    @Test
    public void UpdateBuyer_ChangeEmailTwiceAfterBlockingTimeFirstNewEmailVerified_Success() throws InterruptedException {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.email", equalTo(buyerEmail))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true));

        BuyersService.verifyEmail(new EmailVerificationModel(updateBuyerModel.getAdminUser().getEmail(), TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        //on QA blocking time should be 5s
        TimeUnit.SECONDS.sleep(emailChangeBlockingLimit);

        final UpdateBuyerModel updateBuyerModelSecondUpdate = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModelSecondUpdate, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.email", equalTo(updateBuyerModel.getAdminUser().getEmail()))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true));
    }

    @Test
    public void UpdateBuyer_ChangeEmailTwiceInBlockingTimeFirstNewEmailVerified_Conflict() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.email", equalTo(buyerEmail))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true));

        BuyersService.verifyEmail(new EmailVerificationModel(updateBuyerModel.getAdminUser().getEmail(), TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final UpdateBuyerModel updateBuyerModelSecondUpdate = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModelSecondUpdate, secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TOO_FREQUENT_EMAIL_CHANGES"));
    }

    @Test
    public void UpdateBuyer_ChangeAnyFieldTwiceWithinBlockingTime_Success() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .name("Test")
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.name", equalTo(updateBuyerModel.getAdminUser().getName()));

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.name", equalTo(updateBuyerModel.getAdminUser().getName()));
    }

    @Test
    public void UpdateBuyer_ChangeAnyFieldAfterEmailChangedWithinBlockingTime_Success() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.email", equalTo(buyerEmail))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true));

        final UpdateBuyerModel updateBuyerModelSecondUpdate = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .name("Test")
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModelSecondUpdate, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.name", equalTo(updateBuyerModelSecondUpdate.getAdminUser().getName()));
    }

    @Test
    public void UpdateBuyer_RequiredOnlyInAddress_Success() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .company(UpdateCompanyModel.builder().businessAddress(AddressModel.DefaultAddressModel()
                                .setAddressLine2(null)
                                .setState(null)
                                .build())
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("company.name", equalTo(createBuyerModel.getCompany().getName()))
                .body("company.type", equalTo(createBuyerModel.getCompany().getType()))
                .body("company.registrationNumber", equalTo(createBuyerModel.getCompany().getRegistrationNumber()))
                .body("company.businessAddress.addressLine1", equalTo(updateBuyerModel.getCompany().getBusinessAddress().getAddressLine1()))
                .body("company.businessAddress.addressLine2", nullValue())
                .body("company.businessAddress.city", equalTo(updateBuyerModel.getCompany().getBusinessAddress().getCity()))
                .body("company.businessAddress.postCode", equalTo(updateBuyerModel.getCompany().getBusinessAddress().getPostCode()))
                .body("company.businessAddress.state", nullValue())
                .body("company.businessAddress.country", equalTo(updateBuyerModel.getCompany().getBusinessAddress().getCountry()))
                .body("company.countryOfRegistration", equalTo(createBuyerModel.getCompany().getCountryOfRegistration()));
    }

    @Test
    public void UpdateBuyer_SetAllUserDetailsNotAffectedByKybVerification_Success() {

        BuyersHelper.verifyKyb(secretKey, buyerId);

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .email(null)
                        .mobile(null)
                        .dateOfBirth(null)
                        .name(null)
                        .surname(null)
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void UpdateBuyer_UpdateEmailKybVerified_AdminUserAlreadyVerified() {

        BuyersHelper.verifyKyb(secretKey, buyerId);

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ADMIN_USER_ALREADY_VERIFIED"));
    }

    @Test
    public void UpdateBuyer_UpdateNameKybVerified_AdminUserAlreadyVerified() {

        BuyersHelper.verifyKyb(secretKey, buyerId);

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .name(RandomStringUtils.randomAlphabetic(5))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ADMIN_USER_ALREADY_VERIFIED"));
    }

    @Test
    public void UpdateBuyer_UpdateSurnameKybVerified_AdminUserAlreadyVerified() {

        BuyersHelper.verifyKyb(secretKey, buyerId);

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .surname(RandomStringUtils.randomAlphabetic(5))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ADMIN_USER_ALREADY_VERIFIED"));
    }

    @Test
    public void UpdateBuyer_UpdateDateOfBirthKybVerified_AdminUserAlreadyVerified() {

        BuyersHelper.verifyKyb(secretKey, buyerId);

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .dateOfBirth(new DateOfBirthModel(1982, 1, 3))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ADMIN_USER_ALREADY_VERIFIED"));
    }

    @Test
    public void UpdateBuyer_UpdateMobileCountryCodeKybVerified_AdminUserAlreadyVerified() {

        BuyersHelper.verifyKyb(secretKey, buyerId);

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .mobile(new MobileNumberModel("+0044", createBuyerModel.getAdminUser().getMobile().getNumber()))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ADMIN_USER_ALREADY_VERIFIED"));
    }

    @Test
    public void UpdateBuyer_UpdateMobileNumberKybVerified_AdminUserAlreadyVerified() {

        BuyersHelper.verifyKyb(secretKey, buyerId);

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .mobile(new MobileNumberModel(createBuyerModel.getAdminUser().getMobile().getCountryCode(), RandomStringUtils.randomNumeric(8)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ADMIN_USER_ALREADY_VERIFIED"));
    }

    @Test
    public void UpdateBuyer_EmailAlreadyExists_EmailNotUnique() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();

        BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .email(createBuyerModel.getAdminUser().getEmail())
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_UNIQUE"));
    }

    @Test
    public void UpdateBuyer_NullCountryCode_BadRequest() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .mobile(new MobileNumberModel(null, RandomStringUtils.randomNumeric(8)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser","mobile")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("countryCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void UpdateBuyer_EmptyCountryCode_BadRequest() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .mobile(new MobileNumberModel("", RandomStringUtils.randomNumeric(8)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser","mobile")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("countryCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"))
                .body("syntaxErrors.invalidFields[1].params", equalTo(List.of("adminUser","mobile")))
                .body("syntaxErrors.invalidFields[1].fieldName", equalTo("countryCode"))
                .body("syntaxErrors.invalidFields[1].error", equalTo("SIZE"));
    }

    @Test
    public void UpdateBuyer_InvalidEmail_BadRequest() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .email(RandomStringUtils.randomAlphanumeric(6))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void UpdateBuyer_InvalidEmailFormat_BadRequest() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .email(String.format("%s.@weavrtest.io", RandomStringUtils.randomAlphanumeric(6)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void UpdateBuyer_InvalidMobileNumber_BadRequest() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .mobile(new MobileNumberModel("+356", "abc"))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser","mobile")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("number"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void UpdateBuyer_EmptyMobileNumber_BadRequest() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .mobile(new MobileNumberModel("+356", ""))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser","mobile")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("number"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"))
                .body("syntaxErrors.invalidFields[1].params", equalTo(List.of("adminUser","mobile")))
                .body("syntaxErrors.invalidFields[1].fieldName", equalTo("number"))
                .body("syntaxErrors.invalidFields[1].error", equalTo("SIZE"));
    }

    @Test
    public void UpdateBuyer_NullMobileNumber_BadRequest() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .mobile(new MobileNumberModel("+356", null))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser","mobile")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("number"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "123", "+", "49"})
    public void UpdateBuyer_InvalidMobileNumberCountryCode_BadRequest(final String mobileNumberCountryCode) {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .mobile(new MobileNumberModel(mobileNumberCountryCode, "123456"))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser","mobile")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("countryCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void UpdateBuyer_PatchInactiveUser_Unauthorized() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.email", equalTo(buyerEmail))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true));

        InnovatorHelper.deactivateCorporate(new DeactivateIdentityModel(false, "TEMPORARY"),
                buyerId, innovatorToken);

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void UpdateBuyer_AuthUserAuthentication_Forbidden() {
        final String buyerToken = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();
        final String userToken =
                BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, buyerToken).getRight();

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.defaultUpdateBuyerModel()
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, userToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void UpdateBuyer_InvalidApiKey_Unauthorized() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, "abc", buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void UpdateBuyer_EmptyApiKey_Unauthorised() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, "", buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void UpdateBuyer_NoApiKey_BadRequest() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyerNoApiKey(updateBuyerModel, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("api-key"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void UpdateBuyer_DifferentInnovatorApiKey_Unauthorised() {

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void UpdateBuyer_AdminUserLoggedOut_Unauthorised() {
        opc.services.multi.AuthenticationService.logout(secretKey, buyerToken);

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void UpdateBuyer_BackofficeImpersonator_Unauthorised() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, getBackofficeImpersonateToken(buyerId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void UpdateBuyer_InvalidDateOfBirthMonth_BadRequest() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .dateOfBirth(new DateOfBirthModel(2000, 13, 11))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser","dateOfBirth")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("month"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("AT_MOST"));
    }

    @Test
    public void UpdateBuyer_InvalidDateOfBirthDay_BadRequest() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .dateOfBirth(new DateOfBirthModel(2000, 12, 41))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser","dateOfBirth")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("day"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("AT_MOST"));
    }

    @Test
    public void UpdateBuyer_InvalidDateOfBirthYear_BadRequest() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .dateOfBirth(new DateOfBirthModel(2101, 12, 11))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser","dateOfBirth")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("year"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("AT_MOST"));
    }

    @Test
    public void UpdateBuyer_NullBusinessAddressAddressLine1_BadRequest() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .company(UpdateCompanyModel.builder()
                        .businessAddress(AddressModel.DefaultAddressModel()
                                .setAddressLine1(null)
                                .build())
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("company","businessAddress")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("addressLine1"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void UpdateBuyer_EmptyBusinessAddressAddressLine1_BadRequest() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .company(UpdateCompanyModel.builder()
                        .businessAddress(AddressModel.DefaultAddressModel()
                                .setAddressLine1("")
                                .build())
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Bad Request"));
    }

    @Test
    public void UpdateBuyer_NullBusinessAddressCity_BadRequest() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .company(UpdateCompanyModel.builder()
                        .businessAddress(AddressModel.DefaultAddressModel()
                                .setCity(null)
                                .build())
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("company","businessAddress")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("city"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void UpdateBuyer_EmptyBusinessAddressCity_BadRequest() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .company(UpdateCompanyModel.builder()
                        .businessAddress(AddressModel.DefaultAddressModel()
                                .setCity("")
                                .build())
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.companyBusinessAddress.city: must not be blank"));
    }

    @Test
    public void UpdateBuyer_NullBusinessAddressPostCode_BadRequest() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .company(UpdateCompanyModel.builder()
                        .businessAddress(AddressModel.DefaultAddressModel()
                                .setPostCode(null)
                                .build())
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("company","businessAddress")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("postCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void UpdateBuyer_EmptyBusinessAddressPostCode_BadRequest() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .company(UpdateCompanyModel.builder()
                        .businessAddress(AddressModel.DefaultAddressModel()
                                .setPostCode("")
                                .build())
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.companyBusinessAddress.postCode: must not be blank"));
    }

    @Test
    public void UpdateBuyer_InvalidBaseCurrency_BadRequest() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .baseCurrency("ABCD")
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("baseCurrency"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @Test
    public void UpdateBuyer_EmptyBaseCurrency_BadRequest() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .baseCurrency("")
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("baseCurrency"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @Test
    public void UpdateBuyer_UnknownCurrency_BadRequest() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .baseCurrency("ABC")
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("baseCurrency"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @Test
    public void UpdateBuyer_MobileNotUnique_Success() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();

        BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .mobile(createBuyerModel.getAdminUser().getMobile())
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.mobile.countryCode", equalTo(updateBuyerModel.getAdminUser().getMobile().getCountryCode()))
                .body("adminUser.mobile.number", equalTo(updateBuyerModel.getAdminUser().getMobile().getNumber()));
    }

    @Test
    public void UpdateBuyer_PatchMobileWithUniqueNumber_Success() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .mobile(MobileNumberModel.random())
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.mobile.countryCode", equalTo(updateBuyerModel.getAdminUser().getMobile().getCountryCode()))
                .body("adminUser.mobile.number", equalTo(updateBuyerModel.getAdminUser().getMobile().getNumber()));
    }

    @Test
    public void UpdateBuyer_PatchMobileWithNonUniqueNumberSameIdentityType_Success() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .mobile(MobileNumberModel.random())
                                .build())
                        .build();

        BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .mobile(createBuyerModel.getAdminUser().getMobile())
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.mobile.countryCode", equalTo(updateBuyerModel.getAdminUser().getMobile().getCountryCode()))
                .body("adminUser.mobile.number", equalTo(updateBuyerModel.getAdminUser().getMobile().getNumber()));
    }

    @Test
    public void UpdateBuyer_MobileChangeLimitUpdateMobileFieldNull_Success() {

        IntStream.range(0, mobileChangeLimit)
                .forEach(i -> {
                    final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                            .adminUser(AdminUserModel.builder()
                                    .mobile(null)
                                    .build())
                            .build();

                    BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                            .then()
                            .statusCode(SC_OK)
                            .body("adminUser.mobile.countryCode", equalTo(createBuyerModel.getAdminUser().getMobile().getCountryCode()))
                            .body("adminUser.mobile.number", equalTo(createBuyerModel.getAdminUser().getMobile().getNumber()));
                });

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .mobile(null)
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.mobile.countryCode", equalTo(createBuyerModel.getAdminUser().getMobile().getCountryCode()))
                .body("adminUser.mobile.number", equalTo(createBuyerModel.getAdminUser().getMobile().getNumber()));

        final UpdateBuyerModel updateBuyerModelSecond = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .mobile(MobileNumberModel.random())
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModelSecond, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.mobile.countryCode", equalTo(updateBuyerModelSecond.getAdminUser().getMobile().getCountryCode()))
                .body("adminUser.mobile.number", equalTo(updateBuyerModelSecond.getAdminUser().getMobile().getNumber()));
    }

    @Test
    public void UpdateBuyer_MobileChangeLimitByUpdateNewNumber_LimitExceeded() {

        IntStream.range(0, mobileChangeLimit)
                .forEach(i -> {
                    final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                            .adminUser(AdminUserModel.builder()
                                    .mobile(MobileNumberModel.random())
                                    .build())
                            .build();

                    BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                            .then()
                            .statusCode(SC_OK)
                            .body("adminUser.mobile.countryCode", equalTo(updateBuyerModel.getAdminUser().getMobile().getCountryCode()))
                            .body("adminUser.mobile.number", equalTo(updateBuyerModel.getAdminUser().getMobile().getNumber()));
                });

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .mobile(MobileNumberModel.random())
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MOBILE_NO_CHANGE_LIMIT_EXCEEDED"));
    }

    @Test
    public void UpdateBuyer_UpdateMobileMultipleTimesWithItsOwnNumber_Success() {

        IntStream.range(0, 2)
                .forEach(i -> {
                    final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                            .adminUser(AdminUserModel.builder()
                                    .mobile(createBuyerModel.getAdminUser().getMobile())
                                    .build())
                            .build();

                    BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                            .then()
                            .statusCode(SC_OK)
                            .body("adminUser.mobile.countryCode", equalTo(updateBuyerModel.getAdminUser().getMobile().getCountryCode()))
                            .body("adminUser.mobile.number", equalTo(updateBuyerModel.getAdminUser().getMobile().getNumber()));
                });

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .mobile(MobileNumberModel.random())
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.mobile.countryCode", equalTo(updateBuyerModel.getAdminUser().getMobile().getCountryCode()))
                .body("adminUser.mobile.number", equalTo(updateBuyerModel.getAdminUser().getMobile().getNumber()));
    }

    @Test
    public void UpdateBuyer_UpdateAnotherFieldAfterMobileChangeLimitExceeded_Success() {

        IntStream.range(0, mobileChangeLimit)
                .forEach(i -> {
                    final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                            .adminUser(AdminUserModel.builder()
                                    .mobile(MobileNumberModel.random())
                                    .build())
                            .build();

                    BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                            .then()
                            .statusCode(SC_OK)
                            .body("adminUser.mobile.countryCode", equalTo(updateBuyerModel.getAdminUser().getMobile().getCountryCode()))
                            .body("adminUser.mobile.number", equalTo(updateBuyerModel.getAdminUser().getMobile().getNumber()));
                });

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .mobile(MobileNumberModel.random())
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MOBILE_NO_CHANGE_LIMIT_EXCEEDED"));

        final UpdateBuyerModel updateBuyerModelSecond = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .name(RandomStringUtils.randomAlphabetic(5))
                        .surname(RandomStringUtils.randomAlphabetic(5))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModelSecond, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.name", equalTo(updateBuyerModelSecond.getAdminUser().getName()))
                .body("adminUser.surname", equalTo(updateBuyerModelSecond.getAdminUser().getSurname()));
    }

    @Test
    public void UpdateBuyer_MobileChangeLimitByUpdateNonUniqueNumber_LimitExceeded() {

        IntStream.range(0, mobileChangeLimit)
                .forEach(i -> {
                    final CreateBuyerModel createBuyerModel =
                            CreateBuyerModel.defaultCreateBuyerModel()
                                    .adminUser(AdminUserModel.defaultAdminUserModel()
                                            .mobile(MobileNumberModel.random())
                                            .build())
                                    .build();

                    BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);

                    final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                            .adminUser(AdminUserModel.builder()
                                    .mobile(createBuyerModel.getAdminUser().getMobile())
                                    .build())
                            .build();

                    BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                            .then()
                            .statusCode(SC_OK)
                            .body("adminUser.mobile.countryCode", equalTo(updateBuyerModel.getAdminUser().getMobile().getCountryCode()))
                            .body("adminUser.mobile.number", equalTo(updateBuyerModel.getAdminUser().getMobile().getNumber()));
                });

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .mobile(MobileNumberModel.random())
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MOBILE_NO_CHANGE_LIMIT_EXCEEDED"));
    }

    @Test
    public void UpdateBuyer_SeveralUsersSameNumberPatchMobileNotUniqueNumber_Success() {
        final String mobileNumber = RandomStringUtils.randomNumeric(6);

        final CreateBuyerModel firstBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .mobile(new MobileNumberModel("+356", String.format("79%s", mobileNumber)))
                                .build())
                        .build();
        BuyersHelper.createAuthenticatedBuyer(firstBuyerModel, secretKey);

        final CreateBuyerModel secondBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .mobile(new MobileNumberModel("+356", String.format("79%s", mobileNumber)))
                                .build())
                        .build();
        BuyersHelper.createAuthenticatedBuyer(secondBuyerModel, secretKey);

        final CreateBuyerModel thirdBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .mobile(new MobileNumberModel("+356", String.format("79%s", RandomStringUtils.randomNumeric(6))))
                                .build())
                        .build();
        final Pair<String, String> thirdBuyer = BuyersHelper.createAuthenticatedBuyer(thirdBuyerModel, secretKey);

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .mobile(new MobileNumberModel("+356", String.format("79%s", mobileNumber)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, thirdBuyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("adminUser.mobile.countryCode", equalTo(updateBuyerModel.getAdminUser().getMobile().getCountryCode()))
                .body("adminUser.mobile.number", equalTo(updateBuyerModel.getAdminUser().getMobile().getNumber()));
    }

    @Test
    public void UpdateBuyer_CheckMobileChangeSmsOldNumberVerified_Success() {
        AuthenticationHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey,
                buyerToken);

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .mobile(MobileNumberModel.random())
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.mobile.countryCode", equalTo(updateBuyerModel.getAdminUser().getMobile().getCountryCode()))
                .body("adminUser.mobile.number", equalTo(updateBuyerModel.getAdminUser().getMobile().getNumber()));

        AuthenticationHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey,
                buyerToken);

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(createBuyerModel.getAdminUser().getMobile().getNumber());
        assertEquals(String.format(MailHogSms.SCA_CHANGE_SMS.getSmsText(),
                        StringUtils.right(createBuyerModel.getAdminUser().getMobile().getNumber(), 4),
                        StringUtils.right(updateBuyerModel.getAdminUser().getMobile().getNumber(), 4)),
                sms.getBody());
    }

    @Test
    public void UpdateBuyer_CheckMobileChangeSmsOldNumberNotVerified_NoSms() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .mobile(MobileNumberModel.random())
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.mobile.countryCode", equalTo(updateBuyerModel.getAdminUser().getMobile().getCountryCode()))
                .body("adminUser.mobile.number", equalTo(updateBuyerModel.getAdminUser().getMobile().getNumber()));

        AuthenticationHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey,
                buyerToken);

        MailhogService.getMailHogSms(createBuyerModel.getAdminUser().getMobile().getNumber())
                .then()
                .statusCode(200)
                .body("items[0]", nullValue());
    }

    @Test
    public void UpdateBuyer_EmailChecksNewEmailNotValidated_Success() {
        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        //if new email is not validated - old email remains active and returns in response
        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.email", equalTo(buyerEmail));
    }

    @Test
    public void UpdateBuyer_NewEmailVerifiedAndUpdateMobileNumberSecurityRule24H_Success() throws InterruptedException {

        BuyersHelper.verifyEmail(buyerEmail, secretKey);

        final UpdateBuyerModel updateBuyerModelChangeEmail = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModelChangeEmail, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.email", equalTo(buyerEmail))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true))
                .body("adminUser.mobileNumberVerified", equalTo(false));

        BuyersService.verifyEmail(new EmailVerificationModel(updateBuyerModelChangeEmail.getAdminUser().getEmail(), TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        BuyersService.getBuyer(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.name", equalTo(createBuyerModel.getAdminUser().getName()))
                .body("adminUser.surname", equalTo(createBuyerModel.getAdminUser().getSurname()))
                .body("adminUser.email", equalTo(updateBuyerModelChangeEmail.getAdminUser().getEmail()))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true));

        //Set blocking time to check the new functionality
        TimeUnit.SECONDS.sleep(mobileChangeBlockingLimit);

        final UpdateBuyerModel updateBuyerModelChangeMobile =
                UpdateBuyerModel.builder()
                        .adminUser(AdminUserModel.builder()
                                .mobile(MobileNumberModel.random())
                                .build())
                        .build();

        BuyersService.updateBuyer(updateBuyerModelChangeMobile, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.mobile.countryCode", equalTo(updateBuyerModelChangeMobile.getAdminUser().getMobile().getCountryCode()))
                .body("adminUser.mobile.number", equalTo(updateBuyerModelChangeMobile.getAdminUser().getMobile().getNumber()));

        BuyersService.getBuyer(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.name", equalTo(createBuyerModel.getAdminUser().getName()))
                .body("adminUser.surname", equalTo(createBuyerModel.getAdminUser().getSurname()))
                .body("adminUser.email", equalTo(updateBuyerModelChangeEmail.getAdminUser().getEmail()))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true))
                .body("adminUser.mobile.countryCode", equalTo(updateBuyerModelChangeMobile.getAdminUser().getMobile().getCountryCode()))
                .body("adminUser.mobile.number", equalTo(updateBuyerModelChangeMobile.getAdminUser().getMobile().getNumber()));
    }

    @Test
    public void UpdateBuyer_NewEmailNotVerifiedAndUpdateMobileNumberSecurityRule24H_Success() {

        BuyersHelper.verifyEmail(buyerEmail, secretKey);

        final UpdateBuyerModel updateBuyerModelChangeEmail = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModelChangeEmail, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.email", equalTo(buyerEmail))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true))
                .body("adminUser.mobileNumberVerified", equalTo(false));

        BuyersService.getBuyer(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.name", equalTo(createBuyerModel.getAdminUser().getName()))
                .body("adminUser.surname", equalTo(createBuyerModel.getAdminUser().getSurname()))
                .body("adminUser.email", equalTo(buyerEmail))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true));

        //Blocking time less than mobileChangeBlockingLimit to check the response, should be 200 once new email was not verified

        final UpdateBuyerModel updateBuyerModelChangeMobile = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .mobile(MobileNumberModel.random())
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModelChangeMobile, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.mobile.countryCode", equalTo(updateBuyerModelChangeMobile.getAdminUser().getMobile().getCountryCode()))
                .body("adminUser.mobile.number", equalTo(updateBuyerModelChangeMobile.getAdminUser().getMobile().getNumber()));

        BuyersService.getBuyer(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.name", equalTo(createBuyerModel.getAdminUser().getName()))
                .body("adminUser.surname", equalTo(createBuyerModel.getAdminUser().getSurname()))
                .body("adminUser.email", equalTo(buyerEmail))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true))
                .body("adminUser.mobile.countryCode", equalTo(updateBuyerModelChangeMobile.getAdminUser().getMobile().getCountryCode()))
                .body("adminUser.mobile.number", equalTo(updateBuyerModelChangeMobile.getAdminUser().getMobile().getNumber()));
    }

    @Test
    public void UpdateBuyer_NewEmailVerifiedAndUpdateMobileNumberSecurityRule24H_Conflict() {

        BuyersHelper.verifyEmail(buyerEmail, secretKey);

        final UpdateBuyerModel updateBuyerModelChangeEmail = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModelChangeEmail, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.email", equalTo(buyerEmail))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true))
                .body("adminUser.mobileNumberVerified", equalTo(false));

        BuyersService.verifyEmail(new EmailVerificationModel(updateBuyerModelChangeEmail.getAdminUser().getEmail(), TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        BuyersService.getBuyer(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.name", equalTo(createBuyerModel.getAdminUser().getName()))
                .body("adminUser.surname", equalTo(createBuyerModel.getAdminUser().getSurname()))
                .body("adminUser.email", equalTo(updateBuyerModelChangeEmail.getAdminUser().getEmail()))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true));

        AuthenticationHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, buyerToken);

        //Blocking time less than mobileChangeBlockingLimit, once new email was verified have to get error 409

        final UpdateBuyerModel updateBuyerModelChangeMobile =
                UpdateBuyerModel.builder()
                        .adminUser(AdminUserModel.builder()
                                .mobile(MobileNumberModel.random())
                                .build())
                        .build();

        BuyersService.updateBuyer(updateBuyerModelChangeMobile, secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MOBILE_CHANGE_NOT_ALLOWED"));

        BuyersService.getBuyer(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.name", equalTo(createBuyerModel.getAdminUser().getName()))
                .body("adminUser.surname", equalTo(createBuyerModel.getAdminUser().getSurname()))
                .body("adminUser.email", equalTo(updateBuyerModelChangeEmail.getAdminUser().getEmail()))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true))
                .body("adminUser.mobile.countryCode", equalTo(createBuyerModel.getAdminUser().getMobile().getCountryCode()))
                .body("adminUser.mobile.number", equalTo(createBuyerModel.getAdminUser().getMobile().getNumber()));
    }

    @Test
    public void UpdateBuyer_ChangeEmailWithNotAllowedDomain_Conflict() {
        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .email(String.format("%s@gav0.com", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_DOMAIN_NOT_ALLOWED"));
    }

    @ParameterizedTest
    @MethodSource("emailProvider")
    public void UpdateBuyer_ChangeEmailHavingApostropheOrSingleQuotes_Success(final String email) {
        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .email(email)
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void UpdateBuyer_PatchUsingInnovatorAuthentication_Unauthorised() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, innovatorToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void UpdateBuyer_PatchUsingAdminAuthentication_Unauthorised() {

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .email(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build())
                .build();

        BuyersService.updateBuyer(updateBuyerModel, secretKey, adminToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    private static Stream<Arguments> emailProvider() {
        return Stream.of(
                arguments(String.format("%s's@weavrtest.io", RandomStringUtils.randomAlphabetic(5))),
                arguments(String.format("'%s'@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
        );
    }
}
