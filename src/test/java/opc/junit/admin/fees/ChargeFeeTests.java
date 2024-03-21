package opc.junit.admin.fees;

import commons.enums.Currency;
import opc.enums.opc.*;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.admin.ChargeFeeModel;
import opc.models.admin.FeeSpecModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.testmodels.BalanceModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.admin.AdminFeesService;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChargeFeeTests extends BaseFeesSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;

    @BeforeAll
    public static void Setup() {
        corporateSetup();
        consumerSetup();
    }

    @ParameterizedTest
    @EnumSource(value = AdminFeeType.class, mode = EnumSource.Mode.EXCLUDE, names = {"UNKNOWN"})
    public void ChargeFee_CorporateManagedAccount_Success(final AdminFeeType adminFeeType){

        final long feeAmount = 100L;

        final String managedAccountId = createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken);
        final BalanceModel initialBalance = simulateManagedAccountDeposit(managedAccountId, corporateCurrency, 10000L, corporateAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(adminFeeType)
                        .setFeeSubType("PRINTED_STATEMENT")
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(corporateCurrency, feeAmount)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_OK);

        final BalanceModel managedAccountBalance = ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, corporateAuthenticationToken);

        assertEquals((int)(initialBalance.getAvailableBalance() - feeAmount), managedAccountBalance.getAvailableBalance());
        assertEquals((int)(initialBalance.getActualBalance() - feeAmount), managedAccountBalance.getActualBalance());
    }

    @Test
    public void ChargeFee_CorporatePrepaidManagedCard_Success(){

        final long initialBalance = 1000L;
        final long feeAmount = 100L;

        final ManagedCardDetails managedCard = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);
        transferFundsToCard(corporateAuthenticationToken, IdentityType.CORPORATE, managedCard.getManagedCardId(),
                corporateCurrency, initialBalance, 1);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedCard.getManagedCardId(), ManagedInstrumentType.MANAGED_CARDS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(corporateCurrency, feeAmount)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword))
                .then()
                .statusCode(SC_OK);

        final BalanceModel managedCardBalance = ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(), secretKey, corporateAuthenticationToken);

        assertEquals((int)(initialBalance - feeAmount), managedCardBalance.getAvailableBalance());
        assertEquals((int)(initialBalance - feeAmount), managedCardBalance.getActualBalance());
    }

    @Test
    public void ChargeFee_CorporateDebitManagedCard_Success(){

        final long feeAmount = 100L;

        final ManagedCardDetails managedCard = createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                corporateCurrency, corporateAuthenticationToken);
        final BalanceModel initialBalance = simulateManagedAccountDeposit(managedCard.getManagedCardModel().getParentManagedAccountId(), corporateCurrency,
                10000L, corporateAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedCard.getManagedCardId(), ManagedInstrumentType.MANAGED_CARDS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(corporateCurrency, feeAmount)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_OK);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(), secretKey, corporateAuthenticationToken);

        assertEquals((int)(initialBalance.getAvailableBalance() - feeAmount), managedAccountBalance.getAvailableBalance());
        assertEquals((int)(initialBalance.getActualBalance() - feeAmount), managedAccountBalance.getActualBalance());
    }

    @Test
    public void ChargeFee_ConsumerManagedAccount_Success(){

        final long feeAmount = 100L;

        final String managedAccountId = createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);
        final BalanceModel initialBalance = simulateManagedAccountDeposit(managedAccountId, consumerCurrency, 10000L, consumerAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(consumerCurrency, feeAmount)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_OK);

        final BalanceModel managedAccountBalance = ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, consumerAuthenticationToken);

        assertEquals((int)(initialBalance.getAvailableBalance() - feeAmount), managedAccountBalance.getAvailableBalance());
        assertEquals((int)(initialBalance.getActualBalance() - feeAmount), managedAccountBalance.getActualBalance());

    }

    @Test
    public void ChargeFee_ConsumerPrepaidManagedCard_Success(){

        final long initialBalance = 1000L;
        final long feeAmount = 100L;

        final ManagedCardDetails managedCard = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);
        transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                consumerCurrency, initialBalance, 1);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedCard.getManagedCardId(), ManagedInstrumentType.MANAGED_CARDS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(consumerCurrency, feeAmount)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_OK);

        final BalanceModel managedCardBalance = ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(), secretKey, consumerAuthenticationToken);

        assertEquals((int)(initialBalance - feeAmount), managedCardBalance.getAvailableBalance());
        assertEquals((int)(initialBalance - feeAmount), managedCardBalance.getActualBalance());
    }

    @Test
    public void ChargeFee_ConsumerDebitManagedCard_Success(){

        final long feeAmount = 100L;

        final ManagedCardDetails managedCard = createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                consumerCurrency, consumerAuthenticationToken);
        final BalanceModel initialBalance = simulateManagedAccountDeposit(managedCard.getManagedCardModel().getParentManagedAccountId(), consumerCurrency,
                10000L, consumerAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedCard.getManagedCardId(), ManagedInstrumentType.MANAGED_CARDS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(consumerCurrency, feeAmount)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_OK);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(), secretKey, consumerAuthenticationToken);

        assertEquals((int)(initialBalance.getAvailableBalance() - feeAmount), managedAccountBalance.getAvailableBalance());
        assertEquals((int)(initialBalance.getActualBalance() - feeAmount), managedAccountBalance.getActualBalance());
    }

    @Test
    public void ChargeFee_RequiredOnly_Success(){

        final long feeAmount = 100L;

        final String managedAccountId = createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken);
        final BalanceModel initialBalance = simulateManagedAccountDeposit(managedAccountId, corporateCurrency, 10000L, corporateAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.CHARGEBACK)
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(corporateCurrency, feeAmount)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_OK);

        final BalanceModel managedAccountBalance = ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, corporateAuthenticationToken);

        assertEquals((int)(initialBalance.getAvailableBalance() - feeAmount), managedAccountBalance.getAvailableBalance());
        assertEquals((int)(initialBalance.getActualBalance() - feeAmount), managedAccountBalance.getActualBalance());
    }

    @Test
    public void ChargeFee_ManagedCardStatement_Success(){

        final long initialBalance = 1000L;
        final long feeAmount = 100L;

        final ManagedCardDetails managedCard = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);
        transferFundsToCard(corporateAuthenticationToken, IdentityType.CORPORATE, managedCard.getManagedCardId(),
                corporateCurrency, initialBalance, 1);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedCard.getManagedCardId(), ManagedInstrumentType.MANAGED_CARDS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(corporateCurrency, feeAmount)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword))
                .then()
                .statusCode(SC_OK);

        final BalanceModel managedCardBalance = ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(), secretKey, corporateAuthenticationToken);

        assertEquals((int)(initialBalance - feeAmount), managedCardBalance.getAvailableBalance());
        assertEquals((int)(initialBalance - feeAmount), managedCardBalance.getActualBalance());

        ManagedCardsHelper.getManagedCardStatement(managedCard.getManagedCardId(), secretKey, corporateAuthenticationToken, 2)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("CHARGE_FEE"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact((int)feeAmount)))
                .body("entry[0].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].balanceAfter.amount", equalTo((int)(initialBalance - feeAmount)))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.chargeFeeType", equalTo(AdminFeeType.DEPOSIT.name()))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("TRANSFER"))
                .body("entry[1].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[1].transactionAmount.amount", equalTo((int)initialBalance))
                .body("entry[1].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].balanceAfter.amount", equalTo((int)initialBalance))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @Test
    public void ChargeFee_ManagedAccountStatement_Success(){

        final long feeAmount = 100L;

        final String managedAccountId = createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);
        final BalanceModel initialBalance = simulateManagedAccountDeposit(managedAccountId, consumerCurrency, 10000L, consumerAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(consumerCurrency, feeAmount)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_OK);

        final BalanceModel managedAccountBalance = ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, consumerAuthenticationToken);

        assertEquals((int)(initialBalance.getAvailableBalance() - feeAmount), managedAccountBalance.getAvailableBalance());
        assertEquals((int)(initialBalance.getActualBalance() - feeAmount), managedAccountBalance.getActualBalance());

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, consumerAuthenticationToken, 2)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("CHARGE_FEE"))
                .body("entry[0].transactionAmount.currency", equalTo(consumerCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact((int)feeAmount)))
                .body("entry[0].balanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[0].balanceAfter.amount", equalTo((int)(initialBalance.getAvailableBalance() - feeAmount)))
                .body("entry[0].cardholderFee.currency", equalTo(consumerCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.chargeFeeType", equalTo(AdminFeeType.DEPOSIT.name()))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[1].transactionAmount.currency", equalTo(consumerCurrency))
                .body("entry[1].transactionAmount.amount", equalTo(initialBalance.getAvailableBalance()))
                .body("entry[1].balanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[1].balanceAfter.amount", equalTo(initialBalance.getAvailableBalance()))
                .body("entry[1].cardholderFee.currency", equalTo(consumerCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(TestHelper.getFees(consumerCurrency).get(FeeType.DEPOSIT_FEE).getAmount().intValue()))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @Test
    public void ChargeFee_UnknownManagedAccountId_UnresolvedInstrument(){

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(corporateCurrency, 100L)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("UNRESOLVED_INSTRUMENT"));
    }

    @ParameterizedTest()
    @ValueSource(strings = {"", "abc"})
    public void ChargeFee_InvalidManagedAccountId_BadRequest(final String managedAccountId){

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(corporateCurrency, 100L)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"", "abc"})
    public void ChargeFee_InvalidManagedCardId_BadRequest(final String managedCardId){

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedCardId, ManagedInstrumentType.MANAGED_CARDS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(consumerCurrency, 100L)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ChargeFee_UnknownManagedCardId_UnresolvedInstrument(){

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), ManagedInstrumentType.MANAGED_CARDS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(consumerCurrency, 100L)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("UNRESOLVED_INSTRUMENT"));
    }

    @ParameterizedTest()
    @ValueSource(strings = {"", "ABCD"})
    public void ChargeFee_InvalidCurrency_BadRequest(final String currency){

        final String managedAccountId = createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(currency, 100L)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ChargeFee_UnknownCurrency_UnresolvedCurrency(){

        final String managedAccountId = createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount("AAA", 100L)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("UNRESOLVED_CURRENCY"));
    }

    @Test
    public void ChargeFee_DifferentManagedInstrumentCurrency_UnresolvedCurrency(){

        final String managedAccountId = createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel
                                .defaultFeeSpecModel(Collections
                                        .singletonList(new CurrencyAmount(Currency
                                                .getRandomWithExcludedCurrency(Currency.valueOf(consumerCurrency)).name(), 100L)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("UNRESOLVED_CURRENCY"));
    }

    @ParameterizedTest
    @ValueSource(longs = { 0, -100 })
    public void ChargeFee_InvalidAmount_BadRequest(final long amount){

        final String managedAccountId = createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(consumerCurrency, amount)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_BAD_REQUEST);

        final BalanceModel managedAccountBalance = ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, consumerAuthenticationToken);
        assertEquals(0, managedAccountBalance.getAvailableBalance());
        assertEquals(0, managedAccountBalance.getActualBalance());
    }

    @Test
    public void ChargeFee_InvalidSourceType_BadRequest(){

        final String managedAccountId = createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.UNKNOWN))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(consumerCurrency, 100L)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ChargeFee_CrossSourceType_UnresolvedInstrument(){

        final String managedAccountId = createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_CARDS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(consumerCurrency, 100L)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("UNRESOLVED_INSTRUMENT"));
    }

    @Test
    public void ChargeFee_InvalidFeeType_BadRequest(){

        final String managedAccountId = createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.UNKNOWN)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(consumerCurrency, 100L)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ChargeFee_NoFundsInManagedAccount_InsufficientFunds(){

        final String managedAccountId = createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(consumerCurrency, 100L)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSUFFICIENT_FUNDS"));
    }

    @Test
    public void ChargeFee_NoFundsInManagedCard_InsufficientFunds(){

        final ManagedCardDetails managedCard = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedCard.getManagedCardId(), ManagedInstrumentType.MANAGED_CARDS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(consumerCurrency, 100L)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSUFFICIENT_FUNDS"));
    }

    @Test
    public void ChargeFee_AdminTokenNoImpersonation_TokenNotImpersonated(){

        final ManagedCardDetails managedCard = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedCard.getManagedCardId(), ManagedInstrumentType.MANAGED_CARDS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(consumerCurrency, 100L)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, AdminService.loginAdmin())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TOKEN_NOT_IMPERSONATED"));
    }

    @Test
    public void ChargeFee_TenantTokenNoImpersonation_TokenNotImpersonated(){

        final ManagedCardDetails managedCard = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedCard.getManagedCardId(), ManagedInstrumentType.MANAGED_CARDS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(consumerCurrency, 100L)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword))
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TOKEN_NOT_IMPERSONATED"));
    }

    @Test
    public void ChargeFee_CrossTenantManagedCardFee_UnresolvedInstrument(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String impersonatedToken = AdminService.impersonateTenant(innovator.getLeft(), AdminService.loginAdmin());

        final ManagedCardDetails managedCard = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedCard.getManagedCardId(), ManagedInstrumentType.MANAGED_CARDS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(consumerCurrency, 100L)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, impersonatedToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("UNRESOLVED_INSTRUMENT"));
    }

    @Test
    public void ChargeFee_CrossTenantManagedAccountFee_UnresolvedInstrument(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String impersonatedToken = AdminService.impersonateTenant(innovator.getLeft(), AdminService.loginAdmin());

        final String managedAccountId = createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(corporateCurrency, 100L)))).build();

        AdminFeesService.chargeFee(chargeFeeModel, impersonatedToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("UNRESOLVED_INSTRUMENT"));
    }

    @Test
    public void ChargeFee_InvalidFeeSpecType_BadRequest(){

        final String managedAccountId = createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.builder()
                                .setType("INVALID")
                                .setFlatAmount(Collections.singletonList(new CurrencyAmount(consumerCurrency, 100L)))
                                .build())
                .build();

        AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    private static void consumerSetup() {
        final CreateConsumerModel consumerDetails =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(consumerDetails, secretKey);
        final String consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = consumerDetails.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);
    }

    private static void corporateSetup() {

        final CreateCorporateModel corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(corporateDetails, secretKey);
        final String corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = corporateDetails.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }
}
