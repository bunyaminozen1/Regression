package opc.junit.multi.corporates;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.CorporatesService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.*;

public class GetCorporatesTests extends BaseCorporatesSetup {

    private static String corporateId;
    private static CreateCorporateModel createCorporateModel;
    private static String authenticationToken;

    @BeforeAll
    public static void Setup(){
        createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        authenticationToken = authenticatedCorporate.getRight();
    }

    @Test
    public void GetCorporates_Success(){
        CorporatesService.getCorporates(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("id.type", equalTo(IdentityType.CORPORATE.name()))
                .body("id.id", equalTo(corporateId))
                .body("profileId", equalTo(corporateProfileId))
                .body("tag", equalTo(createCorporateModel.getTag()))
                .body("rootUser.id.type", equalTo("CORPORATE"))
                .body("rootUser.id.id", notNullValue())
                .body("rootUser.name", equalTo(createCorporateModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(createCorporateModel.getRootUser().getEmail()))
                .body("rootUser.mobile.countryCode", equalTo(createCorporateModel.getRootUser().getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(createCorporateModel.getRootUser().getMobile().getNumber()))
                .body("rootUser.companyPosition", equalTo(createCorporateModel.getRootUser().getCompanyPosition().name()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobileNumberVerified", equalTo(false))
                .body("rootUser.dateOfBirth.year", createCorporateModel.getRootUser().getDateOfBirth() == null ? nullValue() :
                        equalTo(createCorporateModel.getRootUser().getDateOfBirth().getYear()))
                .body("rootUser.dateOfBirth.month", createCorporateModel.getRootUser().getDateOfBirth() == null ? nullValue() :
                        equalTo(createCorporateModel.getRootUser().getDateOfBirth().getMonth()))
                .body("rootUser.dateOfBirth.day", createCorporateModel.getRootUser().getDateOfBirth() == null ? nullValue() :
                        equalTo(createCorporateModel.getRootUser().getDateOfBirth().getDay()))
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
                .body("sourceOfFundsOther", equalTo(createCorporateModel.getSourceOfFundsOther()))
                .body("acceptedTerms", equalTo(createCorporateModel.getAcceptedTerms()))
                .body("ipAddress", equalTo(createCorporateModel.getIpAddress()))
                .body("baseCurrency", equalTo(createCorporateModel.getBaseCurrency()))
                .body("feeGroup", equalTo("DEFAULT"))
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void GetCorporates_InvalidApiKey_Unauthorised(){

        CorporatesService.getCorporates("abc", authenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetCorporates_NoApiKey_BadRequest(){

        CorporatesService.getCorporates("", authenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetCorporates_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        CorporatesService.getCorporates(secretKey, authenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetCorporates_RootUserLoggedOut_Unauthorised(){

        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        CorporatesService.getCorporates(secretKey, token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetCorporates_BackofficeImpersonator_Forbidden(){
        CorporatesService.getCorporates(secretKey, getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }
}
