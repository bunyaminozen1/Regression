package opc.junit.multi.modulr;

import commons.enums.Currency;
import opc.enums.opc.IdentityType;
import opc.junit.database.ModulrDatabaseHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.admin.SwitchModulrSubscriptionFeatureModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.services.admin.AdminService;
import opc.services.multi.ManagedAccountsService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ModulrSubscriptionTests extends BaseModulrSetup{

    @Test
    public void ModulrSubscription_ReadyToSubscribeAsDefault_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.CurrencyCreateCorporateModel(corporateProfileId, Currency.GBP).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyEmail(createCorporateModel.getRootUser().getEmail(), secretKey);

        AdminService.getModulrSubscriptionPermission(adminToken, corporate.getLeft())
                .then()
                .statusCode(SC_OK)
                .body("subscriberId", equalTo(corporate.getLeft()))
                .body("subscriberType", equalTo(IdentityType.CORPORATE.getValue()))
                .body("status", equalTo("READY_TO_SUBSCRIBE"));

        assertEquals("READY_TO_SUBSCRIBE", ModulrDatabaseHelper.getSubscriber(corporate.getLeft()).get(0).get("status"));

        final CreateManagedAccountModel managedAccountModel = CreateManagedAccountModel.DefaultCreateManagedAccountModel
                (corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency()).build();

        ManagedAccountsService.createManagedAccount(managedAccountModel, secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        assertEquals("APPROVED", ModulrDatabaseHelper.getSubscriber(corporate.getLeft()).get(0).get("status"));
    }

    @Test
    public void ModulrSubscription_SwitchPermissionAfterApproval_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.CurrencyCreateCorporateModel(corporateProfileId, Currency.GBP).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyEmail(createCorporateModel.getRootUser().getEmail(), secretKey);

        AdminService.getModulrSubscriptionPermission(adminToken, corporate.getLeft())
                .then()
                .statusCode(SC_OK)
                .body("subscriberId", equalTo(corporate.getLeft()))
                .body("subscriberType", equalTo(IdentityType.CORPORATE.getValue()))
                .body("status", equalTo("READY_TO_SUBSCRIBE"));

        assertEquals("READY_TO_SUBSCRIBE", ModulrDatabaseHelper.getSubscriber(corporate.getLeft()).get(0).get("status"));

        AdminService.switchModulrSubscriptionPermission(adminToken, corporate.getLeft(), new SwitchModulrSubscriptionFeatureModel(false))
                .then()
                .statusCode(SC_OK)
                .body("subscriberId", equalTo(corporate.getLeft()))
                .body("subscriberType", equalTo(IdentityType.CORPORATE.getValue()))
                .body("status", equalTo("NOT_READY"));

        assertEquals("UNREGISTERED", ModulrDatabaseHelper.getSubscriber(corporate.getLeft()).get(0).get("status"));

        final CreateManagedAccountModel managedAccountModel = CreateManagedAccountModel.DefaultCreateManagedAccountModel
                (corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency()).build();

        //Error code will be decided by B&V team later
        ManagedAccountsService.createManagedAccount(managedAccountModel, secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_SERVICE_UNAVAILABLE);

        AdminService.switchModulrSubscriptionPermission(adminToken, corporate.getLeft(), new SwitchModulrSubscriptionFeatureModel(true))
                .then()
                .statusCode(SC_OK)
                .body("subscriberId", equalTo(corporate.getLeft()))
                .body("subscriberType", equalTo(IdentityType.CORPORATE.getValue()))
                .body("status", equalTo("READY_TO_SUBSCRIBE"));

        assertEquals("READY_TO_SUBSCRIBE", ModulrDatabaseHelper.getSubscriber(corporate.getLeft()).get(0).get("status"));

        ManagedAccountsService.createManagedAccount(managedAccountModel, secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        assertEquals("APPROVED", ModulrDatabaseHelper.getSubscriber(corporate.getLeft()).get(0).get("status"));
    }
}
