package opc.junit.multiprivate;

import commons.enums.Currency;
import commons.models.CompanyModel;
import opc.database.LinkedAccountsDataBaseHelper;
import opc.enums.opc.CountryCode;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multiprivate.RegisterLinkedAccountModel;
import opc.models.multiprivate.SetActiveStateLinkedAccountModel;
import opc.services.multiprivate.MultiPrivateService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Map;

import static opc.junit.helpers.multi.CorporatesHelper.createAuthenticatedVerifiedCorporate;
import static opc.junit.helpers.multiprivate.MultiPrivateHelper.createLinkedManagedAccount;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PatchLinkedAccountTests extends BaseMultiPrivateSetup {
    private static String corporateId;
    private static String corporateCurrency;

    @BeforeAll
    public static void Setup() {
        corporateSetup();
    }

    @Test
    public void DeactivateLinkedAccount_Corporate_Success() throws SQLException {

        final Pair<String, RegisterLinkedAccountModel> linkedAccount =
                createLinkedManagedAccount(corporateId, corporateCurrency, pluginProgrammeId, pluginCorporateLinkedManagedAccountProfileId, fpiKey);

        deactivateLinkedAccount(linkedAccount.getLeft());

        final Map<Integer, Map<String, String>> getLinkedAccount =
                LinkedAccountsDataBaseHelper.getLinkedAccount(linkedAccount.getLeft());

        final Long stateLinkedAccount = Long.valueOf(getLinkedAccount.get(0).get("active"));
        assertEquals(stateLinkedAccount, 0);
    }

    @Test
    public void ActivateLinkedAccount_Corporate_Success() throws SQLException {

        final Pair<String, RegisterLinkedAccountModel> linkedAccount =
                createLinkedManagedAccount(corporateId, corporateCurrency, pluginProgrammeId, pluginCorporateLinkedManagedAccountProfileId, fpiKey);

        deactivateLinkedAccount(linkedAccount.getLeft());
        final Map<Integer, Map<String, String>> getLinkedAccount =
                LinkedAccountsDataBaseHelper.getLinkedAccount(linkedAccount.getLeft());
        final Long stateLinkedAccount = Long.valueOf(getLinkedAccount.get(0).get("active"));
        assertEquals(stateLinkedAccount, 0);

        activateLinkedAccount(linkedAccount.getLeft());
        final Map<Integer, Map<String, String>> getLinkedAccountWithActiveState =
                LinkedAccountsDataBaseHelper.getLinkedAccount(linkedAccount.getLeft());
        final Long activeStateLinkedAccount = Long.valueOf(getLinkedAccountWithActiveState.get(0).get("active"));
        assertEquals(activeStateLinkedAccount, 1);
    }

    @Test
    public void DeactivateLinkedAccount_CorporateUnknownLinkedAccountId_Conflict() {

        MultiPrivateService.setActiveStateLinkedAccount
                (RandomStringUtils.randomNumeric(18), SetActiveStateLinkedAccountModel.DefaultSetActiveStateLinkedAccountModel(false).build(), fpiKey)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void DeactivateLinkedAccount_CorporateRandomToken_Unauthorised() {

        final Pair<String, RegisterLinkedAccountModel> linkedAccount =
                createLinkedManagedAccount(corporateId, corporateCurrency, pluginProgrammeId, pluginCorporateLinkedManagedAccountProfileId, fpiKey);

        MultiPrivateService.setActiveStateLinkedAccount
                        (linkedAccount.getLeft(), SetActiveStateLinkedAccountModel.DefaultSetActiveStateLinkedAccountModel(false).build(), RandomStringUtils.randomAlphabetic(6))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void DeactivateLinkedAccount_CorporateNoToken_Unauthorised() {

        final Pair<String, RegisterLinkedAccountModel> linkedAccount =
                createLinkedManagedAccount(corporateId, corporateCurrency, pluginProgrammeId, pluginCorporateLinkedManagedAccountProfileId, fpiKey);

        MultiPrivateService.setActiveStateLinkedAccount
                        (linkedAccount.getLeft(), SetActiveStateLinkedAccountModel.DefaultSetActiveStateLinkedAccountModel(false).build(), "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    private static void deactivateLinkedAccount(final String linkedAccountId){

        MultiPrivateService.setActiveStateLinkedAccount
                (linkedAccountId, SetActiveStateLinkedAccountModel.DefaultSetActiveStateLinkedAccountModel(false).build(), fpiKey)
                .then()
                .statusCode(SC_OK);
    }

    //add 2 tests with cross identities once QA env will set up'ed

    private static void activateLinkedAccount(final String linkedAccountId){

        MultiPrivateService.setActiveStateLinkedAccount
                (linkedAccountId, SetActiveStateLinkedAccountModel.DefaultSetActiveStateLinkedAccountModel(true).build(), fpiKey)
                .then()
                .statusCode(SC_OK);
    }

    private static void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(pluginCorporateProfileId)
                        .setBaseCurrency(Currency.GBP.toString())
                        .setCompany(CompanyModel.defaultCompanyModel()
                                .setRegistrationCountry(CountryCode.GB.name())
                                .build())
                        .build();

        final Pair<String, String> authenticatedCorporate = createAuthenticatedVerifiedCorporate(createCorporateModel, pluginSecretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateCurrency = createCorporateModel.getBaseCurrency();
    }
}
