package data;

import commons.enums.Currency;
import lombok.SneakyThrows;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.InnovatorSetup;
import opc.enums.opc.KycLevel;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.SendsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.simulator.SimulatorService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.concurrent.TimeUnit;

import static opc.enums.opc.InstrumentType.VIRTUAL;
import static opc.enums.opc.ManagedCardMode.PREPAID_MODE;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;

@Tag(MultiTags.DATA)
@Execution(ExecutionMode.CONCURRENT)
public class BaseTestSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel dataApplication;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String corporateManagedAccountsProfileId;
    protected static String consumerManagedCardsProfileId;
    protected static String sendsProfileId;
    protected static String owtProfileId;
    protected static String secretKey;
    protected static String sharedKey;
    protected static String innovatorId;
    protected static String innovatorEmail;
    protected static String innovatorPassword;
    protected static String programmeId;

    @BeforeAll
    public static void GlobalSetup() {

        dataApplication = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.DATA_APPLICATION);
        innovatorId = dataApplication.getInnovatorId();
        innovatorEmail = dataApplication.getInnovatorEmail();
        innovatorPassword = dataApplication.getInnovatorPassword();
        corporateProfileId = dataApplication.getCorporatesProfileId();
        consumerProfileId = dataApplication.getConsumersProfileId();
        corporateManagedAccountsProfileId = dataApplication.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedCardsProfileId = dataApplication.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        sendsProfileId = dataApplication.getSendProfileId();
        owtProfileId = dataApplication.getOwtProfileId();
        secretKey = dataApplication.getSecretKey();
        sharedKey = dataApplication.getSharedKey();
        programmeId = dataApplication.getProgrammeId();
    }

    protected static ManagedCardDetails createDataConsumerPrepaidCardWithFunds(final String managedAccountProfileId,
                                                                               final String managedCardProfileId,
                                                                               final String sendsProfileId,
                                                                               final String currency,
                                                                               final String corporateAuthenticationToken,
                                                                               final String consumerAuthenticationToken,
                                                                               final String nameOnCard){
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.dataCreateManagedAccountModel(managedAccountProfileId, Currency.valueOf(currency)).build();
        final String corporateManagedAccount =
                ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, corporateAuthenticationToken);

        TestHelper.simulateSuccessfulDeposit(corporateManagedAccount, new CurrencyAmount(currency, 10000L), secretKey, corporateAuthenticationToken);

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .dataCreatePrepaidManagedCardModel(managedCardProfileId, currency)
                        .setNameOnCard(nameOnCard)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken);

        final long cardAmount = 5000L;
        SendsHelper.sendFundsToCardSuccessfulOtpVerified(sendsProfileId, new CurrencyAmount(currency, cardAmount),
                corporateManagedAccount, managedCardId, secretKey, corporateAuthenticationToken);

        return ManagedCardDetails.builder()
                .setManagedCardId(managedCardId)
                .setManagedCardModel(createManagedCardModel)
                .setManagedCardMode(PREPAID_MODE)
                .setInstrumentType(VIRTUAL)
                .setInitialDepositAmount((int) cardAmount)
                .build();
    }

    @SneakyThrows
    protected static Pair<String, String> createSteppedUpCorporate(final CreateCorporateModel createCorporateModel, final String secretKey) {
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey, TestHelper.getDefaultPassword(secretKey));
        verifyKyb(corporate.getLeft(), secretKey);
        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, corporate.getRight());
        AuthenticationHelper.startAndVerifyStepup(TestHelper.VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, corporate.getRight());

        return corporate;
    }

    @SneakyThrows
    public static Pair<String, String> createSteppedUpConsumer(final CreateConsumerModel createConsumerModel,
                                                               final KycLevel kycLevel,
                                                               final String secretKey){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey, TestHelper.getDefaultPassword(secretKey));
        ConsumersHelper.startKyc(kycLevel, secretKey, consumer.getRight());
        verifyKyc(consumer.getLeft(), secretKey);
        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, consumer.getRight());
        AuthenticationHelper.startAndVerifyStepup(TestHelper.VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, consumer.getRight());

        return consumer;
    }

    @SneakyThrows
    public static Pair<String, String> createSteppedUpConsumer(final CreateConsumerModel createConsumerModel, final String secretKey){

        return createSteppedUpConsumer(createConsumerModel, KycLevel.KYC_LEVEL_2, secretKey);
    }

    @SneakyThrows
    public static void verifyKyc(final String consumerId, final String secretKey){

        TestHelper.ensureAsExpected(180,
                () -> SimulatorService.simulateKycApproval(secretKey, consumerId),
                SC_NO_CONTENT);

        TimeUnit.SECONDS.sleep(15);
    }

    @SneakyThrows
    public static void verifyKyb(final String corporateId, final String secretKey){

        TestHelper.ensureAsExpected(180,
                () -> SimulatorService.simulateKybApproval(secretKey, corporateId),
                SC_NO_CONTENT);

        TimeUnit.SECONDS.sleep(15);
    }
}
