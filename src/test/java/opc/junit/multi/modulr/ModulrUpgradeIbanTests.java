package opc.junit.multi.modulr;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import commons.enums.Currency;
import java.sql.SQLException;
import opc.enums.opc.IdentityType;
import opc.junit.database.ModulrDatabaseHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.innovator.UpdateManagedAccountProfileModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedAccountsService;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ModulrUpgradeIbanTests extends BaseModulrSetup {

  private static String innovatorToken;

  @BeforeAll
  public static void Setup() {
    innovatorToken = InnovatorHelper.loginInnovator(payneticsInnovatorEmail, payneticsInnovatorPassword);
    InnovatorService.updateManagedAccountProfile(new UpdateManagedAccountProfileModel.Builder().setProxyFiProvider("modulr").build(), payneticsCorporateManagedAccountProfileId, innovatorToken, payneticsProgrammeId);
  }

  @Test
  public void ModulrUpgradeIban_UpgradeIbanToModulrProxy_Success() throws SQLException {
    final CreateCorporateModel createCorporateModel = CreateCorporateModel.CurrencyCreateCorporateModel( payneticsCorporateProfileId, Currency.EUR).build();
    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, payneticsSecretKey);
    CorporatesHelper.verifyEmail(createCorporateModel.getRootUser().getEmail(), payneticsSecretKey);

    AdminService.getModulrSubscriptionPermission(adminToken, corporate.getLeft())
        .then()
        .statusCode(SC_OK)
        .body("subscriberId", equalTo(corporate.getLeft()))
        .body("subscriberType", equalTo(IdentityType.CORPORATE.getValue()))
        .body("status", equalTo("READY_TO_SUBSCRIBE"));

    assertEquals("READY_TO_SUBSCRIBE", ModulrDatabaseHelper.getSubscriber(corporate.getLeft()).get(0).get("status"));

    final CreateManagedAccountModel managedAccountModel = CreateManagedAccountModel.DefaultCreateManagedAccountModel
        (payneticsCorporateManagedAccountProfileId, createCorporateModel.getBaseCurrency()).build();


    final String managedAccountId = ManagedAccountsHelper.createManagedAccount(managedAccountModel, payneticsSecretKey, corporate.getRight());

    ManagedAccountsService.assignManagedAccountIban(payneticsSecretKey, managedAccountId, corporate.getRight())
        .then()
        .statusCode(SC_OK)
        .body("bankAccountDetails[0].beneficiaryBank", equalTo("SEPA - Saxo Payments A/S"))
        .body("bankAccountDetails[0].beneficiaryBankAddress", equalTo("SEPA - Hill Tower, 76A, James Bourchier Blvd., 1407 Sofia, Bulgaria"))
        .body("bankAccountDetails[0].beneficiaryNameAndSurname", notNullValue())
        .body("bankAccountDetails[0].details.code", equalTo("SEPA-SXPXXXXX"))
        .body("bankAccountDetails[0].details.iban", notNullValue())
        .body("bankAccountDetails[1].beneficiaryBank", equalTo("Dummy Irish Bank"))
        .body("bankAccountDetails[1].beneficiaryBankAddress", equalTo("Dummy Address, City, Post Code, IE"))
        .body("bankAccountDetails[1].beneficiaryNameAndSurname", notNullValue())
        .body("bankAccountDetails[1].details.accountNumber", notNullValue())
        .body("bankAccountDetails[1].details.sortCode", notNullValue())
        .body("bankAccountDetails[2].beneficiaryBank", equalTo("SEPA - Saxo Payments A/S"))
        .body("bankAccountDetails[2].beneficiaryBankAddress", equalTo("SEPA - Hill Tower, 76A, James Bourchier Blvd., 1407 Sofia, Bulgaria"))
        .body("bankAccountDetails[2].beneficiaryNameAndSurname", notNullValue())
        .body("bankAccountDetails[2].details.bankIdentifierCode", equalTo("SEPA-SXPXXXXX"))
        .body("bankAccountDetails[2].details.iban", notNullValue())
        .body("state", equalTo("ALLOCATED"));

    //check that we have created a proxy innovator iban on modulr side
    InnovatorService.getManagedAccount(managedAccountId, innovatorToken)
        .then()
        .statusCode(200) // Replace with your expected status code
        .body("id.type", equalTo("managed_accounts"))
        .body("id.id", equalTo(managedAccountId))
        .body("profileId", equalTo(payneticsCorporateManagedAccountProfileId))
        .body("owner.type", equalTo("corporates"))
        .body("owner.id", equalTo(corporate.getLeft()))
        .body("active", equalTo(true))
        .body("currency", equalTo("EUR"))
        .body("balances.availableBalance", equalTo("0"))
        .body("balances.actualBalance", equalTo("0"))
        .body("state.destroyType", equalTo(""))
        .body("bankAccountDetails[0].fiProvider", anyOf(equalTo("paynetics_eea"), equalTo("modulr")))
        .body("bankAccountDetails[1].fiProvider", anyOf(equalTo("paynetics_eea"), equalTo("modulr")))
        .body("fiProvider", equalTo("paynetics_eea"))
        .body("channelProvider", equalTo("gps"))
        .body("upgraded", equalTo(true))
        .body("processorAccountProduct", equalTo("5168"))
        .body("emiLicenseHolder", equalTo("NON_EMI"));
  }

  @Test
  public void ModulrUpgradeIban_AdminUpgradeIbanToModulrProxy_Success() throws SQLException {
    final CreateCorporateModel createCorporateModel = CreateCorporateModel.CurrencyCreateCorporateModel( payneticsCorporateProfileId, Currency.EUR).build();
    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, payneticsSecretKey);
    CorporatesHelper.verifyEmail(createCorporateModel.getRootUser().getEmail(), payneticsSecretKey);

    AdminService.getModulrSubscriptionPermission(adminToken, corporate.getLeft())
        .then()
        .statusCode(SC_OK)
        .body("subscriberId", equalTo(corporate.getLeft()))
        .body("subscriberType", equalTo(IdentityType.CORPORATE.getValue()))
        .body("status", equalTo("READY_TO_SUBSCRIBE"));

    assertEquals("READY_TO_SUBSCRIBE", ModulrDatabaseHelper.getSubscriber(corporate.getLeft()).get(0).get("status"));

    final CreateManagedAccountModel managedAccountModel = CreateManagedAccountModel.DefaultCreateManagedAccountModel
        (payneticsCorporateManagedAccountProfileId, createCorporateModel.getBaseCurrency()).build();


    final String managedAccountId = ManagedAccountsHelper.createManagedAccount(managedAccountModel, payneticsSecretKey, corporate.getRight());

    AdminService.upgradeManagedAccount(managedAccountId, adminToken)
            .then()
        .statusCode(SC_OK);

    //check that we have created a proxy innovator iban on modulr side
    InnovatorService.getManagedAccount(managedAccountId, innovatorToken)
        .then()
        .statusCode(200) // Replace with your expected status code
        .body("id.type", equalTo("managed_accounts"))
        .body("id.id", equalTo(managedAccountId))
        .body("profileId", equalTo(payneticsCorporateManagedAccountProfileId))
        .body("owner.type", equalTo("corporates"))
        .body("owner.id", equalTo(corporate.getLeft()))
        .body("active", equalTo(true))
        .body("currency", equalTo("EUR"))
        .body("balances.availableBalance", equalTo("0"))
        .body("balances.actualBalance", equalTo("0"))
        .body("state.destroyType", equalTo(""))
        .body("bankAccountDetails[0].fiProvider", anyOf(equalTo("paynetics_eea"), equalTo("modulr")))
        .body("bankAccountDetails[1].fiProvider", anyOf(equalTo("paynetics_eea"), equalTo("modulr")))
        .body("fiProvider", equalTo("paynetics_eea"))
        .body("channelProvider", equalTo("gps"))
        .body("upgraded", equalTo(true))
        .body("processorAccountProduct", equalTo("5168"))
        .body("emiLicenseHolder", equalTo("NON_EMI"));
  }
}