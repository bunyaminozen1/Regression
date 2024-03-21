package opc.junit.multi.multipleapps;

import opc.enums.opc.LimitInterval;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.SpendLimitModel;
import opc.models.multi.managedcards.SpendRulesModel;
import opc.models.shared.CurrencyAmount;
import opc.models.simulator.SimulateCardPurchaseByIdModel;
import opc.services.multi.ConsumersService;
import opc.services.multi.CorporatesService;
import opc.services.simulator.SimulatorService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

public class SimulatorTests extends BaseApplicationsSetup {

    @Test
    public void VerifyKyc_OtherApplicationKey_Forbidden(){

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(applicationTwo.getConsumersProfileId()).build();

        final String consumerId =
                ConsumersService.createConsumer(createConsumerModel, applicationTwo.getSecretKey(), Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath().getString("id.id");

        SimulatorService.simulateKycApproval(applicationFour.getSecretKey(), consumerId)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void VerifyKyb_OtherApplicationKey_Forbidden(){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationTwo.getCorporatesProfileId()).build();

        final String corporateId =
                CorporatesService.createCorporate(createCorporateModel, applicationTwo.getSecretKey(), Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath().getString("id.id");

        SimulatorService.simulateKybApproval(applicationFour.getSecretKey(), corporateId)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void SimulatePurchase_OtherApplicationKey_Forbidden(){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationTwo.getCorporatesProfileId()).build();

        final String corporateToken =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, applicationTwo.getSecretKey())
                .getRight();

        final String managedAccountId = createManagedAccount(applicationTwo.getCorporatePayneticsEeaManagedAccountsProfileId(),
                createCorporateModel.getBaseCurrency(), corporateToken, applicationTwo.getSecretKey()).getLeft();

        simulateManagedAccountDepositAndCheckBalance(managedAccountId, createCorporateModel.getBaseCurrency(), 1000L,
                applicationTwo.getSecretKey(), corporateToken);

        final String managedCardId = createDebitManagedCard(applicationTwo.getCorporateNitecrestEeaDebitManagedCardsProfileId(),
                managedAccountId, corporateToken, applicationTwo.getSecretKey()).getLeft();

        setSpendLimit(managedCardId,
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(createCorporateModel.getBaseCurrency(), 1000L), LimitInterval.ALWAYS)),
                corporateToken, applicationTwo.getSecretKey());

        final SimulateCardPurchaseByIdModel simulateCardPurchaseModel =
                SimulateCardPurchaseByIdModel.builder()
                        .setTransactionAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), 100L))
                        .build();

        SimulatorService.simulateCardPurchaseById(simulateCardPurchaseModel, managedCardId, applicationFour.getSecretKey())
                .then()
                .statusCode(SC_OK)
                .body("code", not(equalTo("APPROVED")));
    }

    private void setSpendLimit(final String managedCardId,
                               final List<SpendLimitModel> spendLimit,
                               final String authenticationToken,
                               final String secretKey){
        final SpendRulesModel spendRulesModel = getDefaultSpendRulesModel(spendLimit)
                .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCardId, authenticationToken);
    }

    private SpendRulesModel.Builder getDefaultSpendRulesModel(final List<SpendLimitModel> spendLimit){
        return SpendRulesModel
                .builder()
                .setAllowedMerchantCategories(new ArrayList<>())
                .setBlockedMerchantCategories(Arrays.asList("7995", "1234"))
                .setAllowedMerchantIds(new ArrayList<>())
                .setBlockedMerchantIds(new ArrayList<>())
                .setAllowContactless(true)
                .setAllowAtm(true)
                .setAllowECommerce(true)
                .setAllowCashback(true)
                .setAllowCreditAuthorisations(true)
                .setSpendLimit(spendLimit);
    }
}
