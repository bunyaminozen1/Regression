package opc.junit.multi.managedcards;

import commons.enums.Currency;
import opc.enums.opc.CardBureau;
import opc.enums.opc.DefaultTimeoutDecision;
import opc.enums.opc.IdentityType;
import opc.enums.opc.InnovatorSetup;
import opc.enums.opc.InstrumentType;
import opc.enums.opc.ManufacturingState;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.multi.BaseSetupExtension;
import opc.models.innovator.AbstractCreateManagedCardsProfileModel;
import opc.models.innovator.CreateDebitManagedCardsProfileModel;
import opc.models.innovator.CreateManagedCardsProfileV2Model;
import opc.models.innovator.CreatePrepaidManagedCardsProfileModel;
import opc.models.innovator.UpdateProgrammeModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.AuthForwardingModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.PhysicalCardAddressModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.admin.AdminService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.provider.Arguments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static opc.enums.opc.InstrumentType.PHYSICAL;
import static opc.enums.opc.InstrumentType.VIRTUAL;
import static opc.enums.opc.ManagedCardMode.DEBIT_MODE;
import static opc.enums.opc.ManagedCardMode.PREPAID_MODE;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiTags.MULTI)
public class BaseManagedCardsSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static ProgrammeDetailsModel nonFpsEnabledTenantDetails;
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

    protected static String adminToken;
    protected static String adminImpersonatedTenantToken;

    private static String transfersProfileId;

    protected static final String DEFAULT_ARTWORK_REFERENCE = "WEAVR";
    protected static final String DEFAULT_MASTERCARD_ARTWORK_REFERENCE = "WEAVRMC";
    protected static final String MULTI_ARTWORK_REFERENCE = "MULTI";
    protected static final String MULTI_MASTERCARD_ARTWORK_REFERENCE = "MULTIMC";
    protected static final String MULTI_MASTERCARD_ARTWORK_REFERENCE2 = "MULTIMC2";

    @BeforeAll
    public static void GlobalSetup() {

        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        nonFpsEnabledTenantDetails = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.NON_FPS_ENABLED_TENANT);

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

        adminToken = AdminService.loginAdmin();
        adminImpersonatedTenantToken = AdminService.impersonateTenant(innovatorId, adminToken);
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

    protected static ManagedCardDetails createPrepaidAuthForwardingEnabledManagedCard(final String managedCardProfileId,
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
        return createManagedAccountAndPhysicalDebitCard(managedAccountProfileId, managedCardProfileId, currency, authenticationToken, CardBureau.NITECREST);
    }

    protected static ManagedCardDetails createManagedAccountAndPhysicalDebitCard(final String managedAccountProfileId,
                                                                                 final String managedCardProfileId,
                                                                                 final String currency,
                                                                                 final String authenticationToken,
                                                                                 final CardBureau cardBureau){
        final String managedAccountId =
                createManagedAccount(managedAccountProfileId, currency, authenticationToken);

        return createPhysicalDebitManagedCard(managedCardProfileId, managedAccountId, authenticationToken, cardBureau);
    }

    protected static ManagedCardDetails createPhysicalPrepaidManagedCard(final String managedCardProfileId,
                                                                         final String currency,
                                                                         final String authenticationToken){
        return createPhysicalPrepaidManagedCard(managedCardProfileId, currency, authenticationToken, CardBureau.NITECREST);
    }

    protected static ManagedCardDetails createPhysicalPrepaidManagedCard(final String managedCardProfileId,
                                                                         final String currency,
                                                                         final String authenticationToken,
                                                                         final CardBureau cardBureau){
        final PhysicalCardAddressModel physicalCardAddressModel =
                PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(managedCardProfileId, currency, authenticationToken);

        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(),
                authenticationToken, physicalCardAddressModel, cardBureau);

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
        return createPhysicalDebitManagedCard(managedCardProfileId, managedAccountId, authenticationToken, CardBureau.NITECREST);
    }

    protected static ManagedCardDetails createPhysicalDebitManagedCard(final String managedCardProfileId,
                                                                       final String managedAccountId,
                                                                       final String authenticationToken,
                                                                       final CardBureau cardBureau){
        final PhysicalCardAddressModel physicalCardAddressModel =
                PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();

        final ManagedCardDetails managedCard =
                createDebitManagedCard(managedCardProfileId, managedAccountId, authenticationToken);

        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(),
                authenticationToken, physicalCardAddressModel, cardBureau);

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

    protected static ManagedCardDetails createPhysicalNonActivatedManagedCard(final String managedCardProfileId,
                                                                              final String currency,
                                                                              final String authenticationToken,
                                                                              final CardBureau cardBureau){
        final PhysicalCardAddressModel physicalCardAddressModel =
                PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(managedCardProfileId, currency, authenticationToken);

        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(),
                authenticationToken, physicalCardAddressModel, cardBureau);

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

    protected static List<DefaultTimeoutDecision> getDefaultTimeoutDecision(){
        return Arrays.asList(DefaultTimeoutDecision.APPROVE, DefaultTimeoutDecision.DECLINE);
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

    protected static Pair<String, CreateManagedAccountModel> createPendingApprovalManagedAccount(final String managedAccountProfileId,
                                                                                                 final String authenticationToken){
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(managedAccountProfileId,
                                Currency.GBP)
                        .build();

        final String managedAccountId =
                ManagedAccountsHelper.createPendingApprovalManagedAccount(createManagedAccountModel, secretKey, authenticationToken);

        return Pair.of(managedAccountId, createManagedAccountModel);
    }

    protected static String getBackofficeImpersonateToken(final String identityId, final IdentityType identityType){
        return BackofficeHelper.impersonateIdentity(identityId, identityType, secretKey);
    }

    protected static CreateManagedCardsProfileV2Model getCorporatePrepaidAuthForwardingCardProfileModel(final boolean authForwardingEnabled, final String defaultTimeoutDecision){
        return CreateManagedCardsProfileV2Model.builder()
                .createPrepaidProfileRequest(CreatePrepaidManagedCardsProfileModel
                        .DefaultCorporateCreatePrepaidManagedCardsProfileModel()
                        .createManagedCardsProfileRequest(AbstractCreateManagedCardsProfileModel
                                .DefaultCorporatePrepaidCreateManagedCardsProfileModel()
                                .authForwarding(new AuthForwardingModel(authForwardingEnabled, defaultTimeoutDecision))
                                .build())
                        .build())
                .cardFundingType("PREPAID")
                .build();
    }

    protected static CreateManagedCardsProfileV2Model getCorporateDebitAuthForwardingCardProfileModel(final boolean authForwardingEnabled, final String defaultTimeoutDecision){
        return CreateManagedCardsProfileV2Model.builder()
                .createDebitProfileRequest(CreateDebitManagedCardsProfileModel
                        .DefaultCorporateCreateDebitManagedCardsProfileModel()
                        .createManagedCardsProfileRequest(AbstractCreateManagedCardsProfileModel
                                .DefaultCorporateDebitCreateManagedCardsProfileModel()
                                .authForwarding(new AuthForwardingModel(authForwardingEnabled, defaultTimeoutDecision))
                                .build())
                        .build())
                .cardFundingType("DEBIT")
                .build();
    }

    protected static void authForwardingConfiguration(final boolean innovatorLevelEnabled,
                                                    final boolean programmeLevelEnabled) {
        AdminHelper.enableAuthForwarding(innovatorLevelEnabled, innovatorId, adminImpersonatedTenantToken);
        InnovatorHelper.enableAuthForwarding(UpdateProgrammeModel.AuthForwardingUrlSetup(programmeId, programmeLevelEnabled, "https://authforwardingurl.com"),
                programmeId, innovatorToken);
    }

    protected static Stream<Arguments> authForwardingLevels() {
        return Stream.of(
                // All levels disabled
                Arguments.of(false, false, false),
                // Innovator and Programme levels enabled, Card profile level disabled
                Arguments.of(true, true, false),
                // Innovator level disabled, Programme and Card profile levels enabled
                Arguments.of(false, true, true),
                // Innovator and Card profile levels disabled, Programme level enabled
                Arguments.of(false, true, false),
                // Innovator level enabled, Programme and Card profile levels disabled
                Arguments.of(true, false, false),
                // Innovator and Programme levels disabled, Card profile level disabled
                Arguments.of(false, false, true),
                // Innovator and Card profile levels enabled, Programme level disabled
                Arguments.of(true, false, true));
    }
}