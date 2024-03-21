package opc.junit.admin.managedcards;

import opc.enums.opc.CardBrand;
import opc.junit.helpers.admin.AdminHelper;
import opc.models.innovator.AbstractCreateManagedCardsProfileModel;
import opc.models.innovator.AbstractUpdateManagedCardsProfileModel;
import opc.models.innovator.CreateDebitManagedCardsProfileModel;
import opc.models.innovator.CreateManagedCardsProfileV2Model;
import opc.models.innovator.DigitalWalletsEnabledModel;
import opc.models.innovator.UpdateDebitManagedCardsProfileModel;
import opc.models.innovator.UpdateManagedCardsProfileV2Model;
import opc.services.admin.AdminService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

@Execution(ExecutionMode.SAME_THREAD)
public class UpdateManagedCardProfileTests extends BaseManagedCardsSetup {

    private static String profileId;

    @BeforeAll
    public static void Setup(){

        profileId =
                AdminService.createManagedCardsProfileV2(getConsumerDebitModel(), adminTenantImpersonationToken, programmeId)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("debitManagedCardsProfile.managedCardsProfile.profile.id");
    }

    @Test
    public void UpdateProfile_WalletEnabledPropertyEnabled_Success(){

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, false, AdminService.loginAdmin());

        final UpdateManagedCardsProfileV2Model updateManagedCardsProfile =
                getUpdateManagedCardProfileModel(true, false);

        AdminService.updateManagedCardsProfileV2(updateManagedCardsProfile, adminTenantImpersonationToken, programmeId, profileId)
                .then()
                .statusCode(SC_OK)
                .body("debitManagedCardsProfile.managedCardsProfile.digitalWalletsEnabled.manualProvisioningEnabled", equalTo(true))
                .body("debitManagedCardsProfile.managedCardsProfile.digitalWalletsEnabled.pushProvisioningEnabled", equalTo(false))
                .body("innovatorDigitalWalletsEnabled.manualProvisioningEnabled", equalTo(true))
                .body("innovatorDigitalWalletsEnabled.pushProvisioningEnabled", equalTo(false));
    }

    @Test
    public void UpdateProfile_WalletNotEnabledPropertyEnabled_Success(){

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, false, AdminService.loginAdmin());

        final UpdateManagedCardsProfileV2Model updateManagedCardsProfile =
                getUpdateManagedCardProfileModel(false, false);

        AdminService.updateManagedCardsProfileV2(updateManagedCardsProfile, adminTenantImpersonationToken, programmeId, profileId)
                .then()
                .statusCode(SC_OK)
                .body("debitManagedCardsProfile.managedCardsProfile.digitalWalletsEnabled.manualProvisioningEnabled", equalTo(false))
                .body("debitManagedCardsProfile.managedCardsProfile.digitalWalletsEnabled.pushProvisioningEnabled", equalTo(false))
                .body("innovatorDigitalWalletsEnabled.manualProvisioningEnabled", equalTo(true))
                .body("innovatorDigitalWalletsEnabled.pushProvisioningEnabled", equalTo(false));
    }

    @Test
    public void UpdateProfile_WalletEnabledPropertyNotEnabled_WalletsNotEnabled(){

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                false, false, AdminService.loginAdmin());

        final UpdateManagedCardsProfileV2Model updateManagedCardsProfile =
                getUpdateManagedCardProfileModel(true, false);

        AdminService.updateManagedCardsProfileV2(updateManagedCardsProfile, adminTenantImpersonationToken, programmeId, profileId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("WALLETS_NOT_ENABLED"));
    }

    @Test
    public void UpdateProfile_WalletNotEnabledPropertyNotEnabled_Success(){

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                false, false, AdminService.loginAdmin());

        final UpdateManagedCardsProfileV2Model updateManagedCardsProfile =
                getUpdateManagedCardProfileModel(false, false);

        AdminService.updateManagedCardsProfileV2(updateManagedCardsProfile, adminTenantImpersonationToken, programmeId, profileId)
                .then()
                .statusCode(SC_OK)
                .body("debitManagedCardsProfile.managedCardsProfile.digitalWalletsEnabled.manualProvisioningEnabled", equalTo(false))
                .body("debitManagedCardsProfile.managedCardsProfile.digitalWalletsEnabled.pushProvisioningEnabled", equalTo(false))
                .body("innovatorDigitalWalletsEnabled.manualProvisioningEnabled", equalTo(false))
                .body("innovatorDigitalWalletsEnabled.pushProvisioningEnabled", equalTo(false));
    }

    private static CreateManagedCardsProfileV2Model getConsumerDebitModel(){
        return CreateManagedCardsProfileV2Model.builder()
                .createDebitProfileRequest(CreateDebitManagedCardsProfileModel
                        .DefaultConsumerCreateDebitManagedCardsProfileModel()
                        .createManagedCardsProfileRequest(AbstractCreateManagedCardsProfileModel
                                .DefaultConsumerDebitCreateManagedCardsProfileModel()
                                .build())
                        .build())
                .cardFundingType("DEBIT")
                .build();
    }

    private static UpdateManagedCardsProfileV2Model getUpdateManagedCardProfileModel(final boolean isManualProvisioningEnabled,
                                                                                     final boolean isPushProvisioningEnabled){
        return UpdateManagedCardsProfileV2Model
                .builder()
                .setUpdateDebitProfileRequest(UpdateDebitManagedCardsProfileModel
                        .builder()
                        .setUpdateManagedCardsProfileRequest(AbstractUpdateManagedCardsProfileModel.builder()
                                .setDigitalWalletsEnabled(new DigitalWalletsEnabledModel(isManualProvisioningEnabled, isPushProvisioningEnabled))
                                .build())
                        .build())
                .setCardFundingType("DEBIT")
                .build();
    }
}
