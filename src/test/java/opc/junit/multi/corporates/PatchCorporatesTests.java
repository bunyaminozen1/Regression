package opc.junit.multi.corporates;

import commons.models.DateOfBirthModel;
import commons.models.MobileNumberModel;
import io.restassured.response.Response;
import opc.enums.mailhog.MailHogSms;
import opc.enums.opc.CorporateSourceOfFunds;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.Industry;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.innovator.DeactivateIdentityModel;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.multi.consumers.ConsumerRootUserModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.corporates.PatchCorporateModel;
import opc.models.multi.passwords.LostPasswordResumeModel;
import opc.models.multi.passwords.LostPasswordStartModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.AddressModel;
import opc.models.shared.EmailVerificationModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import opc.services.mailhog.MailhogService;
import opc.services.multi.AuthenticationService;
import opc.services.multi.CorporatesService;
import opc.services.multi.PasswordsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_REQUEST_TOO_LONG;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class PatchCorporatesTests extends BaseCorporatesSetup {

    private String authenticationToken;
    private CreateCorporateModel createCorporateModel;
    private String corporateId;
    private String corporateEmail;
    final private static int mobileChangeLimit = 3;
    final private static int emailChangeBlockingLimit = 10;
    final private static int mobileChangeBlockingLimit = 10;

    private final static String VERIFICATION_CODE = "123456";

    @BeforeEach
    public void Setup() {
        createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateEmail = createCorporateModel.getRootUser().getEmail();
        authenticationToken = authenticatedCorporate.getRight();
    }

    @Test
    public void PatchCorporates_EmailChecksNewEmailNotValidated_Success() {
        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        //if new email is not validated - old email remains active and returns in response
        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id.type", equalTo("CORPORATE"))
                .body("id.id", notNullValue())
                .body("profileId", equalTo(corporateProfileId))
                .body("tag", equalTo(createCorporateModel.getTag()))
                .body("rootUser.id.type", equalTo("CORPORATE"))
                .body("rootUser.id.id", notNullValue())
                .body("rootUser.name", equalTo(createCorporateModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(corporateEmail))
                .body("rootUser.mobile.countryCode", equalTo(createCorporateModel.getRootUser().getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(createCorporateModel.getRootUser().getMobile().getNumber()))
                .body("rootUser.companyPosition", equalTo(createCorporateModel.getRootUser().getCompanyPosition().name()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobileNumberVerified", equalTo(false))
                .body("rootUser.dateOfBirth.year", equalTo(createCorporateModel.getRootUser().getDateOfBirth().getYear()))
                .body("rootUser.dateOfBirth.month", equalTo(createCorporateModel.getRootUser().getDateOfBirth().getMonth()))
                .body("rootUser.dateOfBirth.day", equalTo(createCorporateModel.getRootUser().getDateOfBirth().getDay()))
                .body("company.name", equalTo(createCorporateModel.getCompany().getName()))
                .body("company.type", equalTo(createCorporateModel.getCompany().getType()))
                .body("company.registrationNumber", equalTo(createCorporateModel.getCompany().getRegistrationNumber()))
                .body("company.businessAddress.addressLine1", equalTo(createCorporateModel.getCompany().getBusinessAddress().getAddressLine1()))
                .body("company.businessAddress.addressLine2", equalTo(createCorporateModel.getCompany().getBusinessAddress().getAddressLine2()))
                .body("company.businessAddress.city", equalTo(createCorporateModel.getCompany().getBusinessAddress().getCity()))
                .body("company.businessAddress.postCode", equalTo(createCorporateModel.getCompany().getBusinessAddress().getPostCode()))
                .body("company.businessAddress.state", equalTo(createCorporateModel.getCompany().getBusinessAddress().getState()))
                .body("company.businessAddress.country", equalTo(createCorporateModel.getCompany().getBusinessAddress().getCountry()))
                .body("company.countryOfRegistration", equalTo(createCorporateModel.getCompany().getRegistrationCountry()))
                .body("industry", equalTo(createCorporateModel.getIndustry().toString()))
                .body("sourceOfFunds", equalTo(createCorporateModel.getSourceOfFunds().toString()))
                .body("sourceOfFundsOther", equalTo(createCorporateModel.getSourceOfFunds() == CorporateSourceOfFunds.OTHER ? createCorporateModel.getSourceOfFundsOther() : null))
                .body("acceptedTerms", equalTo(createCorporateModel.getAcceptedTerms()))
                .body("ipAddress", equalTo(createCorporateModel.getIpAddress()))
                .body("baseCurrency", equalTo(createCorporateModel.getBaseCurrency()))
                .body("feeGroup", equalTo("DEFAULT"))
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void PatchCorporates_PatchAllEntries_Success() {
        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.DefaultPatchCorporateModel()
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id.type", equalTo("CORPORATE"))
                .body("id.id", notNullValue())
                .body("profileId", equalTo(corporateProfileId))
                .body("tag", equalTo(patchCorporateModel.getTag()))
                .body("rootUser.id.type", equalTo("CORPORATE"))
                .body("rootUser.id.id", notNullValue())
                .body("rootUser.name", equalTo(patchCorporateModel.getName()))
                .body("rootUser.surname", equalTo(patchCorporateModel.getSurname()))
                .body("rootUser.email", equalTo(corporateEmail))
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateModel.getMobile().getNumber()))
                .body("rootUser.companyPosition", equalTo(createCorporateModel.getRootUser().getCompanyPosition().name()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobileNumberVerified", equalTo(false))
                .body("rootUser.dateOfBirth.year", equalTo(patchCorporateModel.getDateOfBirth().getYear()))
                .body("rootUser.dateOfBirth.month", equalTo(patchCorporateModel.getDateOfBirth().getMonth()))
                .body("rootUser.dateOfBirth.day", equalTo(patchCorporateModel.getDateOfBirth().getDay()))
                .body("company.name", equalTo(createCorporateModel.getCompany().getName()))
                .body("company.type", equalTo(createCorporateModel.getCompany().getType()))
                .body("company.registrationNumber", equalTo(createCorporateModel.getCompany().getRegistrationNumber()))
                .body("company.businessAddress.addressLine1", equalTo(patchCorporateModel.getCompanyBusinessAddress().getAddressLine1()))
                .body("company.businessAddress.addressLine2", equalTo(patchCorporateModel.getCompanyBusinessAddress().getAddressLine2()))
                .body("company.businessAddress.city", equalTo(patchCorporateModel.getCompanyBusinessAddress().getCity()))
                .body("company.businessAddress.postCode", equalTo(patchCorporateModel.getCompanyBusinessAddress().getPostCode()))
                .body("company.businessAddress.state", equalTo(patchCorporateModel.getCompanyBusinessAddress().getState()))
                .body("company.businessAddress.country", equalTo(patchCorporateModel.getCompanyBusinessAddress().getCountry()))
                .body("company.countryOfRegistration", equalTo(createCorporateModel.getCompany().getRegistrationCountry()))
                .body("industry", equalTo(createCorporateModel.getIndustry().toString()))
                .body("sourceOfFunds", equalTo(createCorporateModel.getSourceOfFunds().toString()))
                .body("sourceOfFundsOther", equalTo(createCorporateModel.getSourceOfFunds() == CorporateSourceOfFunds.OTHER ? createCorporateModel.getSourceOfFundsOther() : null))
                .body("acceptedTerms", equalTo(createCorporateModel.getAcceptedTerms()))
                .body("ipAddress", equalTo(createCorporateModel.getIpAddress()))
                .body("baseCurrency", equalTo(patchCorporateModel.getBaseCurrency()))
                .body("feeGroup", equalTo("DEFAULT"))
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void PatchCorporates_PatchDateOfBirthNotAddedDuringCreate_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setDateOfBirth(null)
                                .build())
                        .build();

        final Pair<String, String> authenticatedCorporate =
                CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.DefaultPatchCorporateModel()
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticatedCorporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id.type", equalTo("CORPORATE"))
                .body("id.id", notNullValue())
                .body("profileId", equalTo(corporateProfileId))
                .body("tag", equalTo(patchCorporateModel.getTag()))
                .body("rootUser.id.type", equalTo("CORPORATE"))
                .body("rootUser.id.id", notNullValue())
                .body("rootUser.name", equalTo(patchCorporateModel.getName()))
                .body("rootUser.surname", equalTo(patchCorporateModel.getSurname()))
                .body("rootUser.email", equalTo(createCorporateModel.getRootUser().getEmail()))
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateModel.getMobile().getNumber()))
                .body("rootUser.companyPosition", equalTo(createCorporateModel.getRootUser().getCompanyPosition().name()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobileNumberVerified", equalTo(false))
                .body("rootUser.dateOfBirth.year", equalTo(patchCorporateModel.getDateOfBirth().getYear()))
                .body("rootUser.dateOfBirth.month", equalTo(patchCorporateModel.getDateOfBirth().getMonth()))
                .body("rootUser.dateOfBirth.day", equalTo(patchCorporateModel.getDateOfBirth().getDay()))
                .body("company.name", equalTo(createCorporateModel.getCompany().getName()))
                .body("company.type", equalTo(createCorporateModel.getCompany().getType()))
                .body("company.registrationNumber", equalTo(createCorporateModel.getCompany().getRegistrationNumber()))
                .body("company.businessAddress.addressLine1", equalTo(patchCorporateModel.getCompanyBusinessAddress().getAddressLine1()))
                .body("company.businessAddress.addressLine2", equalTo(patchCorporateModel.getCompanyBusinessAddress().getAddressLine2()))
                .body("company.businessAddress.city", equalTo(patchCorporateModel.getCompanyBusinessAddress().getCity()))
                .body("company.businessAddress.postCode", equalTo(patchCorporateModel.getCompanyBusinessAddress().getPostCode()))
                .body("company.businessAddress.state", equalTo(patchCorporateModel.getCompanyBusinessAddress().getState()))
                .body("company.businessAddress.country", equalTo(patchCorporateModel.getCompanyBusinessAddress().getCountry()))
                .body("company.countryOfRegistration", equalTo(createCorporateModel.getCompany().getRegistrationCountry()))
                .body("industry", equalTo(createCorporateModel.getIndustry().toString()))
                .body("sourceOfFunds", equalTo(createCorporateModel.getSourceOfFunds().toString()))
                .body("sourceOfFundsOther", equalTo(createCorporateModel.getSourceOfFunds() == CorporateSourceOfFunds.OTHER ? createCorporateModel.getSourceOfFundsOther() : null))
                .body("acceptedTerms", equalTo(createCorporateModel.getAcceptedTerms()))
                .body("ipAddress", equalTo(createCorporateModel.getIpAddress()))
                .body("baseCurrency", equalTo(patchCorporateModel.getBaseCurrency()))
                .body("feeGroup", equalTo("DEFAULT"))
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void PatchCorporates_SameEmail_NoChanges() {
        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(createCorporateModel.getRootUser().getEmail())
                        .build();

        CorporatesHelper.verifyEmail(patchCorporateModel.getEmail(), secretKey);

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK);

        CorporatesService.getCorporates(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("id.type", equalTo(IdentityType.CORPORATE.name()))
                .body("id.id", equalTo(corporateId))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.active", equalTo(true));
    }

    @Test
    public void PatchCorporates_OldEmailVerified_Success() {

        CorporatesHelper.verifyEmail(corporateEmail, secretKey);

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(corporateEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobileNumberVerified", equalTo(false));
    }

    @Test
    public void PatchCorporates_NewEmailVerified_Success() {

        CorporatesHelper.verifyEmail(corporateEmail, secretKey);

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(corporateEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobileNumberVerified", equalTo(false));

        CorporatesService.verifyEmail(new EmailVerificationModel(patchCorporateModel.getEmail(), TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        CorporatesService.getCorporates(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createCorporateModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(patchCorporateModel.getEmail()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));
    }

    @Test
    public void PatchCorporates_ChangeEmailTwiceWithinBlockingTime_Conflict() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(corporateEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TOO_FREQUENT_EMAIL_CHANGES"));
    }

    @Test
    public void PatchCorporates_ChangeEmailTwiceAfterBlockingTime_Success() throws InterruptedException {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(corporateEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        //on QA blocking time should be 5s
        TimeUnit.SECONDS.sleep(emailChangeBlockingLimit);
        final PatchCorporateModel patchCorporateModelSecondUpdate =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModelSecondUpdate, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(corporateEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));
    }

    @Test
    public void PatchCorporates_ChangeEmailTwiceAfterBlockingTimeFirstNewEmailVerified_Success() throws InterruptedException {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(corporateEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        CorporatesService.verifyEmail(new EmailVerificationModel(patchCorporateModel.getEmail(), TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        //on QA blocking time should be 5s
        TimeUnit.SECONDS.sleep(emailChangeBlockingLimit);
        final PatchCorporateModel patchCorporateModelSecondUpdate =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModelSecondUpdate, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(patchCorporateModel.getEmail()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));
    }

    @Test
    public void PatchCorporates_ChangeEmailTwiceInBlockingTimeFirstNewEmailVerified_Conflict() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(corporateEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        CorporatesService.verifyEmail(new EmailVerificationModel(patchCorporateModel.getEmail(), TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        //on QA blocking time should be 5s
        final PatchCorporateModel patchCorporateModelSecondUpdate =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModelSecondUpdate, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TOO_FREQUENT_EMAIL_CHANGES"));
    }

    @Test
    public void PatchCorporates_ChangeAnyFieldTwiceWithinBlockingTime_Success() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setName("Test")
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(patchCorporateModel.getName()));

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(patchCorporateModel.getName()));
    }

    @Test
    public void PatchCorporates_ChangeAnyFieldAfterEmailChangedWithinBlockingTime_Success() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(corporateEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        final PatchCorporateModel patchCorporateModelSecond =
                PatchCorporateModel.newBuilder()
                        .setName("Test")
                        .build();

        CorporatesService.patchCorporate(patchCorporateModelSecond, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(patchCorporateModelSecond.getName()));
    }

    @Test
    public void PatchCorporates_RequiredOnlyInAddress_Success() {
        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setCompanyBusinessAddress(AddressModel.DefaultAddressModel()
                                .setAddressLine2(null)
                                .setState(null)
                                .build())
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id.type", equalTo("CORPORATE"))
                .body("id.id", notNullValue())
                .body("profileId", equalTo(corporateProfileId))
                .body("tag", equalTo(createCorporateModel.getTag()))
                .body("rootUser.id.type", equalTo("CORPORATE"))
                .body("rootUser.id.id", notNullValue())
                .body("rootUser.name", equalTo(createCorporateModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(corporateEmail))
                .body("rootUser.mobile.countryCode", equalTo(createCorporateModel.getRootUser().getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(createCorporateModel.getRootUser().getMobile().getNumber()))
                .body("rootUser.companyPosition", equalTo(createCorporateModel.getRootUser().getCompanyPosition().name()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobileNumberVerified", equalTo(false))
                .body("rootUser.dateOfBirth.year", equalTo(createCorporateModel.getRootUser().getDateOfBirth().getYear()))
                .body("rootUser.dateOfBirth.month", equalTo(createCorporateModel.getRootUser().getDateOfBirth().getMonth()))
                .body("rootUser.dateOfBirth.day", equalTo(createCorporateModel.getRootUser().getDateOfBirth().getDay()))
                .body("company.name", equalTo(createCorporateModel.getCompany().getName()))
                .body("company.type", equalTo(createCorporateModel.getCompany().getType()))
                .body("company.registrationNumber", equalTo(createCorporateModel.getCompany().getRegistrationNumber()))
                .body("company.businessAddress.addressLine1", equalTo(patchCorporateModel.getCompanyBusinessAddress().getAddressLine1()))
                .body("company.businessAddress.addressLine2", nullValue())
                .body("company.businessAddress.city", equalTo(patchCorporateModel.getCompanyBusinessAddress().getCity()))
                .body("company.businessAddress.postCode", equalTo(patchCorporateModel.getCompanyBusinessAddress().getPostCode()))
                .body("company.businessAddress.state", nullValue())
                .body("company.businessAddress.country", equalTo(patchCorporateModel.getCompanyBusinessAddress().getCountry()))
                .body("company.countryOfRegistration", equalTo(createCorporateModel.getCompany().getRegistrationCountry()))
                .body("industry", equalTo(createCorporateModel.getIndustry().toString()))
                .body("sourceOfFunds", equalTo(createCorporateModel.getSourceOfFunds().toString()))
                .body("sourceOfFundsOther", equalTo(createCorporateModel.getSourceOfFunds() == CorporateSourceOfFunds.OTHER ? createCorporateModel.getSourceOfFundsOther() : null))
                .body("acceptedTerms", equalTo(createCorporateModel.getAcceptedTerms()))
                .body("ipAddress", equalTo(createCorporateModel.getIpAddress()))
                .body("baseCurrency", equalTo(createCorporateModel.getBaseCurrency()))
                .body("feeGroup", equalTo("DEFAULT"))
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void PatchCorporates_SetAllDetailsNotAffectedByKybVerification_Success() {

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.DefaultPatchCorporateModel()
                        .setEmail(null)
                        .setMobile(null)
                        .setDateOfBirth(null)
                        .setName(null)
                        .setSurname(null)
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PatchCorporates_UpdateMissingSourceOfFundsOther_Success() {
        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setSourceOfFunds(CorporateSourceOfFunds.OTHER)
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("sourceOfFunds", equalTo(createCorporateModel.getSourceOfFunds().toString()));
    }

    @Test
    public void PatchCorporates_UpdateEmailKybVerified_RootUserAlreadyVerified() {

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ROOT_USER_ALREADY_VERIFIED"));
    }

    @Test
    public void PatchCorporates_UpdateNameKybVerified_RootUserAlreadyVerified() {

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setName(RandomStringUtils.randomAlphabetic(5))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ROOT_USER_ALREADY_VERIFIED"));
    }

    @Test
    public void PatchCorporates_UpdateSurnameKybVerified_RootUserAlreadyVerified() {

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setSurname(RandomStringUtils.randomAlphabetic(5))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ROOT_USER_ALREADY_VERIFIED"));
    }

    @Test
    public void PatchCorporates_UpdateDateOfBirthKybVerified_RootUserAlreadyVerified() {

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setDateOfBirth(new DateOfBirthModel(1982, 1, 3))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ROOT_USER_ALREADY_VERIFIED"));
    }

    @Test
    public void PatchCorporates_UpdateMobileCountryCodeKybVerified_RootUserAlreadyVerified() {

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(new MobileNumberModel("+0044", createCorporateModel.getRootUser().getMobile().getNumber()))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ROOT_USER_ALREADY_VERIFIED"));
    }

    @Test
    public void PatchCorporates_UpdateMobileNumberKybVerified_RootUserAlreadyVerified() {

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(new MobileNumberModel(createCorporateModel.getRootUser().getMobile().getCountryCode(),
                                RandomStringUtils.randomNumeric(8)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ROOT_USER_ALREADY_VERIFIED"));
    }

    @Test
    public void PatchCorporates_EmailAlreadyExists_EmailNotUnique() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(createCorporateModel.getRootUser().getEmail())
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_UNIQUE"));
    }

    @Test
    public void PatchCorporates_MobileWithoutCountryCode_BadRequest() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(new MobileNumberModel(null, RandomStringUtils.randomNumeric(8)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchCorporates_CountryCodeWithoutMobile_BadRequest() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(new MobileNumberModel("+356", null))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchCorporates_InvalidEmail_BadRequest() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(RandomStringUtils.randomAlphanumeric(6))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchCorporates_InvalidEmailFormat_BadRequest() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s.@weavrtest.io", RandomStringUtils.randomAlphanumeric(6)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"abc", ""})
    public void PatchCorporates_InvalidMobileNumber_BadRequest(final String mobileNumber) {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(new MobileNumberModel("+356", mobileNumber))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"abc", "123", "+"})
    public void PatchCorporates_InvalidMobileNumberCountryCode_MobileOrCountryCodeInvalid(final String mobileNumberCountryCode) {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(new MobileNumberModel(mobileNumberCountryCode, "123456"))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchCorporates_NoMobileNumberCountryCode_BadRequest() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(new MobileNumberModel("", "123456"))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchCorporates_PatchInactiveUser_Unauthorized() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(corporateEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        InnovatorHelper.deactivateCorporate(new DeactivateIdentityModel(false, "TEMPORARY"),
                corporateId, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword));

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PatchCorporates_PatchUsingNonRootUserAuthentication_Forbidden() {
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, authenticationToken);

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, user.getRight(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PatchCorporates_InvalidApiKey_Unauthorised() {
        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, "abc", authenticationToken, Optional.empty())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PatchCorporates_NoApiKey_BadRequest() {
        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, "", authenticationToken, Optional.empty())
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchCorporates_DifferentInnovatorApiKey_Forbidden() {

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PatchCorporates_RootUserLoggedOut_Unauthorised() {

        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, token, Optional.empty())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PatchCorporates_RootUserInactivityNotAffectingUsers_Success() {
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        UsersHelper.createAuthenticatedUser(usersModel, secretKey, authenticationToken);

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK);

        AuthenticationService
                .loginWithPassword(new LoginModel(usersModel.getEmail(), new PasswordModel(TestHelper.getDefaultPassword(secretKey))), secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());
    }

    @Test
    public void PatchCorporates_BackofficeImpersonator_Forbidden() {
        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PatchCorporate_InvalidDateOfBirthMonth_BadRequest() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setDateOfBirth(new DateOfBirthModel(2000, 13, 11))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchCorporate_InvalidDateOfBirthYear_BadRequest() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setDateOfBirth(new DateOfBirthModel(2101, 12, 3))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchCorporates_MissingAddressLine1_BadRequest() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setCompanyBusinessAddress(AddressModel.DefaultAddressModel()
                                .setAddressLine1(null)
                                .build())
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchCorporates_MissingCity_BadRequest() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setCompanyBusinessAddress(AddressModel.DefaultAddressModel()
                                .setCity(null)
                                .build())
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchCorporates_MissingPostCode_BadRequest() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setCompanyBusinessAddress(AddressModel.DefaultAddressModel()
                                .setPostCode(null)
                                .build())
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchCorporates_UnknownSourceOfFunds_BadRequest() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setSourceOfFunds(CorporateSourceOfFunds.UNKNOWN)
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchCorporates_UnknownIndustry_BadRequest() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setIndustry(Industry.UNKNOWN)
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"ABCD", ""})
    public void PatchCorporates_InvalidCurrency_BadRequest(final String currency) {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setBaseCurrency(currency)
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    @DisplayName("PatchCorporates_UnknownCurrency_CurrencyUnsupported - DEV-2807 opened to return 409")
    public void PatchCorporates_UnknownCurrency_CurrencyUnsupported() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setBaseCurrency("ABC")
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_INTERNAL_SERVER_ERROR);// To be updated to 409 - CURRENCY_UNSUPPORTED following fix
    }

    @Test
    public void PatchCorporates_UnknownFeeGroup_FeeGroupInvalid() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setFeeGroup("ABC")
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FEE_GROUP_INVALID"));
    }

    @Test
    public void PatchCorporates_MobileNotUnique_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(createCorporateModel.getRootUser().getMobile())
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateModel.getMobile().getNumber()));
    }

    @Test
    public void PatchCorporates_PatchUsingInnovatorAuthentication_Forbidden() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PatchCorporates_PatchUsingAdminAuthentication_Forbidden() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, AdminService.loginAdmin(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PatchCorporates_PatchMobileWithUniqueNumber_Success() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateModel.getMobile().getNumber()));
    }

    @Test
    public void PatchCorporates_PatchMobileWithNonUniqueNumberSameIdentityType_Success() {

        final CreateCorporateModel corporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                        .setMobile(MobileNumberModel.random())
                        .build())
                .build();

        CorporatesHelper.createAuthenticatedCorporate(corporateModel, secretKey);

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(corporateModel.getRootUser().getMobile())
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateModel.getMobile().getNumber()));
    }

    @Test
    public void PatchCorporates_PatchMobileWithNonUniqueNumberCrossIdentityType_Success() {

        final CreateConsumerModel consumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setMobile(MobileNumberModel.random())
                        .build())
                .build();

        ConsumersHelper.createAuthenticatedConsumer(consumerModel, secretKey);

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(consumerModel.getRootUser().getMobile())
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateModel.getMobile().getNumber()));
    }

    @Test
    public void PatchCorporates_MobileChangeLimitUpdateMobileFieldNull_Success() {

        IntStream.range(0, mobileChangeLimit)
                .forEach(i -> {
                    final PatchCorporateModel patchCorporateModel =
                            PatchCorporateModel.newBuilder()
                                    .setMobile(null)
                                    .build();

                    CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .body("rootUser.mobile.countryCode", equalTo(createCorporateModel.getRootUser().getMobile().getCountryCode()))
                            .body("rootUser.mobile.number", equalTo(createCorporateModel.getRootUser().getMobile().getNumber()));
                });

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(null)
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(createCorporateModel.getRootUser().getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(createCorporateModel.getRootUser().getMobile().getNumber()));

        final PatchCorporateModel patchCorporateModel2 =
                PatchCorporateModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel2, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateModel2.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateModel2.getMobile().getNumber()));
    }

    @Test
    public void PatchCorporates_MobileChangeLimitByUpdateNewNumber_LimitExceeded() {

        IntStream.range(0, mobileChangeLimit)
                .forEach(i -> {
                    final PatchCorporateModel patchCorporateModel =
                            PatchCorporateModel.newBuilder()
                                    .setMobile(MobileNumberModel.random())
                                    .build();

                    CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .body("rootUser.mobile.countryCode", equalTo(patchCorporateModel.getMobile().getCountryCode()))
                            .body("rootUser.mobile.number", equalTo(patchCorporateModel.getMobile().getNumber()));
                });

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MOBILE_NO_CHANGE_LIMIT_EXCEEDED"));
    }

    @Test
    public void PatchCorporates_UpdateMobileMultipleTimesWithItsOwnNumber_Success() {

        IntStream.range(0, 2)
                .forEach(i -> {
                    final PatchCorporateModel patchCorporateModel =
                            PatchCorporateModel.newBuilder()
                                    .setMobile(createCorporateModel.getRootUser().getMobile())
                                    .build();

                    CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .body("rootUser.mobile.countryCode", equalTo(patchCorporateModel.getMobile().getCountryCode()))
                            .body("rootUser.mobile.number", equalTo(patchCorporateModel.getMobile().getNumber()));
                });

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateModel.getMobile().getNumber()));
    }

    @Test
    public void PatchCorporates_UpdateAnotherFieldAfterMobileChangeLimitExceeded_Success() {
        IntStream.range(0, mobileChangeLimit)
                .forEach(i -> {
                    final PatchCorporateModel patchCorporateModel =
                            PatchCorporateModel.newBuilder()
                                    .setMobile(MobileNumberModel.random())
                                    .build();

                    CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .body("rootUser.mobile.countryCode", equalTo(patchCorporateModel.getMobile().getCountryCode()))
                            .body("rootUser.mobile.number", equalTo(patchCorporateModel.getMobile().getNumber()));
                });

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MOBILE_NO_CHANGE_LIMIT_EXCEEDED"));

        final PatchCorporateModel patchCorporateModel2 =
                PatchCorporateModel.newBuilder()
                        .setName(RandomStringUtils.randomAlphabetic(5))
                        .setSurname(RandomStringUtils.randomAlphabetic(5))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel2, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(patchCorporateModel2.getName()))
                .body("rootUser.surname", equalTo(patchCorporateModel2.getSurname()));
    }

    @Test
    public void PatchCorporates_MobileChangeLimitByUpdateNonUniqueNumber_LimitExceeded() {

        IntStream.range(0, mobileChangeLimit)
                .forEach(i -> {
                    final CreateCorporateModel corporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                            .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                    .setMobile(MobileNumberModel.random())
                                    .build())
                            .build();

                    CorporatesHelper.createAuthenticatedCorporate(corporateModel, secretKey);

                    final PatchCorporateModel patchCorporateModel =
                            PatchCorporateModel.newBuilder()
                                    .setMobile(corporateModel.getRootUser().getMobile())
                                    .build();

                    CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .body("rootUser.mobile.countryCode", equalTo(patchCorporateModel.getMobile().getCountryCode()))
                            .body("rootUser.mobile.number", equalTo(patchCorporateModel.getMobile().getNumber()));
                });


        PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MOBILE_NO_CHANGE_LIMIT_EXCEEDED"));
    }

    /**
     * Cases for the ticket <a href="https://weavr-payments.atlassian.net/browse/DEV-4974">...</a>
     * 1. Create 2 users with the same mobile number
     * 2. Create third user without mobile number or different mobile number
     * 3. Patch third user with the same mobile number and country code as previous ones.
     */
    @Test
    public void PatchCorporates_SeveralUsersSameNumberPatchMobileNotUniqueNumber_Success() {
        final String mobileNumber = MobileNumberModel.random().getNumber();

        final CreateCorporateModel firstCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setMobile(new MobileNumberModel("+356", mobileNumber))
                                .build())
                        .build();
        CorporatesHelper.createAuthenticatedVerifiedCorporate(firstCorporateModel, secretKey);

        final CreateCorporateModel secondCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setMobile(new MobileNumberModel("+356", mobileNumber))
                                .build())
                        .build();
        CorporatesHelper.createAuthenticatedVerifiedCorporate(secondCorporateModel, secretKey);

        final CreateCorporateModel thirdCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setMobile(MobileNumberModel.random())
                                .build())
                        .build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(thirdCorporateModel, secretKey);

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(new MobileNumberModel("+356", mobileNumber))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateModel.getMobile().getNumber()));
    }

    @Test
    public void PatchCorporates_CheckMobileChangeSmsOldNumberVerified_Success() {
        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey,
                authenticationToken);

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateModel.getMobile().getNumber()));

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey,
                authenticationToken);

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(createCorporateModel.getRootUser().getMobile().getNumber());
        assertEquals(String.format(MailHogSms.SCA_CHANGE_SMS.getSmsText(),
                        StringUtils.right(createCorporateModel.getRootUser().getMobile().getNumber(), 4),
                        StringUtils.right(patchCorporateModel.getMobile().getNumber(), 4)),
                sms.getBody());
    }

    @Test
    public void PatchCorporates_CheckMobileChangeSmsOldNumberNotVerified_NoSms() {

        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateModel.getMobile().getNumber()));

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey,
                authenticationToken);

        MailhogService.getMailHogSms(createCorporateModel.getRootUser().getMobile().getNumber())
                .then()
                .statusCode(200)
                .body("items[0]", nullValue());
    }

    @Test
    public void PatchCorporates_NewEmailVerifiedMobileNumberOtpNotEnrolledSecurityRule24H_Success() {

        CorporatesHelper.verifyEmail(corporateEmail, secretKey);

        final PatchCorporateModel patchCorporateEmailModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateEmailModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(corporateEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobileNumberVerified", equalTo(false));

        CorporatesService.verifyEmail(new EmailVerificationModel(patchCorporateEmailModel.getEmail(), TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        CorporatesService.getCorporates(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createCorporateModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(patchCorporateEmailModel.getEmail()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        //No blocking time to check the new functionality, user can update the mobile number once otp was not enrolled

        final PatchCorporateModel patchCorporateMobileModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(new MobileNumberModel("+356", String.format("21%s", RandomStringUtils.randomNumeric(6))))
                        .build();
        CorporatesService.patchCorporate(patchCorporateMobileModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateMobileModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateMobileModel.getMobile().getNumber()));

        CorporatesService.getCorporates(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createCorporateModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(patchCorporateEmailModel.getEmail()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateMobileModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateMobileModel.getMobile().getNumber()));

    }

    @Test
    public void PatchCorporates_NewEmailNotVerifiedAndMobileNumberOtpNotEnrolledSecurityRule24H_Success() {

        CorporatesHelper.verifyEmail(corporateEmail, secretKey);

        final PatchCorporateModel patchCorporateEmailModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateEmailModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(corporateEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobileNumberVerified", equalTo(false));

        CorporatesService.getCorporates(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createCorporateModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(corporateEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        //No blocking time to check the response, should be 200 once new email was not verified and otp not enrolled

        final PatchCorporateModel patchCorporateMobileModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();
        CorporatesService.patchCorporate(patchCorporateMobileModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateMobileModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateMobileModel.getMobile().getNumber()));

        CorporatesService.getCorporates(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createCorporateModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(corporateEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateMobileModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateMobileModel.getMobile().getNumber()));
    }

    @Test
    public void PatchCorporates_NewEmailVerifiedAndMobileNumberOtpEnrolledSecurityRule24H_Conflict() {

        CorporatesHelper.verifyEmail(corporateEmail, secretKey);

        final PatchCorporateModel patchCorporateEmailModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateEmailModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(corporateEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobileNumberVerified", equalTo(false));

        CorporatesService.verifyEmail(new EmailVerificationModel(patchCorporateEmailModel.getEmail(), TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        CorporatesService.getCorporates(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createCorporateModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(patchCorporateEmailModel.getEmail()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
                secretKey, authenticationToken);

        //Blocking time less than mobileChangeBlockingLimit, once new email was verified and otp enrolled user has to get error 409

        final PatchCorporateModel patchCorporateMobileModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();
        CorporatesService.patchCorporate(patchCorporateMobileModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MOBILE_CHANGE_NOT_ALLOWED"));

        CorporatesService.getCorporates(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createCorporateModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(patchCorporateEmailModel.getEmail()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobile.countryCode", equalTo(createCorporateModel.getRootUser().getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(createCorporateModel.getRootUser().getMobile().getNumber()));
    }

    @Test
    public void PatchCorporates_NewEmailVerifiedAndMobileNumberOtpEnrolledSecurityRule24H_Success() throws InterruptedException {

        CorporatesHelper.verifyEmail(corporateEmail, secretKey);

        final PatchCorporateModel patchCorporateEmailModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateEmailModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(corporateEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobileNumberVerified", equalTo(false));

        CorporatesService.verifyEmail(new EmailVerificationModel(patchCorporateEmailModel.getEmail(), TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        CorporatesService.getCorporates(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createCorporateModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(patchCorporateEmailModel.getEmail()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
                secretKey, authenticationToken);

        //Blocking time is mobileChangeBlockingLimit, once new email was verified and otp enrolled user can update mobile number after blocking time
        TimeUnit.SECONDS.sleep(mobileChangeBlockingLimit);

        final PatchCorporateModel patchCorporateMobileModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();
        CorporatesService.patchCorporate(patchCorporateMobileModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateMobileModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateMobileModel.getMobile().getNumber()));

        CorporatesService.getCorporates(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createCorporateModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(patchCorporateEmailModel.getEmail()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateMobileModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateMobileModel.getMobile().getNumber()));
    }

    @Test
    public void PatchCorporates_ResumeLostPasswordMobileNumberOtpNotEnrolledSecurityRule24_Success() {

        CorporatesHelper.verifyEmail(corporateEmail, secretKey);

        PasswordsService.startLostPassword(new LostPasswordStartModel(corporateEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(corporateEmail)
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());

        //No blocking time to check the new functionality, user can update the mobile number once otp was not enrolled

        final PatchCorporateModel patchCorporateMobileModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();
        CorporatesService.patchCorporate(patchCorporateMobileModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateMobileModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateMobileModel.getMobile().getNumber()));

        CorporatesService.getCorporates(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createCorporateModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(corporateEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateMobileModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateMobileModel.getMobile().getNumber()));
    }

    @Test
    public void PatchCorporates_ResumeLostPasswordAndMobileNumberOtpEnrolledSecurityRule24H_Conflict() {

        CorporatesHelper.verifyEmail(corporateEmail, secretKey);

        PasswordsService.startLostPassword(new LostPasswordStartModel(corporateEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(corporateEmail)
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
                secretKey, authenticationToken);

        //Blocking time less than mobileChangeBlockingLimit, once new email was verified and otp enrolled user has to get error 409

        final PatchCorporateModel patchCorporateMobileModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();
        CorporatesService.patchCorporate(patchCorporateMobileModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MOBILE_CHANGE_NOT_ALLOWED"));

        CorporatesService.getCorporates(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createCorporateModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(corporateEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobile.countryCode", equalTo(createCorporateModel.getRootUser().getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(createCorporateModel.getRootUser().getMobile().getNumber()));
    }

    @Test
    public void PatchCorporates_ResumeLostPasswordAndMobileNumberOtpEnrolledSecurityRule24H_Success() throws InterruptedException {

        CorporatesHelper.verifyEmail(corporateEmail, secretKey);

        PasswordsService.startLostPassword(new LostPasswordStartModel(corporateEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(corporateEmail)
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
                secretKey, authenticationToken);

        //Blocking time is mobileChangeBlockingLimit, once new email was verified and otp enrolled user can update mobile number after blocking time
        TimeUnit.SECONDS.sleep(mobileChangeBlockingLimit);

        final PatchCorporateModel patchCorporateMobileModel =
                PatchCorporateModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();
        CorporatesService.patchCorporate(patchCorporateMobileModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateMobileModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateMobileModel.getMobile().getNumber()));

        CorporatesService.getCorporates(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createCorporateModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(corporateEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateMobileModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateMobileModel.getMobile().getNumber()));
    }

    @Test
    public void PatchCorporates_ChangeEmailWithNotAllowedDomain_Conflict() {
        final PatchCorporateModel patchCorporateModel =
                PatchCorporateModel.newBuilder()
                        .setEmail(String.format("%s@gav0.com", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_DOMAIN_NOT_ALLOWED"));
    }

    @Test
    public void PatchCorporates_PatchMobileNumberCountryCodeCharacterLimitOne_BadRequest() {

        final PatchCorporateModel patchCorporateModel = PatchCorporateModel.newBuilder()
                .setMobile(new MobileNumberModel("1", String.format("82923%s", RandomStringUtils.randomNumeric(5))))
                .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchUser_PatchMobileNumberCountryCodeCharacterLimitTwo_Success() {

        final PatchCorporateModel patchCorporateModel = PatchCorporateModel.newBuilder()
                .setMobile(new MobileNumberModel("+49",
                        String.format("30%s", RandomStringUtils.randomNumeric(6))))
                .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateModel.getMobile().getNumber()));
    }

    @Test
    public void PatchUser_PatchMobileNumberCountryCodeCharacterLimitFour_Success() {

        final PatchCorporateModel patchCorporateModel = PatchCorporateModel.newBuilder()
                .setMobile(new MobileNumberModel("+1829",
                        String.format("23%s", RandomStringUtils.randomNumeric(5))))
                .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateModel.getMobile().getNumber()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"+1-1829", "001-1829"})
    public void PatchUser_PatchMobileNumberCountryCodeCharacterLimitMoreThanSix_BadRequest(final String countryCode) {

        final PatchCorporateModel patchCorporateModel = PatchCorporateModel.newBuilder()
                .setMobile(new MobileNumberModel(countryCode, String.format("23%s", RandomStringUtils.randomNumeric(5))))
                .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("_embedded.errors[0].message", equalTo("request.mobile.countryCode: must match \"^\\+[0-9]+$\""))
                .body("_embedded.errors[1].message", equalTo("request.mobile.countryCode: size must be between 1 and 6"));
    }


    @ParameterizedTest
    @MethodSource("emailProvider")
    public void PatchCorporates_ChangeEmailHavingApostropheOrSingleQuotes_Success(final String email) {

        final PatchCorporateModel patchCorporateModel = PatchCorporateModel.newBuilder().setEmail(email).build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    // TODO Idempotency tests to be updated when this operation becomes idempotent

//    @Test
//    public void PatchCorporates_SameIdempotencyRefDifferentPayload_BadRequest() {
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//
//        final PatchCorporateModel firstPatchCorporateModel =
//                PatchCorporateModel.newBuilder().setName(RandomStringUtils.randomAlphabetic(5))
//                        .build();
//
//        CorporatesService.patchCorporate(firstPatchCorporateModel, secretKey, authenticationToken, Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_OK);
//
//        final PatchCorporateModel secondPatchCorporateModel =
//                PatchCorporateModel.newBuilder().setName(RandomStringUtils.randomAlphabetic(5))
//                        .build();
//
//        CorporatesService.patchCorporate(secondPatchCorporateModel, secretKey, authenticationToken, Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_BAD_REQUEST);
//    }
//
//    @Test
//    public void PatchCorporates_SameIdempotencyRefSamePayload_Success() {
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//        final List<Response> responses = new ArrayList<>();
//
//        final PatchCorporateModel patchCorporateModel =
//                PatchCorporateModel.newBuilder().setName(RandomStringUtils.randomAlphabetic(5))
//                        .build();
//
//        responses.add(CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.of(idempotencyReference)));
//        responses.add(CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.of(idempotencyReference)));
//
//        responses.forEach(response ->
//                response.then()
//                        .statusCode(SC_OK)
//                        .body("id.type", equalTo("CORPORATE"))
//                        .body("id.id", notNullValue())
//                        .body("profileId", equalTo(corporateProfileId))
//                        .body("tag", equalTo(createCorporateModel.getTag()))
//                        .body("rootUser.id.type", equalTo("CORPORATE"))
//                        .body("rootUser.id.id", notNullValue())
//                        .body("rootUser.name", equalTo(patchCorporateModel.getName()))
//                        .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
//                        .body("rootUser.email", equalTo(corporateEmail))
//                        .body("rootUser.mobile.countryCode", equalTo(createCorporateModel.getRootUser().getMobile().getCountryCode()))
//                        .body("rootUser.mobile.number", equalTo(createCorporateModel.getRootUser().getMobile().getNumber()))
//                        .body("rootUser.companyPosition", equalTo(createCorporateModel.getRootUser().getCompanyPosition().name()))
//                        .body("rootUser.active", equalTo(true))
//                        .body("rootUser.emailVerified", equalTo(true))
//                        .body("rootUser.mobileNumberVerified", equalTo(false))
//                        .body("rootUser.dateOfBirth.year", equalTo(createCorporateModel.getRootUser().getDateOfBirth().getYear()))
//                        .body("rootUser.dateOfBirth.month", equalTo(createCorporateModel.getRootUser().getDateOfBirth().getMonth()))
//                        .body("rootUser.dateOfBirth.day", equalTo(createCorporateModel.getRootUser().getDateOfBirth().getDay()))
//                        .body("company.name", equalTo(createCorporateModel.getCompany().getName()))
//                        .body("company.type", equalTo(createCorporateModel.getCompany().getType()))
//                        .body("company.registrationNumber", equalTo(createCorporateModel.getCompany().getRegistrationNumber()))
//                        .body("company.businessAddress.addressLine1", equalTo(createCorporateModel.getCompany().getBusinessAddress().getAddressLine1()))
//                        .body("company.businessAddress.addressLine2", equalTo(createCorporateModel.getCompany().getBusinessAddress().getAddressLine2()))
//                        .body("company.businessAddress.city", equalTo(createCorporateModel.getCompany().getBusinessAddress().getCity()))
//                        .body("company.businessAddress.postCode", equalTo(createCorporateModel.getCompany().getBusinessAddress().getPostCode()))
//                        .body("company.businessAddress.state", equalTo(createCorporateModel.getCompany().getBusinessAddress().getState()))
//                        .body("company.businessAddress.country", equalTo(createCorporateModel.getCompany().getBusinessAddress().getCountry()))
//                        .body("company.countryOfRegistration", equalTo(createCorporateModel.getCompany().getRegistrationCountry()))
//                        .body("industry", equalTo(createCorporateModel.getIndustry().toString()))
//                        .body("sourceOfFunds", equalTo(createCorporateModel.getSourceOfFunds().toString()))
//                        .body("sourceOfFundsOther", equalTo(createCorporateModel.getSourceOfFunds() == CorporateSourceOfFunds.OTHER ? createCorporateModel.getSourceOfFundsOther() : null))
//                        .body("acceptedTerms", equalTo(createCorporateModel.getAcceptedTerms()))
//                        .body("ipAddress", equalTo(createCorporateModel.getIpAddress()))
//                        .body("baseCurrency", equalTo(createCorporateModel.getBaseCurrency()))
//                        .body("feeGroup", equalTo("DEFAULT"))
//                        .body("creationTimestamp", notNullValue()));
//
//        assertEquals(responses.get(0).jsonPath().getString("id.id"), responses.get(1).jsonPath().getString("id.id"));
//        assertEquals(responses.get(0).jsonPath().getString("rootUser.name"), responses.get(1).jsonPath().getString("rootUser.name"));
//    }
//
//    @Test
//    public void PatchCorporates_DifferentIdempotencyRefSamePayload_Success() {
//        final String firstIdempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//        final String secondIdempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//        final List<Response> responses = new ArrayList<>();
//
//        final PatchCorporateModel patchCorporateModel =
//                PatchCorporateModel.newBuilder().setName(RandomStringUtils.randomAlphabetic(5))
//                        .build();
//
//        responses.add(CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.of(firstIdempotencyReference)));
//        responses.add(CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.of(secondIdempotencyReference)));
//
//        responses.forEach(response ->
//                assertSuccessfulResponse(response, createCorporateModel, patchCorporateModel, corporateProfileId));
//
//        assertEquals(responses.get(0).jsonPath().getString("id.id"), responses.get(1).jsonPath().getString("id.id"));
//        assertEquals(responses.get(0).jsonPath().getString("rootUser.name"), responses.get(1).jsonPath().getString("rootUser.name"));
//    }
//
//    @Test
//    public void PatchCorporates_DifferentIdempotencyRefDifferentPayload_Success() {
//        final String firstIdempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//        final String secondIdempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//        final List<Response> responses = new ArrayList<>();
//
//        final PatchCorporateModel patchCorporateModel =
//                PatchCorporateModel.newBuilder().setName(RandomStringUtils.randomAlphabetic(5))
//                        .build();
//
//        final PatchCorporateModel patchCorporateModel1 =
//                PatchCorporateModel.newBuilder().setName(RandomStringUtils.randomAlphabetic(5))
//                        .build();
//
//        responses.add(CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.of(firstIdempotencyReference)));
//        responses.add(CorporatesService.patchCorporate(patchCorporateModel1, secretKey, authenticationToken, Optional.of(secondIdempotencyReference)));
//
//        assertSuccessfulResponse(responses.get(0), createCorporateModel, patchCorporateModel, corporateProfileId);
//        assertSuccessfulResponse(responses.get(1), createCorporateModel, patchCorporateModel1, corporateProfileId);
//
//        assertEquals(responses.get(0).jsonPath().getString("id.id"), responses.get(1).jsonPath().getString("id.id"));
//        assertEquals(responses.get(0).jsonPath().getString("rootUser.name"), responses.get(1).jsonPath().getString("rootUser.name"));
//    }
//
//    @Test
//    public void PatchCorporates_LongIdempotencyRef_RequestTooLong() {
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(8000);
//
//        final PatchCorporateModel patchCorporateModel =
//                PatchCorporateModel.DefaultPatchCorporateModel()
//                        .build();
//
//        CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_REQUEST_TOO_LONG);
//    }
//
//    @Test
//    public void PatchCorporates_SameIdempotencyRefDifferentPayloadInitialCallFailed_Success() {
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//
//        final PatchCorporateModel firstPatchCorporateModel =
//                PatchCorporateModel.newBuilder()
//                        .setMobile(new MobileNumberModel(null, RandomStringUtils.randomNumeric(8)))
//                        .build();
//
//        CorporatesService.patchCorporate(firstPatchCorporateModel, secretKey, authenticationToken, Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_BAD_REQUEST);
//
//        final PatchCorporateModel secondPatchCorporateModel =
//                PatchCorporateModel.newBuilder()
//                        .setMobile(MobileNumberModel.random())
//                        .build();
//
//        assertSuccessfulResponse(CorporatesService.patchCorporate(secondPatchCorporateModel, secretKey, authenticationToken, Optional.of(idempotencyReference)), createCorporateModel, secondPatchCorporateModel, corporateProfileId);
//    }
//
//    @Test
//    public void PatchCorporates_SameIdempotencyRefSamePayloadReferenceExpired_Success() throws InterruptedException {
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//        final List<Response> responses = new ArrayList<>();
//
//        final PatchCorporateModel patchCorporateModel =
//                PatchCorporateModel.newBuilder().setName(RandomStringUtils.randomAlphabetic(5))
//                        .build();
//
//        responses.add(CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.of(idempotencyReference)));
//        TimeUnit.SECONDS.sleep(18);
//        responses.add(CorporatesService.patchCorporate(patchCorporateModel, secretKey, authenticationToken, Optional.of(idempotencyReference)));
//
//        responses.forEach(response ->
//                response.then()
//                        .statusCode(SC_OK)
//                        .body("id.type", equalTo("CORPORATE"))
//                        .body("id.id", notNullValue())
//                        .body("profileId", equalTo(corporateProfileId))
//                        .body("tag", equalTo(createCorporateModel.getTag()))
//                        .body("rootUser.id.type", equalTo("CORPORATE"))
//                        .body("rootUser.id.id", notNullValue())
//                        .body("rootUser.name", equalTo(patchCorporateModel.getName()))
//                        .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
//                        .body("rootUser.email", equalTo(corporateEmail))
//                        .body("rootUser.mobile.countryCode", equalTo(createCorporateModel.getRootUser().getMobile().getCountryCode()))
//                        .body("rootUser.mobile.number", equalTo(createCorporateModel.getRootUser().getMobile().getNumber()))
//                        .body("rootUser.companyPosition", equalTo(createCorporateModel.getRootUser().getCompanyPosition().name()))
//                        .body("rootUser.active", equalTo(true))
//                        .body("rootUser.emailVerified", equalTo(true))
//                        .body("rootUser.mobileNumberVerified", equalTo(false))
//                        .body("rootUser.dateOfBirth.year", equalTo(createCorporateModel.getRootUser().getDateOfBirth().getYear()))
//                        .body("rootUser.dateOfBirth.month", equalTo(createCorporateModel.getRootUser().getDateOfBirth().getMonth()))
//                        .body("rootUser.dateOfBirth.day", equalTo(createCorporateModel.getRootUser().getDateOfBirth().getDay()))
//                        .body("company.name", equalTo(createCorporateModel.getCompany().getName()))
//                        .body("company.type", equalTo(createCorporateModel.getCompany().getType()))
//                        .body("company.registrationNumber", equalTo(createCorporateModel.getCompany().getRegistrationNumber()))
//                        .body("company.businessAddress.addressLine1", equalTo(createCorporateModel.getCompany().getBusinessAddress().getAddressLine1()))
//                        .body("company.businessAddress.addressLine2", equalTo(createCorporateModel.getCompany().getBusinessAddress().getAddressLine2()))
//                        .body("company.businessAddress.city", equalTo(createCorporateModel.getCompany().getBusinessAddress().getCity()))
//                        .body("company.businessAddress.postCode", equalTo(createCorporateModel.getCompany().getBusinessAddress().getPostCode()))
//                        .body("company.businessAddress.state", equalTo(createCorporateModel.getCompany().getBusinessAddress().getState()))
//                        .body("company.businessAddress.country", equalTo(createCorporateModel.getCompany().getBusinessAddress().getCountry()))
//                        .body("company.countryOfRegistration", equalTo(createCorporateModel.getCompany().getRegistrationCountry()))
//                        .body("industry", equalTo(createCorporateModel.getIndustry().toString()))
//                        .body("sourceOfFunds", equalTo(createCorporateModel.getSourceOfFunds().toString()))
//                        .body("sourceOfFundsOther", equalTo(createCorporateModel.getSourceOfFunds() == CorporateSourceOfFunds.OTHER ? createCorporateModel.getSourceOfFundsOther() : null))
//                        .body("acceptedTerms", equalTo(createCorporateModel.getAcceptedTerms()))
//                        .body("ipAddress", equalTo(createCorporateModel.getIpAddress()))
//                        .body("baseCurrency", equalTo(createCorporateModel.getBaseCurrency()))
//                        .body("feeGroup", equalTo("DEFAULT"))
//                        .body("creationTimestamp", notNullValue()));
//
//        assertEquals(responses.get(0).jsonPath().getString("id.id"), responses.get(1).jsonPath().getString("id.id"));
//        assertEquals(responses.get(0).jsonPath().getString("rootUser.name"), responses.get(1).jsonPath().getString("rootUser.name"));
//    }

    private void assertSuccessfulResponse(final Response response,
                                          final CreateCorporateModel createCorporateModel,
                                          final PatchCorporateModel patchCorporateModel,
                                          final String corporateProfileId) {

        response.then()
                .statusCode(SC_OK)
                .body("id.type", equalTo("CORPORATE"))
                .body("id.id", notNullValue())
                .body("profileId", equalTo(corporateProfileId))
                .body("tag", equalTo(createCorporateModel.getTag()))
                .body("rootUser.id.type", equalTo("CORPORATE"))
                .body("rootUser.id.id", notNullValue())
                .body("rootUser.name", equalTo(patchCorporateModel.getName()))
                .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(corporateEmail))
                .body("rootUser.mobile.countryCode", equalTo(createCorporateModel.getRootUser().getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(createCorporateModel.getRootUser().getMobile().getNumber()))
                .body("rootUser.companyPosition", equalTo(createCorporateModel.getRootUser().getCompanyPosition().name()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobileNumberVerified", equalTo(false))
                .body("rootUser.dateOfBirth.year", equalTo(createCorporateModel.getRootUser().getDateOfBirth().getYear()))
                .body("rootUser.dateOfBirth.month", equalTo(createCorporateModel.getRootUser().getDateOfBirth().getMonth()))
                .body("rootUser.dateOfBirth.day", equalTo(createCorporateModel.getRootUser().getDateOfBirth().getDay()))
                .body("company.name", equalTo(createCorporateModel.getCompany().getName()))
                .body("company.type", equalTo(createCorporateModel.getCompany().getType()))
                .body("company.registrationNumber", equalTo(createCorporateModel.getCompany().getRegistrationNumber()))
                .body("company.businessAddress.addressLine1", equalTo(createCorporateModel.getCompany().getBusinessAddress().getAddressLine1()))
                .body("company.businessAddress.addressLine2", equalTo(createCorporateModel.getCompany().getBusinessAddress().getAddressLine2()))
                .body("company.businessAddress.city", equalTo(createCorporateModel.getCompany().getBusinessAddress().getCity()))
                .body("company.businessAddress.postCode", equalTo(createCorporateModel.getCompany().getBusinessAddress().getPostCode()))
                .body("company.businessAddress.state", equalTo(createCorporateModel.getCompany().getBusinessAddress().getState()))
                .body("company.businessAddress.country", equalTo(createCorporateModel.getCompany().getBusinessAddress().getCountry()))
                .body("company.countryOfRegistration", equalTo(createCorporateModel.getCompany().getRegistrationCountry()))
                .body("industry", equalTo(createCorporateModel.getIndustry().toString()))
                .body("sourceOfFunds", equalTo(createCorporateModel.getSourceOfFunds().toString()))
                .body("sourceOfFundsOther", equalTo(createCorporateModel.getSourceOfFunds() == CorporateSourceOfFunds.OTHER ? createCorporateModel.getSourceOfFundsOther() : null))
                .body("acceptedTerms", equalTo(createCorporateModel.getAcceptedTerms()))
                .body("ipAddress", equalTo(createCorporateModel.getIpAddress()))
                .body("baseCurrency", equalTo(createCorporateModel.getBaseCurrency()))
                .body("feeGroup", equalTo("DEFAULT"))
                .body("creationTimestamp", notNullValue());
    }

    private static Stream<Arguments> emailProvider() {
        return Stream.of(
                arguments(String.format("%s's@weavrtest.io", RandomStringUtils.randomAlphabetic(5))),
                arguments(String.format("'%s'@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
        );
    }
}
