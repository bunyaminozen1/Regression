package fpi.paymentrun.identities.buyers;

import commons.enums.Roles;
import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.models.UpdateBuyerModel;
import fpi.paymentrun.services.BuyersService;
import opc.enums.opc.IdentityType;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.List;

import static fpi.helpers.BuyersHelper.createUnauthenticatedBuyer;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@Tag(PluginsTags.PAYMENT_RUN_BUYERS)
public class GetBuyerTests extends BasePaymentRunSetup {

    private static String buyerId;
    private static String buyerToken;
    private static CreateBuyerModel createBuyerModel;

    @BeforeAll
    public static void Setup() {
        createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();

        final Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(createBuyerModel, secretKey);
        buyerId = buyer.getLeft();
        buyerToken = buyer.getRight();
    }

    @Test
    public void GetBuyer_RootUserToken_Success() {
        BuyersService.getBuyer(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(buyerId))
                .body("tag", equalTo(createBuyerModel.getTag()))
                .body("adminUser.id", notNullValue())
                .body("adminUser.name", equalTo(createBuyerModel.getAdminUser().getName()))
                .body("adminUser.surname", equalTo(createBuyerModel.getAdminUser().getSurname()))
                .body("adminUser.email", equalTo(createBuyerModel.getAdminUser().getEmail()))
                .body("adminUser.mobile.countryCode", equalTo(createBuyerModel.getAdminUser().getMobile().getCountryCode()))
                .body("adminUser.mobile.number", equalTo(createBuyerModel.getAdminUser().getMobile().getNumber()))
                .body("adminUser.companyPosition", equalTo(createBuyerModel.getAdminUser().getCompanyPosition().toString()))
                .body("adminUser.dateOfBirth.year", equalTo(createBuyerModel.getAdminUser().getDateOfBirth().getYear()))
                .body("adminUser.dateOfBirth.month", equalTo(createBuyerModel.getAdminUser().getDateOfBirth().getMonth()))
                .body("adminUser.dateOfBirth.day", equalTo(createBuyerModel.getAdminUser().getDateOfBirth().getDay()))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true))
                .body("adminUser.mobileNumberVerified", equalTo(true))
                .body("adminUser.roles[0]", equalTo("ADMIN"))
                .body("company.type", equalTo(createBuyerModel.getCompany().getType()))
                .body("company.name", equalTo(createBuyerModel.getCompany().getName()))
                .body("company.registrationNumber", equalTo(createBuyerModel.getCompany().getRegistrationNumber()))
                .body("company.businessAddress.addressLine1", equalTo(createBuyerModel.getCompany().getBusinessAddress().getAddressLine1()))
                .body("company.businessAddress.addressLine2", equalTo(createBuyerModel.getCompany().getBusinessAddress().getAddressLine2()))
                .body("company.businessAddress.city", equalTo(createBuyerModel.getCompany().getBusinessAddress().getCity()))
                .body("company.businessAddress.postCode", equalTo(createBuyerModel.getCompany().getBusinessAddress().getPostCode()))
                .body("company.businessAddress.state", equalTo(createBuyerModel.getCompany().getBusinessAddress().getState()))
                .body("company.businessAddress.country", equalTo(createBuyerModel.getCompany().getBusinessAddress().getCountry()))
                .body("company.countryOfRegistration", equalTo(createBuyerModel.getCompany().getCountryOfRegistration()))
                .body("company.incorporatedOn.year", nullValue())
                .body("company.incorporatedOn.month", nullValue())
                .body("company.incorporatedOn.day", nullValue())
                .body("acceptedTerms", equalTo(createBuyerModel.isAcceptedTerms()))
                .body("ipAddress", equalTo(createBuyerModel.getIpAddress()))
                .body("baseCurrency", equalTo(createBuyerModel.getBaseCurrency()))
                .body("supportedCurrencies", equalTo(createBuyerModel.getSupportedCurrencies()))
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void GetBuyer_CheckUserRoles_Success() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final Pair<String, String> buyer = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);
        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.allRolesUpdateBuyerModel().build();
        BuyersHelper.updateBuyer(updateBuyerModel, secretKey, buyer.getRight());

        BuyersService.getBuyer(secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(buyer.getLeft()))
                .body("tag", equalTo(createBuyerModel.getTag()))
                .body("adminUser.id", notNullValue())
                .body("adminUser.name", equalTo(createBuyerModel.getAdminUser().getName()))
                .body("adminUser.surname", equalTo(createBuyerModel.getAdminUser().getSurname()))
                .body("adminUser.email", equalTo(createBuyerModel.getAdminUser().getEmail()))
                .body("adminUser.mobile.countryCode", equalTo(createBuyerModel.getAdminUser().getMobile().getCountryCode()))
                .body("adminUser.mobile.number", equalTo(createBuyerModel.getAdminUser().getMobile().getNumber()))
                .body("adminUser.companyPosition", equalTo(createBuyerModel.getAdminUser().getCompanyPosition().toString()))
                .body("adminUser.dateOfBirth.year", equalTo(createBuyerModel.getAdminUser().getDateOfBirth().getYear()))
                .body("adminUser.dateOfBirth.month", equalTo(createBuyerModel.getAdminUser().getDateOfBirth().getMonth()))
                .body("adminUser.dateOfBirth.day", equalTo(createBuyerModel.getAdminUser().getDateOfBirth().getDay()))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(true))
                .body("adminUser.mobileNumberVerified", equalTo(false))
                .body("adminUser.roles", equalTo(List.of(Roles.ADMIN.name(), Roles.CONTROLLER.name(), Roles.CREATOR.name())))
                .body("company.type", equalTo(createBuyerModel.getCompany().getType()))
                .body("company.name", equalTo(createBuyerModel.getCompany().getName()))
                .body("company.registrationNumber", equalTo(createBuyerModel.getCompany().getRegistrationNumber()))
                .body("company.businessAddress.addressLine1", equalTo(createBuyerModel.getCompany().getBusinessAddress().getAddressLine1()))
                .body("company.businessAddress.addressLine2", equalTo(createBuyerModel.getCompany().getBusinessAddress().getAddressLine2()))
                .body("company.businessAddress.city", equalTo(createBuyerModel.getCompany().getBusinessAddress().getCity()))
                .body("company.businessAddress.postCode", equalTo(createBuyerModel.getCompany().getBusinessAddress().getPostCode()))
                .body("company.businessAddress.state", equalTo(createBuyerModel.getCompany().getBusinessAddress().getState()))
                .body("company.businessAddress.country", equalTo(createBuyerModel.getCompany().getBusinessAddress().getCountry()))
                .body("company.countryOfRegistration", equalTo(createBuyerModel.getCompany().getCountryOfRegistration()))
                .body("company.incorporatedOn.year", nullValue())
                .body("company.incorporatedOn.month", nullValue())
                .body("company.incorporatedOn.day", nullValue())
                .body("acceptedTerms", equalTo(createBuyerModel.isAcceptedTerms()))
                .body("ipAddress", equalTo(createBuyerModel.getIpAddress()))
                .body("baseCurrency", equalTo(createBuyerModel.getBaseCurrency()))
                .body("supportedCurrencies", equalTo(createBuyerModel.getSupportedCurrencies()))
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void GetBuyer_InvalidApiKey_Unauthorised() {
        BuyersService.getBuyer("abc", buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetBuyer_NoApiKey_BadRequest() {
        BuyersService.getBuyerNoApiKey(buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("api-key"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void GetBuyer_NoToken_Unauthorised(final String token) {
        BuyersService.getBuyer(secretKey, token)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetBuyer_InvalidToken_Unauthorised() {
        BuyersService.getBuyer(secretKey, "abc")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetBuyer_DifferentProgrammeApiKey_Unauthorised() {
        BuyersService.getBuyer(secretKeyAppTwo, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetBuyer_RootUserLoggedOut_Unauthorised() {

        final String buyerToken = createUnauthenticatedBuyer(secretKey).getRight();

        BuyersService.getBuyer(secretKey, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetBuyer_BackofficeImpersonator_Unauthorised() {
        BuyersService.getBuyer(secretKey, getBackofficeImpersonateToken(buyerId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetBuyer_AuthUserToken_Forbidden() {
        final String userToken =
                BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, buyerToken).getRight();

        BuyersService.getBuyer(secretKey, userToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }
}
