package opc.junit.admin.managedcards;

import opc.enums.opc.CardBrand;
import opc.junit.helpers.admin.AdminHelper;
import opc.models.innovator.AbstractCreateManagedCardsProfileModel;
import opc.models.innovator.CreateDebitManagedCardsProfileModel;
import opc.models.innovator.CreateManagedCardsProfileV2Model;
import opc.models.innovator.DigitalWalletsEnabledModel;
import opc.services.admin.AdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

@Execution(ExecutionMode.SAME_THREAD)
public class CreateManagedCardProfileTests extends BaseManagedCardsSetup {

    @Test
    public void CreateProfile_WalletEnabledPropertyEnabled_Success(){

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, false, adminTenantImpersonationToken);

        final CreateManagedCardsProfileV2Model createManagedCardsProfile =
                getConsumerDebitModel(true, false);

        AdminService.createManagedCardsProfileV2(createManagedCardsProfile, adminTenantImpersonationToken, programmeId)
                .then()
                .statusCode(SC_OK)
                .body("debitManagedCardsProfile.managedCardsProfile.digitalWalletsEnabled.manualProvisioningEnabled", equalTo(true))
                .body("debitManagedCardsProfile.managedCardsProfile.digitalWalletsEnabled.pushProvisioningEnabled", equalTo(false))
                .body("innovatorDigitalWalletsEnabled.manualProvisioningEnabled", equalTo(true))
                .body("innovatorDigitalWalletsEnabled.pushProvisioningEnabled", equalTo(false));
    }

    @Test
    public void CreateProfile_WalletNotEnabledPropertyEnabled_Success(){

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, false, adminTenantImpersonationToken);

        final CreateManagedCardsProfileV2Model createManagedCardsProfile =
                getConsumerDebitModel(false, false);

        AdminService.createManagedCardsProfileV2(createManagedCardsProfile, adminTenantImpersonationToken, programmeId)
                .then()
                .statusCode(SC_OK)
                .body("debitManagedCardsProfile.managedCardsProfile.digitalWalletsEnabled.manualProvisioningEnabled", equalTo(false))
                .body("debitManagedCardsProfile.managedCardsProfile.digitalWalletsEnabled.pushProvisioningEnabled", equalTo(false))
                .body("innovatorDigitalWalletsEnabled.manualProvisioningEnabled", equalTo(true))
                .body("innovatorDigitalWalletsEnabled.pushProvisioningEnabled", equalTo(false));
    }

    @Test
    public void CreateProfile_WalletEnabledPropertyNotEnabled_WalletsNotEnabled(){

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                false, false, adminTenantImpersonationToken);

        final CreateManagedCardsProfileV2Model createManagedCardsProfile =
                getConsumerDebitModel(true, false);

        AdminService.createManagedCardsProfileV2(createManagedCardsProfile, adminTenantImpersonationToken, programmeId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("WALLETS_NOT_ENABLED"));
    }

    @Test
    public void CreateProfile_WalletNotEnabledPropertyNotEnabled_Success(){

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                false, false, adminTenantImpersonationToken);

        final CreateManagedCardsProfileV2Model createManagedCardsProfile =
                getConsumerDebitModel(false, false);

        AdminService.createManagedCardsProfileV2(createManagedCardsProfile, adminTenantImpersonationToken, programmeId)
                .then()
                .statusCode(SC_OK)
                .body("debitManagedCardsProfile.managedCardsProfile.digitalWalletsEnabled.manualProvisioningEnabled", equalTo(false))
                .body("debitManagedCardsProfile.managedCardsProfile.digitalWalletsEnabled.pushProvisioningEnabled", equalTo(false))
                .body("innovatorDigitalWalletsEnabled.manualProvisioningEnabled", equalTo(false))
                .body("innovatorDigitalWalletsEnabled.pushProvisioningEnabled", equalTo(false));
    }

    private CreateManagedCardsProfileV2Model getConsumerDebitModel(final boolean manualProvisioningEnabled, final boolean pushProvisioningEnabled){
        return CreateManagedCardsProfileV2Model.builder()
                .createDebitProfileRequest(CreateDebitManagedCardsProfileModel
                        .DefaultConsumerCreateDebitManagedCardsProfileModel()
                        .createManagedCardsProfileRequest(AbstractCreateManagedCardsProfileModel
                                .DefaultConsumerDebitCreateManagedCardsProfileModel()
                                .digitalWalletsEnabled(new DigitalWalletsEnabledModel(manualProvisioningEnabled, pushProvisioningEnabled))
                                .build())
                        .build())
                .cardFundingType("DEBIT")
                .build();
    }
}