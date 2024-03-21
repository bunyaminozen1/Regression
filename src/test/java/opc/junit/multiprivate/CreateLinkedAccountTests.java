package opc.junit.multiprivate;

import commons.enums.Currency;
import commons.models.CompanyModel;
import opc.enums.opc.CountryCode;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.backoffice.IdentityModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multiprivate.RegisterLinkedAccountModel;
import opc.services.multiprivate.MultiPrivateService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class CreateLinkedAccountTests extends BaseMultiPrivateSetup {

    @Test
    public void createLinkedAccount_FPS_Success()
    {
        final CreateCorporateModel corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(pluginCorporateProfileId)
                        .setBaseCurrency(Currency.GBP.name())
                        .setCompany(CompanyModel.defaultCompanyModel()
                                .setRegistrationCountry(CountryCode.GB.name())
                                .build())
                        .build();

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateDetails, pluginSecretKey);
        final IdentityModel identityModel = new IdentityModel(corporate.getLeft(), IdentityType.CORPORATE);

        final RegisterLinkedAccountModel registerLinkedAccountModel = RegisterLinkedAccountModel.DefaultRegisterLinkedAccountFasterModel(identityModel, pluginProgrammeId, pluginCorporateLinkedManagedAccountProfileId, corporateDetails.getBaseCurrency()).build();

        MultiPrivateService.createLinkedAccount(registerLinkedAccountModel, fpiKey)
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(pluginCorporateLinkedManagedAccountProfileId))
                .body("tag", equalTo(registerLinkedAccountModel.getTag()))
                .body("friendlyName", equalTo(registerLinkedAccountModel.getFriendlyName()))
                .body("currency", equalTo(registerLinkedAccountModel.getCurrency()))
                .body("country", equalTo(registerLinkedAccountModel.getCountry()))
                .body("accountDetails.accountNumber", notNullValue())
                .body("accountDetails.sortCode", notNullValue());
    }

    @Test
    public void createLinkedAccount_Sepa_Success()
    {
        final CreateCorporateModel corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(pluginCorporateProfileId)
                        .setBaseCurrency(Currency.GBP.name())
                        .setCompany(CompanyModel.defaultCompanyModel()
                                .setRegistrationCountry(CountryCode.GB.name())
                                .build())
                        .build();

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateDetails, pluginSecretKey);
        final IdentityModel identityModel = new IdentityModel(corporate.getLeft(), IdentityType.CORPORATE);

        final RegisterLinkedAccountModel registerLinkedAccountModel = RegisterLinkedAccountModel.DefaultRegisterLinkedAccountSepaModel(identityModel, pluginProgrammeId, pluginCorporateLinkedManagedAccountProfileId, corporateDetails.getBaseCurrency()).build();

        MultiPrivateService.createLinkedAccount(registerLinkedAccountModel, fpiKey)
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(pluginCorporateLinkedManagedAccountProfileId))
                .body("tag", equalTo(registerLinkedAccountModel.getTag()))
                .body("friendlyName", equalTo(registerLinkedAccountModel.getFriendlyName()))
                .body("currency", equalTo(registerLinkedAccountModel.getCurrency()))
                .body("country", equalTo(registerLinkedAccountModel.getCountry()))
                .body("accountDetails.iban", notNullValue())
                .body("accountDetails.bankIdentifierCode", notNullValue());
    }

    @Test
    public void createLinkedAccount_CorporateNoToken_Unauthorised()
    {
        final CreateCorporateModel corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(pluginCorporateProfileId)
                        .setBaseCurrency(Currency.GBP.name())
                        .setCompany(CompanyModel.defaultCompanyModel()
                                .setRegistrationCountry(CountryCode.GB.name())
                                .build())
                        .build();

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateDetails, pluginSecretKey);
        final IdentityModel identityModel = new IdentityModel(corporate.getLeft(), IdentityType.CORPORATE);

        final RegisterLinkedAccountModel registerLinkedAccountModel = RegisterLinkedAccountModel.DefaultRegisterLinkedAccountSepaModel(identityModel, pluginProgrammeId, pluginCorporateLinkedManagedAccountProfileId, corporateDetails.getBaseCurrency()).build();

        MultiPrivateService.createLinkedAccount(registerLinkedAccountModel, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }
}
