package data;

import com.github.javafaker.Faker;
import commons.enums.Currency;
import opc.enums.opc.KycLevel;
import opc.enums.opc.OwtType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.junit.helpers.multi.SendsHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.SpendRulesModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.FeeSourceModel;
import opc.models.shared.FeesChargeModel;
import opc.models.simulator.SimulateCardPurchaseByIdModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.multi.ConsumersService;
import opc.services.multi.CorporatesService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.SC_OK;

public class TransactionsTests extends BaseTestSetup {

    private static final Currency DEFAULT_CURRENCY = Currency.EUR;
    private static String corporateAuthenticationToken;
    private static CreateConsumerModel createConsumerModel;
    private static String consumerAuthenticationToken;

    @BeforeAll
    public static void Setup(){
        corporateSetup();
        consumerSetup();
    }

    @Test
    public void CardPurchase_Prepaid_Success(){

        final long purchaseAmount = new RandomDataGenerator().nextLong(100, 300);

        final ManagedCardDetails managedCard =
                createDataConsumerPrepaidCardWithFunds(corporateManagedAccountsProfileId, consumerManagedCardsProfileId,
                        sendsProfileId, DEFAULT_CURRENCY.name(), corporateAuthenticationToken, consumerAuthenticationToken,
                        String.format("%s %s", createConsumerModel.getRootUser().getName(), createConsumerModel.getRootUser().getSurname()));

        setSpendLimit(managedCard.getManagedCardId(), consumerAuthenticationToken);

        IntStream.range(0, new RandomDataGenerator().nextInt(2, 10)).forEach(i -> {
            final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                    new CurrencyAmount(DEFAULT_CURRENCY, purchaseAmount), Optional.empty());

            Assertions.assertEquals("APPROVED", purchaseCode);
        });
    }

    @Test
    public void CardPurchase_KycLevel1MerchantCategoryBlocked_DeniedSpendControl(){

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.dataCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> consumer =
                createSteppedUpConsumer(createConsumerModel, KycLevel.KYC_LEVEL_1, secretKey);

       final Long purchaseAmount = new RandomDataGenerator().nextLong(100, 300);

        final ManagedCardDetails managedCard =
                createDataConsumerPrepaidCardWithFunds(corporateManagedAccountsProfileId, consumerManagedCardsProfileId,
                        sendsProfileId, DEFAULT_CURRENCY.name(), corporateAuthenticationToken, consumer.getRight(),
                        String.format("%s %s", createConsumerModel.getRootUser().getName(), createConsumerModel.getRootUser().getSurname()));

        final List<Integer> mccList = Arrays.asList(4829, 6012, 6051, 6538, 6540, 7995, 6011, 6010);

        mccList.forEach(mcc -> {
            final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                    new CurrencyAmount(DEFAULT_CURRENCY, purchaseAmount), Optional.of(String.valueOf(mcc)));

            Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        });
    }

    @Test
    public void Owt_Completed_Success(){

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.dataCreateManagedAccountModel(corporateManagedAccountsProfileId, DEFAULT_CURRENCY).build();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, corporateAuthenticationToken);

        TestHelper.simulateSuccessfulDeposit(managedAccountId, new CurrencyAmount(DEFAULT_CURRENCY.name(),
                10000L), secretKey, corporateAuthenticationToken);

        IntStream.range(0, new RandomDataGenerator().nextInt(1, 5)).forEach(i -> {

            final OutgoingWireTransfersModel outgoingWireTransfersModel =
                    OutgoingWireTransfersModel.dataOutgoingWireTransfersModel(owtProfileId, managedAccountId,
                            DEFAULT_CURRENCY.name(), new RandomDataGenerator().nextLong(500, 900), OwtType.SEPA).build();

            final String owtId =
                    OutgoingWireTransfersHelper.sendOwt(outgoingWireTransfersModel,
                            secretKey, corporateAuthenticationToken).getLeft();

            OutgoingWireTransfersHelper.verifyOwtOtp(owtId, secretKey, corporateAuthenticationToken);
        });
    }

    @Test
    public void Send_Completed_Success(){

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.dataCreateManagedAccountModel(corporateManagedAccountsProfileId, DEFAULT_CURRENCY).build();

        final String corporateManagedAccount =
                ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, corporateAuthenticationToken);

        TestHelper.simulateSuccessfulDeposit(corporateManagedAccount, new CurrencyAmount(DEFAULT_CURRENCY.name(), 10000L),
                secretKey, corporateAuthenticationToken);

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .dataCreatePrepaidManagedCardModel(consumerManagedCardsProfileId, DEFAULT_CURRENCY.name())
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken);

        IntStream.range(0, new RandomDataGenerator().nextInt(1, 5)).forEach(i ->
            SendsHelper.sendFundsToCardSuccessfulOtpVerified(sendsProfileId, new CurrencyAmount(DEFAULT_CURRENCY, new RandomDataGenerator().nextLong(500, 900)),
                    corporateManagedAccount, managedCardId, secretKey, corporateAuthenticationToken));
    }

    @Test
    public void ChargeConsumerFee_PrepaidCard_Success() {

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.dataCreateManagedAccountModel(corporateManagedAccountsProfileId, DEFAULT_CURRENCY).build();

        final String corporateManagedAccount =
                ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, corporateAuthenticationToken);

        TestHelper.simulateSuccessfulDeposit(corporateManagedAccount, new CurrencyAmount(DEFAULT_CURRENCY.name(), 10000L),
                secretKey, corporateAuthenticationToken);

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .dataCreatePrepaidManagedCardModel(consumerManagedCardsProfileId, DEFAULT_CURRENCY.name())
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken);

        SendsHelper.sendFundsToCardSuccessfulOtpVerified(sendsProfileId, new CurrencyAmount(DEFAULT_CURRENCY, 500L),
                corporateManagedAccount, managedCardId, secretKey, corporateAuthenticationToken);

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_cards", managedCardId));

        ConsumersService.chargeConsumerFee(feesChargeModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void ChargeCorporateFee_ManagedAccount_Success() {

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.dataCreateManagedAccountModel(corporateManagedAccountsProfileId, DEFAULT_CURRENCY).build();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, corporateAuthenticationToken);

        TestHelper.simulateSuccessfulDeposit(managedAccountId, new CurrencyAmount(DEFAULT_CURRENCY.name(), 10000L),
                secretKey, corporateAuthenticationToken);

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void PendingDeposit_Approved_Success(final Currency currency)  {

        final long depositAmount = new RandomDataGenerator().nextLong(4000, 6000);

        IntStream.range(0, new RandomDataGenerator().nextInt(2, 10)).forEach(i -> {

            final Pair<String, String> managedAccount = getManagedAccount(currency);

            TestHelper.simulateSuccessfulPendingDeposit(managedAccount.getLeft(), currency.name(), depositAmount,true, false, innovatorId,  secretKey, managedAccount.getRight());
        });
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void PendingDeposit_SepaInstantApproved_Success(final Currency currency)  {

        final long depositAmount = new RandomDataGenerator().nextLong(4000, 6000);

        IntStream.range(0, new RandomDataGenerator().nextInt(2, 10)).forEach(i -> {

            final Pair<String, String> managedAccount = getManagedAccount(currency);

            TestHelper.simulateSuccessfulPendingDeposit(managedAccount.getLeft(), currency.name(), depositAmount,true, true, innovatorId,  secretKey, managedAccount.getRight());
        });
    }

    private static void consumerSetup() {
        createConsumerModel =
                CreateConsumerModel.dataCreateConsumerModel(consumerProfileId)
                        .setBaseCurrency(DEFAULT_CURRENCY.name())
                        .build();

        final Pair<String, String> authenticatedConsumer =
                createSteppedUpConsumer(createConsumerModel, secretKey);
        consumerAuthenticationToken = authenticatedConsumer.getRight();
    }

    private static void corporateSetup() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.dataCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(DEFAULT_CURRENCY.name())
                        .build();

        final Pair<String, String> authenticatedCorporate = createSteppedUpCorporate(createCorporateModel, secretKey);
        corporateAuthenticationToken = authenticatedCorporate.getRight();
    }

    private String simulatePurchase(final String managedCardId,
                                    final CurrencyAmount purchaseAmount,
                                    final Optional<String> mcc){

        final List<Integer> mccList = Arrays.asList(4829, 6012, 6051, 6538, 6540, 7995, 6011, 6010);

        final Faker faker = new Faker();

        final SimulateCardPurchaseByIdModel simulateCardPurchaseModel =
                SimulateCardPurchaseByIdModel.builder()
                        .setMerchantName(faker.company().name())
                        .setMerchantCategoryCode(mcc.orElse(String.valueOf(mccList.get(new Random().nextInt(mccList.size())))))
                        .setTransactionAmount(purchaseAmount)
                        .build();

        return SimulatorHelper.simulateCardPurchaseById(secretKey,
                managedCardId,
                simulateCardPurchaseModel);
    }

    private void setSpendLimit(final String managedCardId,
                               final String authenticationToken){
        final SpendRulesModel spendRulesModel = getDefaultSpendRulesModel()
                .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCardId, authenticationToken);
    }

    private SpendRulesModel.Builder getDefaultSpendRulesModel(){
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
                .setAllowCreditAuthorisations(true);
    }

    private Pair<String, String> getManagedAccount(final Currency currency) {
        return innovatorId.equals("1991") ?
                getDataManagedAccount(currency) :
                getProductManagedAccount(currency);
    }

    private Pair<String, String> getDataManagedAccount(final Currency currency) {
        final Map<String, Currency> accounts = new HashMap<>();
        accounts.put("112101953372422193", Currency.EUR);
        accounts.put("112101954549579824", Currency.EUR);
        accounts.put("112101955261497394", Currency.EUR);
        accounts.put("112101952619806769", Currency.EUR);
        accounts.put("112101954378858545", Currency.EUR);
        accounts.put("112101955083960369", Currency.EUR);
        accounts.put("112101954914418738", Currency.EUR);
        accounts.put("112101953192722482", Currency.EUR);
        accounts.put("112101953564049458", Currency.EUR);
        accounts.put("112101954195095601", Currency.EUR);
        accounts.put("112101952432570417", Currency.EUR);
        accounts.put("112101954723643440", Currency.EUR);
        accounts.put("112101952995065905", Currency.EUR);
        accounts.put("112101953805942834", Currency.EUR);
        accounts.put("112101952811302960", Currency.EUR);

        accounts.put("112102035125895217", Currency.GBP);
        accounts.put("112102031155658802", Currency.GBP);
        accounts.put("112102027235098672", Currency.GBP);
        accounts.put("112102023289700402", Currency.GBP);
        accounts.put("112102019415801905", Currency.GBP);
        accounts.put("112102015441961009", Currency.GBP);
        accounts.put("112102013567565872", Currency.GBP);
        accounts.put("112101999639396401", Currency.GBP);
        accounts.put("112101995699503153", Currency.GBP);
        accounts.put("112101991782416432", Currency.GBP);
        accounts.put("112101987859759154", Currency.GBP);
        accounts.put("112101983920848946", Currency.GBP);
        accounts.put("112101979963064369", Currency.GBP);
        accounts.put("112101976038899761", Currency.GBP);
        accounts.put("112101972917747762", Currency.GBP);

        accounts.put("112102056879652912", Currency.USD);
        accounts.put("112102056598372400", Currency.USD);
        accounts.put("112102056330395696", Currency.USD);
        accounts.put("112102056058224689", Currency.USD);
        accounts.put("112102055793786928", Currency.USD);
        accounts.put("112102055534460978", Currency.USD);
        accounts.put("112102055269302321", Currency.USD);
        accounts.put("112102055006634033", Currency.USD);
        accounts.put("112102054746718258", Currency.USD);
        accounts.put("112102054464061490", Currency.USD);
        accounts.put("112102054183501874", Currency.USD);
        accounts.put("112102053921030193", Currency.USD);
        accounts.put("112102053662687280", Currency.USD);
        accounts.put("112102053403754546", Currency.USD);
        accounts.put("112102053144428592", Currency.USD);


        final List<String> filteredAccounts =
                accounts.entrySet().stream().filter(x -> x.getValue().equals(currency))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());

        return Pair.of(filteredAccounts.get(new Random().nextInt(filteredAccounts.size())), AuthenticationHelper.login("MitchelRolfson38820@weavrtesting.io", secretKey));
    }

    private Pair<String, String> getProductManagedAccount(final Currency currency) {
        final Map<String, Currency> accounts = new HashMap<>();
        accounts.put("112111452860121136", Currency.EUR);
        accounts.put("112111452658663473", Currency.EUR);
        accounts.put("112111452464218161", Currency.EUR);
        accounts.put("112111452264136753", Currency.EUR);
        accounts.put("112111452050817073", Currency.EUR);
        accounts.put("112111451834744882", Currency.EUR);
        accounts.put("112111451634270257", Currency.EUR);
        accounts.put("112111451428290608", Currency.EUR);
        accounts.put("112111451228471345", Currency.EUR);
        accounts.put("112111451029635120", Currency.EUR);
        accounts.put("112111450820444210", Currency.EUR);
        accounts.put("112111450625474610", Currency.EUR);
        accounts.put("112111450408419377", Currency.EUR);
        accounts.put("112111450205847601", Currency.EUR);
        accounts.put("112111450010157105", Currency.EUR);

        accounts.put("112111442089082930", Currency.GBP);
        accounts.put("112111441890312242", Currency.GBP);
        accounts.put("112111441697767472", Currency.GBP);
        accounts.put("112111441508237362", Currency.GBP);
        accounts.put("112111441320083504", Currency.GBP);
        accounts.put("112111441123344434", Currency.GBP);
        accounts.put("112111440910549040", Currency.GBP);
        accounts.put("112111440709025841", Currency.GBP);
        accounts.put("112111440510320690", Currency.GBP);
        accounts.put("112111440317906994", Currency.GBP);
        accounts.put("112111440131522610", Currency.GBP);
        accounts.put("112111439929278514", Currency.GBP);
        accounts.put("112111439731228722", Currency.GBP);
        accounts.put("112111439540191280", Currency.GBP);
        accounts.put("112111439347318833", Currency.GBP);

        accounts.put("112111413922758705", Currency.USD);
        accounts.put("112111413751709745", Currency.USD);
        accounts.put("112111413573713968", Currency.USD);
        accounts.put("112111413389951024", Currency.USD);
        accounts.put("112111413214838833", Currency.USD);
        accounts.put("112111413033435185", Currency.USD);
        accounts.put("112111412846133296", Currency.USD);
        accounts.put("112111412665974833", Currency.USD);
        accounts.put("112111412485095473", Currency.USD);
        accounts.put("112111412300808240", Currency.USD);
        accounts.put("112111412109180976", Currency.USD);
        accounts.put("112111411929415730", Currency.USD);
        accounts.put("112111411751813170", Currency.USD);
        accounts.put("112111411567460400", Currency.USD);
        accounts.put("112111411378913328", Currency.USD);


        final List<String> filteredAccounts =
                accounts.entrySet().stream().filter(x -> x.getValue().equals(currency))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());

        return Pair.of(filteredAccounts.get(new Random().nextInt(filteredAccounts.size())), AuthenticationHelper.login("BlossomRoberts46556@weavrtesting.io", secretKey));
    }
}
