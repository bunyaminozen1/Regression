package opc.junit.admin.fees;

import opc.enums.opc.IdentityType;
import opc.enums.opc.InnovatorSetup;
import opc.enums.opc.ManufacturingState;
import opc.junit.admin.BaseSetupExtension;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.PhysicalCardAddressModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.testmodels.BalanceModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static opc.enums.opc.InstrumentType.PHYSICAL;
import static opc.enums.opc.InstrumentType.VIRTUAL;
import static opc.enums.opc.ManagedCardMode.DEBIT_MODE;
import static opc.enums.opc.ManagedCardMode.PREPAID_MODE;

@Execution(ExecutionMode.CONCURRENT)
public class BaseFeesSetup {
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
    protected static String adminImpersonatedTenantToken;
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

        adminImpersonatedTenantToken = AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin());
    }

    protected static ManagedCardDetails createPrepaidManagedCard(final String managedCardProfileId,
                                                                 final String currency,
                                                                 final String authenticationToken) {
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
                                                               final String authenticationToken) {
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
                                                                         final String authenticationToken) {
        final String managedAccountId =
                createManagedAccount(managedAccountProfileId, currency, authenticationToken);

        return createDebitManagedCard(managedCardProfileId, managedAccountId, authenticationToken);
    }

    protected static ManagedCardDetails createPhysicalPrepaidManagedCard(final String managedCardProfileId,
                                                                         final String currency,
                                                                         final String authenticationToken) {
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

    protected static Pair<String, CreateManagedAccountModel> transferFundsToCard(final String token,
                                                                                 final IdentityType identityType,
                                                                                 final String managedCardId,
                                                                                 final String currency,
                                                                                 final Long depositAmount,
                                                                                 final int transferCount) {

        return TestHelper
                .simulateManagedAccountDepositAndTransferToCard(identityType.equals(IdentityType.CONSUMER) ?
                                consumerManagedAccountsProfileId : corporateManagedAccountsProfileId,
                        transfersProfileId, managedCardId, currency, depositAmount, secretKey, token, transferCount);
    }

    protected static String createManagedAccount(final String managedAccountsProfileId, final String currency, final String token) {
        return ManagedAccountsHelper
                .createManagedAccount(CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(managedAccountsProfileId, currency).build(),
                        secretKey, token);
    }

    protected static BalanceModel simulateManagedAccountDeposit(final String managedAccountId,
                                                                final String currency,
                                                                final Long amount,
                                                                final String authenticationToken) {

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, authenticationToken);

        TestHelper.simulateManagedAccountDeposit(managedAccountId, currency,
                amount, secretKey, authenticationToken);

        return ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, authenticationToken);
    }
}
