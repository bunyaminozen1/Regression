package opc.junit.backoffice.managedcards;

import opc.enums.opc.FeeType;
import opc.enums.opc.IdentityType;
import opc.enums.opc.InnovatorSetup;
import opc.enums.opc.InstrumentType;
import opc.enums.opc.ManufacturingState;
import opc.junit.backoffice.BaseSetupExtension;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.admin.SpendRulesModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.PhysicalCardAddressModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.simulator.DetokenizeModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.admin.AdminService;
import opc.services.simulator.SimulatorService;
import opc.tags.MultiBackofficeTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static opc.enums.opc.InstrumentType.PHYSICAL;
import static opc.enums.opc.InstrumentType.VIRTUAL;
import static opc.enums.opc.ManagedCardMode.DEBIT_MODE;
import static opc.enums.opc.ManagedCardMode.PREPAID_MODE;
import static org.apache.http.HttpStatus.SC_OK;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiBackofficeTags.MULTI_BACKOFFICE_INSTRUMENTS)
public class BaseManagedCardsSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String corporatePrepaidManagedCardsProfileId;
    protected static String consumerPrepaidManagedCardsProfileId;
    protected static String corporateDebitManagedCardsProfileId;
    protected static String consumerDebitManagedCardsProfileId;
    protected static String corporateManagedAccountsProfileId;
    protected static String consumerManagedAccountsProfileId;
    protected static String secretKey;
    protected static String innovatorId;
    protected static String innovatorEmail;
    protected static String innovatorPassword;
    protected static String innovatorToken;
    protected static String programmeId;

    private static String transfersProfileId;

    @BeforeAll
    public static void GlobalSetup() {
        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);

        innovatorId = applicationOne.getInnovatorId();
        innovatorEmail = applicationOne.getInnovatorEmail();
        innovatorPassword = applicationOne.getInnovatorPassword();

        corporateProfileId = applicationOne.getCorporatesProfileId();
        consumerProfileId = applicationOne.getConsumersProfileId();

        corporatePrepaidManagedCardsProfileId = applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        consumerPrepaidManagedCardsProfileId = applicationOne.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        corporateDebitManagedCardsProfileId = applicationOne.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        consumerDebitManagedCardsProfileId = applicationOne.getConsumerNitecrestEeaDebitManagedCardsProfileId();
        corporateManagedAccountsProfileId = applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountsProfileId = applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId();

        secretKey = applicationOne.getSecretKey();
        programmeId = applicationOne.getProgrammeId();
        transfersProfileId = applicationOne.getTransfersProfileId();

        innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
        final String adminToken = AdminService.loginAdmin();

        final SpendRulesModel spendRulesModel =
                SpendRulesModel
                        .builder()
                        .setAllowedMerchantCategories(new ArrayList<>())
                        .setBlockedMerchantCategories(Arrays.asList("7995", "1234"))
                        .setAllowedMerchantIds(new ArrayList<>())
                        .setBlockedMerchantIds(new ArrayList<>())
                        .setAllowContactless("TRUE")
                        .setAllowAtm("TRUE")
                        .setAllowECommerce("TRUE")
                        .setAllowCashback("TRUE")
                        .setAllowCreditAuthorisations("TRUE")
                        .setAllowedMerchantCountries(Collections.singletonList("MT"))
                        .setBlockedMerchantCountries(Collections.singletonList("IT"))
                        .build();

        AdminHelper.setProfileSpendRules(spendRulesModel, adminToken, programmeId, consumerDebitManagedCardsProfileId);
        AdminHelper.setProfileSpendRules(spendRulesModel, adminToken, programmeId, consumerPrepaidManagedCardsProfileId);
        AdminHelper.setProfileSpendRules(spendRulesModel, adminToken, programmeId, corporateDebitManagedCardsProfileId);
        AdminHelper.setProfileSpendRules(spendRulesModel, adminToken, programmeId, corporatePrepaidManagedCardsProfileId);
    }

    protected static List<ManagedCardDetails> createVirtualAndPhysicalCards(final String prepaidManagedCardProfileId,
                                                                            final String debitManagedCardProfileId,
                                                                            final String currency,
                                                                            final String parentManagedAccountId,
                                                                            final String authenticationToken){

        final List<ManagedCardDetails> managedCards = new ArrayList<>();
        managedCards.add(createPrepaidManagedCard(prepaidManagedCardProfileId, currency, authenticationToken));
        managedCards.add(createPhysicalPrepaidManagedCard(prepaidManagedCardProfileId, currency, authenticationToken));
        managedCards.add(createDebitManagedCard(debitManagedCardProfileId, parentManagedAccountId, authenticationToken));
        managedCards.add(createPhysicalDebitManagedCard(debitManagedCardProfileId, parentManagedAccountId, authenticationToken));
        managedCards.add(createPhysicalNonActivatedManagedCard(prepaidManagedCardProfileId, currency, authenticationToken));

        return managedCards;
    }

    protected static ManagedCardDetails createPrepaidManagedCard(final String managedCardProfileId,
                                                                 final String currency,
                                                                 final String authenticationToken){
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreatePrepaidManagedCardModel(managedCardProfileId,
                                currency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, authenticationToken);

        return ManagedCardDetails.builder()
                .setManagedCardId(managedCardId)
                .setManagedCardModel(createManagedCardModel)
                .setManagedCardMode(PREPAID_MODE)
                .setInstrumentType(VIRTUAL)
                .build();
    }

    protected static ManagedCardDetails createDebitManagedCard(final String managedCardProfileId,
                                                               final String managedAccountId,
                                                               final String authenticationToken){
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreateDebitManagedCardModel(managedCardProfileId,
                                managedAccountId)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, authenticationToken);

        return ManagedCardDetails.builder()
                .setManagedCardId(managedCardId)
                .setManagedCardModel(createManagedCardModel)
                .setManagedCardMode(DEBIT_MODE)
                .setInstrumentType(VIRTUAL)
                .build();
    }

    protected static ManagedCardDetails createManagedAccountAndDebitCard(final String managedAccountProfileId,
                                                                         final String managedCardProfileId,
                                                                         final String currency,
                                                                         final String authenticationToken){
        final String managedAccountId =
                createManagedAccount(managedAccountProfileId, currency, authenticationToken);

        return createDebitManagedCard(managedCardProfileId, managedAccountId, authenticationToken);
    }

    protected static ManagedCardDetails createFundedManagedAccountAndDebitCard(final String managedAccountProfileId,
                                                                               final String managedCardProfileId,
                                                                               final String currency,
                                                                               final String authenticationToken){

        return createFundedManagedAccountAndDebitCard(managedAccountProfileId, managedCardProfileId,
                currency, authenticationToken, 10000L);
    }

    protected static ManagedCardDetails createFundedManagedAccountAndDebitCard(final String managedAccountProfileId,
                                                                               final String managedCardProfileId,
                                                                               final String currency,
                                                                               final String authenticationToken,
                                                                               final long depositAmount){
        final String managedAccountId =
                createManagedAccount(managedAccountProfileId, currency, authenticationToken);

        TestHelper.simulateManagedAccountDeposit(managedAccountId, currency, depositAmount, secretKey, authenticationToken);

        final int balance = (int) (depositAmount - TestHelper.getFees(currency).get(FeeType.DEPOSIT_FEE).getAmount());

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreateDebitManagedCardModel(managedCardProfileId,
                                managedAccountId)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, authenticationToken);

        return ManagedCardDetails.builder()
                .setManagedCardId(managedCardId)
                .setManagedCardModel(createManagedCardModel)
                .setManagedCardMode(DEBIT_MODE)
                .setInstrumentType(VIRTUAL)
                .setInitialManagedAccountBalance(balance)
                .build();
    }

    protected static ManagedCardDetails createFundedManagedAccountAndPhysicalDebitCard(final String managedAccountProfileId,
                                                                                       final String managedCardProfileId,
                                                                                       final String currency,
                                                                                       final String authenticationToken){
        final String managedAccountId =
                createManagedAccount(managedAccountProfileId, currency, authenticationToken);

        TestHelper.simulateManagedAccountDeposit(managedAccountId, currency, 10000L, secretKey, authenticationToken);

        return createPhysicalDebitManagedCard(managedCardProfileId, managedAccountId, authenticationToken);
    }

    protected static ManagedCardDetails createManagedAccountAndPhysicalDebitCard(final String managedAccountProfileId,
                                                                                 final String managedCardProfileId,
                                                                                 final String currency,
                                                                                 final String authenticationToken){
        final String managedAccountId =
                createManagedAccount(managedAccountProfileId, currency, authenticationToken);

        return createPhysicalDebitManagedCard(managedCardProfileId, managedAccountId, authenticationToken);
    }

    protected static ManagedCardDetails createPhysicalPrepaidManagedCard(final String managedCardProfileId,
                                                                         final String currency,
                                                                         final String authenticationToken){
        final PhysicalCardAddressModel physicalCardAddressModel =
                PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(managedCardProfileId, currency, authenticationToken);

        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(),
                authenticationToken, physicalCardAddressModel);

        return ManagedCardDetails.builder()
                .setManagedCardId(managedCard.getManagedCardId())
                .setManagedCardModel(managedCard.getManagedCardModel())
                .setManagedCardMode(PREPAID_MODE)
                .setInstrumentType(PHYSICAL)
                .setPhysicalCardAddressModel(physicalCardAddressModel)
                .setManufacturingState(ManufacturingState.DELIVERED)
                .build();
    }

    protected static ManagedCardDetails createPhysicalDebitManagedCard(final String managedCardProfileId,
                                                                       final String managedAccountId,
                                                                       final String authenticationToken){
        final PhysicalCardAddressModel physicalCardAddressModel =
                PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();

        final ManagedCardDetails managedCard =
                createDebitManagedCard(managedCardProfileId, managedAccountId, authenticationToken);

        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(),
                authenticationToken, physicalCardAddressModel);

        return ManagedCardDetails.builder()
                .setManagedCardId(managedCard.getManagedCardId())
                .setManagedCardModel(managedCard.getManagedCardModel())
                .setManagedCardMode(DEBIT_MODE)
                .setInstrumentType(PHYSICAL)
                .setPhysicalCardAddressModel(physicalCardAddressModel)
                .setManufacturingState(ManufacturingState.DELIVERED)
                .build();
    }

    protected static ManagedCardDetails createPhysicalNonActivatedManagedCard(final String managedCardProfileId,
                                                                              final String currency,
                                                                              final String authenticationToken){
        final PhysicalCardAddressModel physicalCardAddressModel =
                PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(managedCardProfileId, currency, authenticationToken);

        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(),
                authenticationToken, physicalCardAddressModel);

        return ManagedCardDetails.builder()
                .setManagedCardId(managedCard.getManagedCardId())
                .setManagedCardModel(managedCard.getManagedCardModel())
                .setManagedCardMode(PREPAID_MODE)
                .setInstrumentType(VIRTUAL)
                .setPhysicalCardAddressModel(physicalCardAddressModel)
                .setManufacturingState(ManufacturingState.REQUESTED)
                .build();
    }

    protected static List<InstrumentType> getInstrumentTypes(){
        return Arrays.asList(VIRTUAL, PHYSICAL);
    }

    protected static Pair<String, CreateManagedAccountModel> transferFundsToCard(final String token,
                                                                                 final IdentityType identityType,
                                                                                 final String managedCardId,
                                                                                 final String currency,
                                                                                 final Long depositAmount,
                                                                                 final int transferCount){

        return TestHelper
                .simulateManagedAccountDepositAndTransferToCard(identityType.equals(IdentityType.CONSUMER) ?
                                consumerManagedAccountsProfileId : corporateManagedAccountsProfileId,
                        transfersProfileId, managedCardId, currency, depositAmount, secretKey, token, transferCount);
    }

    protected static String createManagedAccount(final String managedAccountsProfileId, final String currency, final String token){
        return ManagedAccountsHelper
                .createManagedAccount(CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(managedAccountsProfileId, currency).build(),
                        secretKey, token);
    }

    protected String getCardNumber(final String encryptedCardNumber,
                                   final String authenticationToken){
        return TestHelper.ensureAsExpected(15,
                () -> SimulatorService.detokenize(secretKey,
                        new DetokenizeModel(encryptedCardNumber, "CARD_NUMBER"),
                        authenticationToken), SC_OK)
                .jsonPath().get("value");
    }

    protected String getCvv(final String encryptedCvv,
                            final String authenticationToken){
        return TestHelper.ensureAsExpected(15,
                () -> SimulatorService.detokenize(secretKey,
                        new DetokenizeModel(encryptedCvv, "CARD_NUMBER"),
                        authenticationToken), SC_OK)
                .jsonPath().get("value");
    }
}
