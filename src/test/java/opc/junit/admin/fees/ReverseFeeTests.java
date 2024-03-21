package opc.junit.admin.fees;

import opc.enums.opc.AdminFeeType;
import opc.enums.opc.FeeType;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedInstrumentType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.admin.ChargeFeeModel;
import opc.models.admin.FeeSpecModel;
import opc.models.admin.ReverseFeeModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.shared.TxTypeIdModel;
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

public class ReverseFeeTests extends BaseFeesSetup {

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
    public void ReverseFee_CorporateManagedAccount_Success(final AdminFeeType adminFeeType){

        final long feeAmount = 100L;

        final String managedAccountId = createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken);
        final BalanceModel initialBalance = simulateManagedAccountDeposit(managedAccountId, corporateCurrency, 10000L, corporateAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(adminFeeType)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(corporateCurrency, feeAmount)))).build();

        final String transactionId =
                AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(adminFeeType.name(), transactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        AdminFeesService.reverseFee(reverseFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_OK);

        final BalanceModel managedAccountBalance = ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, corporateAuthenticationToken);

        assertEquals(initialBalance.getAvailableBalance(), managedAccountBalance.getAvailableBalance());
        assertEquals(initialBalance.getActualBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void ReverseFee_CorporatePrepaidManagedCard_Success(){

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

        final String transactionId =
                AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.DEPOSIT.name(), transactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(managedCard.getManagedCardId(), ManagedInstrumentType.MANAGED_CARDS))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        AdminFeesService.reverseFee(reverseFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_OK);

        final BalanceModel managedCardBalance = ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(), secretKey, corporateAuthenticationToken);

        assertEquals((int)(initialBalance), managedCardBalance.getAvailableBalance());
        assertEquals((int)(initialBalance), managedCardBalance.getActualBalance());
    }

    @Test
    public void ReverseFee_CorporateDebitManagedCard_Success(){

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

        final String transactionId =
                AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.DEPOSIT.name(), transactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(managedCard.getManagedCardId(), ManagedInstrumentType.MANAGED_CARDS))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        AdminFeesService.reverseFee(reverseFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_OK);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(), secretKey, corporateAuthenticationToken);

        assertEquals(initialBalance.getAvailableBalance(), managedAccountBalance.getAvailableBalance());
        assertEquals(initialBalance.getActualBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void ReverseFee_ConsumerManagedAccount_Success(){

        final long feeAmount = 100L;

        final String managedAccountId = createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);
        final BalanceModel initialBalance = simulateManagedAccountDeposit(managedAccountId, consumerCurrency, 10000L, consumerAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(consumerCurrency, feeAmount)))).build();

        final String transactionId =
                AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.DEPOSIT.name(), transactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        AdminFeesService.reverseFee(reverseFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_OK);

        final BalanceModel managedAccountBalance = ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, consumerAuthenticationToken);

        assertEquals(initialBalance.getAvailableBalance(), managedAccountBalance.getAvailableBalance());
        assertEquals(initialBalance.getActualBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void ReverseFee_ConsumerPrepaidManagedCard_Success(){

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

        final String transactionId =
                AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.DEPOSIT.name(), transactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(managedCard.getManagedCardId(), ManagedInstrumentType.MANAGED_CARDS))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        AdminFeesService.reverseFee(reverseFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_OK);

        final BalanceModel managedCardBalance = ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(), secretKey, consumerAuthenticationToken);

        assertEquals((int)(initialBalance), managedCardBalance.getAvailableBalance());
        assertEquals((int)(initialBalance), managedCardBalance.getActualBalance());
    }

    @Test
    public void ReverseFee_ConsumerDebitManagedCard_Success(){

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

        final String transactionId =
                AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.DEPOSIT.name(), transactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(managedCard.getManagedCardId(), ManagedInstrumentType.MANAGED_CARDS))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        AdminFeesService.reverseFee(reverseFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_OK);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(), secretKey, consumerAuthenticationToken);

        assertEquals(initialBalance.getAvailableBalance(), managedAccountBalance.getAvailableBalance());
        assertEquals(initialBalance.getActualBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void ReverseFee_ManagedCardStatement_Success(){

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

        final String chargeFeeTransactionId =
                AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.DEPOSIT.name(), chargeFeeTransactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(managedCard.getManagedCardId(), ManagedInstrumentType.MANAGED_CARDS))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        final String reverseFeeTransactionId =
                AdminFeesService.reverseFee(reverseFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("txId.id");

        final BalanceModel managedCardBalance = ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(), secretKey, corporateAuthenticationToken);

        assertEquals((int)(initialBalance), managedCardBalance.getAvailableBalance());
        assertEquals((int)(initialBalance), managedCardBalance.getActualBalance());

        ManagedCardsHelper.getManagedCardStatement(managedCard.getManagedCardId(), secretKey, corporateAuthenticationToken, 3)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("FEE_REVERSAL"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo((int)feeAmount))
                .body("entry[0].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].balanceAfter.amount", equalTo((int)(initialBalance)))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.relatedTransactionId", equalTo(chargeFeeTransactionId))
                .body("entry[0].additionalFields.relatedTransactionIdType", equalTo("CHARGE_FEE"))
                .body("entry[0].additionalFields.note", equalTo(reverseFeeModel.getNote()))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("CHARGE_FEE"))
                .body("entry[1].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact((int)feeAmount)))
                .body("entry[1].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].balanceAfter.amount", equalTo((int)(initialBalance - feeAmount)))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].additionalFields.relatedTransactionId", equalTo(reverseFeeTransactionId))
                .body("entry[1].additionalFields.relatedTransactionIdType", equalTo("FEE_REVERSAL"))
                .body("entry[1].additionalFields.note", equalTo(chargeFeeModel.getNote()))
                .body("entry[2].transactionId.id", notNullValue())
                .body("entry[2].transactionId.type", equalTo("TRANSFER"))
                .body("entry[2].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[2].transactionAmount.amount", equalTo((int)initialBalance))
                .body("entry[2].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[2].balanceAfter.amount", equalTo((int)initialBalance))
                .body("entry[2].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[2].cardholderFee.amount", equalTo(0))
                .body("entry[2].processedTimestamp", notNullValue())
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    @Test
    public void ReverseFee_ManagedAccountStatement_Success(){

        final long feeAmount = 100L;

        final String managedAccountId = createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);
        final BalanceModel initialBalance = simulateManagedAccountDeposit(managedAccountId, consumerCurrency, 10000L, consumerAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(consumerCurrency, feeAmount)))).build();

        final String chargeFeeTransactionId =
                AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.DEPOSIT.name(), chargeFeeTransactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        final String reverseFeeTransactionId =
            AdminFeesService.reverseFee(reverseFeeModel, adminImpersonatedTenantToken)
                    .then()
                    .statusCode(SC_OK)
                    .extract()
                    .jsonPath()
                    .get("txId.id");

        final BalanceModel managedAccountBalance = ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, consumerAuthenticationToken);

        assertEquals(initialBalance.getAvailableBalance(), managedAccountBalance.getAvailableBalance());
        assertEquals(initialBalance.getActualBalance(), managedAccountBalance.getActualBalance());

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, consumerAuthenticationToken, 3)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("FEE_REVERSAL"))
                .body("entry[0].transactionAmount.currency", equalTo(consumerCurrency))
                .body("entry[0].transactionAmount.amount", equalTo((int)feeAmount))
                .body("entry[0].balanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(initialBalance.getAvailableBalance()))
                .body("entry[0].cardholderFee.currency", equalTo(consumerCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.relatedTransactionId", equalTo(chargeFeeTransactionId))
                .body("entry[0].additionalFields.relatedTransactionIdType", equalTo("CHARGE_FEE"))
                .body("entry[0].additionalFields.note", equalTo(reverseFeeModel.getNote()))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("CHARGE_FEE"))
                .body("entry[1].transactionAmount.currency", equalTo(consumerCurrency))
                .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact((int)feeAmount)))
                .body("entry[1].balanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[1].balanceAfter.amount", equalTo((int)(initialBalance.getAvailableBalance() - feeAmount)))
                .body("entry[1].cardholderFee.currency", equalTo(consumerCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].additionalFields.relatedTransactionId", equalTo(reverseFeeTransactionId))
                .body("entry[1].additionalFields.relatedTransactionIdType", equalTo("FEE_REVERSAL"))
                .body("entry[1].additionalFields.note", equalTo(chargeFeeModel.getNote()))
                .body("entry[2].transactionId.id", notNullValue())
                .body("entry[2].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[2].transactionAmount.currency", equalTo(consumerCurrency))
                .body("entry[2].transactionAmount.amount", equalTo(initialBalance.getAvailableBalance()))
                .body("entry[2].balanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[2].balanceAfter.amount", equalTo(initialBalance.getAvailableBalance()))
                .body("entry[2].cardholderFee.currency", equalTo(consumerCurrency))
                .body("entry[2].cardholderFee.amount", equalTo(TestHelper.getFees(consumerCurrency).get(FeeType.DEPOSIT_FEE).getAmount().intValue()))
                .body("entry[2].processedTimestamp", notNullValue())
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    @Test
    public void ReverseFee_RequiredOnly_Success(){

        final long feeAmount = 100L;

        final String managedAccountId = createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken);
        final BalanceModel initialBalance = simulateManagedAccountDeposit(managedAccountId, corporateCurrency, 10000L, corporateAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.CHARGEBACK)
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(corporateCurrency, feeAmount)))).build();

        final String transactionId =
                AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.DEPOSIT.name(), transactionId))
                        .build();

        AdminFeesService.reverseFee(reverseFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_OK);

        final BalanceModel managedAccountBalance = ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, corporateAuthenticationToken);

        assertEquals(initialBalance.getAvailableBalance(), managedAccountBalance.getAvailableBalance());
        assertEquals(initialBalance.getActualBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void ReverseFee_UnknownTxId_NotFound(){

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

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.DEPOSIT.name(), RandomStringUtils.randomNumeric(18)))
                        .setInstrumentId(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        AdminFeesService.reverseFee(reverseFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_NOT_FOUND);

        final BalanceModel managedAccountBalance = ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, consumerAuthenticationToken);

        assertEquals(initialBalance.getAvailableBalance() - feeAmount, managedAccountBalance.getAvailableBalance());
        assertEquals(initialBalance.getActualBalance() - feeAmount, managedAccountBalance.getActualBalance());
    }

    @ParameterizedTest()
    @ValueSource(strings = {"", "abc"})
    public void ReverseFee_InvalidTxId_BadRequest(final String transactionId){

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

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.DEPOSIT.name(), transactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        AdminFeesService.reverseFee(reverseFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_BAD_REQUEST);

        final BalanceModel managedAccountBalance = ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, consumerAuthenticationToken);

        assertEquals(initialBalance.getAvailableBalance() - feeAmount, managedAccountBalance.getAvailableBalance());
        assertEquals(initialBalance.getActualBalance() - feeAmount, managedAccountBalance.getActualBalance());
    }

    @Test
    public void ReverseFee_CrossIdentityInstrument_InstrumentNotFound(){

        final long feeAmount = 100L;

        final String managedAccountId = createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);
        final String crossIdentityManagedAccountId = createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken);
        final BalanceModel initialBalance = simulateManagedAccountDeposit(managedAccountId, consumerCurrency, 10000L, consumerAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(consumerCurrency, feeAmount)))).build();

        final String transactionId =
                AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.DEPOSIT.name(), transactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(crossIdentityManagedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        AdminFeesService.reverseFee(reverseFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_FOUND"));

        final BalanceModel managedAccountBalance = ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, consumerAuthenticationToken);

        assertEquals(initialBalance.getAvailableBalance() - feeAmount, managedAccountBalance.getAvailableBalance());
        assertEquals(initialBalance.getActualBalance() - feeAmount, managedAccountBalance.getActualBalance());
    }

    @Test
    public void ReverseFee_UnknownManagedAccountId_InstrumentNotFound(){

        final long feeAmount = 100L;

        final String managedAccountId = createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken);
        final BalanceModel initialBalance = simulateManagedAccountDeposit(managedAccountId, corporateCurrency, 10000L, corporateAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(corporateCurrency, feeAmount)))).build();

        final String transactionId =
                AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.DEPOSIT.name(), transactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        AdminFeesService.reverseFee(reverseFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_FOUND"));

        final BalanceModel managedAccountBalance = ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, corporateAuthenticationToken);

        assertEquals(initialBalance.getAvailableBalance() - feeAmount, managedAccountBalance.getAvailableBalance());
        assertEquals(initialBalance.getActualBalance() - feeAmount, managedAccountBalance.getActualBalance());
    }

    @ParameterizedTest()
    @ValueSource(strings = {"", "abc"})
    public void ReverseFee_InvalidManagedAccountId_BadRequest(final String invalidManagedAccountId){

        final long feeAmount = 100L;

        final String managedAccountId = createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken);
        final BalanceModel initialBalance = simulateManagedAccountDeposit(managedAccountId, corporateCurrency, 10000L, corporateAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(corporateCurrency, feeAmount)))).build();

        final String transactionId =
                AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.DEPOSIT.name(), transactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(invalidManagedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        AdminFeesService.reverseFee(reverseFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_BAD_REQUEST);

        final BalanceModel managedAccountBalance = ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, corporateAuthenticationToken);

        assertEquals(initialBalance.getAvailableBalance() - feeAmount, managedAccountBalance.getAvailableBalance());
        assertEquals(initialBalance.getActualBalance() - feeAmount, managedAccountBalance.getActualBalance());
    }

    @ParameterizedTest()
    @ValueSource(strings = {"", "abc"})
    public void ReverseFee_InvalidManagedCardId_BadRequest(final String managedCardId){

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

        final String transactionId =
                AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.DEPOSIT.name(), transactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(managedCardId, ManagedInstrumentType.MANAGED_CARDS))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        AdminFeesService.reverseFee(reverseFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_BAD_REQUEST);

        final BalanceModel managedCardBalance = ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(), secretKey, consumerAuthenticationToken);

        assertEquals((int)(initialBalance - feeAmount), managedCardBalance.getAvailableBalance());
        assertEquals((int)(initialBalance - feeAmount), managedCardBalance.getActualBalance());
    }

    @Test
    public void ReverseFee_UnknownManagedCardId_InstrumentNotFound(){

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

        final String transactionId =
                AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.DEPOSIT.name(), transactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), ManagedInstrumentType.MANAGED_CARDS))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        AdminFeesService.reverseFee(reverseFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_FOUND"));

        final BalanceModel managedCardBalance = ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(), secretKey, consumerAuthenticationToken);

        assertEquals((int)(initialBalance - feeAmount), managedCardBalance.getAvailableBalance());
        assertEquals((int)(initialBalance - feeAmount), managedCardBalance.getActualBalance());
    }

    @Test
    public void ReverseFee_InvalidSourceType_BadRequest(){

        final long feeAmount = 100L;

        final String managedAccountId = createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken);
        final BalanceModel initialBalance = simulateManagedAccountDeposit(managedAccountId, corporateCurrency, 10000L, corporateAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(corporateCurrency, feeAmount)))).build();

        final String transactionId =
                AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.DEPOSIT.name(), transactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.UNKNOWN))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        AdminFeesService.reverseFee(reverseFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_BAD_REQUEST);

        final BalanceModel managedAccountBalance = ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, corporateAuthenticationToken);

        assertEquals(initialBalance.getAvailableBalance() - feeAmount, managedAccountBalance.getAvailableBalance());
        assertEquals(initialBalance.getActualBalance() - feeAmount, managedAccountBalance.getActualBalance());
    }

    @Test
    public void ReverseFee_CrossSourceType_InstrumentNotFound(){

        final long feeAmount = 100L;

        final String managedAccountId = createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken);
        final BalanceModel initialBalance = simulateManagedAccountDeposit(managedAccountId, corporateCurrency, 10000L, corporateAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(corporateCurrency, feeAmount)))).build();

        final String transactionId =
                AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.DEPOSIT.name(), transactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_CARDS))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        AdminFeesService.reverseFee(reverseFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_FOUND"));

        final BalanceModel managedAccountBalance = ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, corporateAuthenticationToken);

        assertEquals(initialBalance.getAvailableBalance() - feeAmount, managedAccountBalance.getAvailableBalance());
        assertEquals(initialBalance.getActualBalance() - feeAmount, managedAccountBalance.getActualBalance());
    }

    @Test
    public void ReverseFee_InvalidFeeType_BadRequest(){

        final long feeAmount = 100L;

        final String managedAccountId = createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken);
        final BalanceModel initialBalance = simulateManagedAccountDeposit(managedAccountId, corporateCurrency, 10000L, corporateAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(corporateCurrency, feeAmount)))).build();

        final String transactionId =
                AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.UNKNOWN.name(), transactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        AdminFeesService.reverseFee(reverseFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_BAD_REQUEST);

        final BalanceModel managedAccountBalance = ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, corporateAuthenticationToken);

        assertEquals(initialBalance.getAvailableBalance() - feeAmount, managedAccountBalance.getAvailableBalance());
        assertEquals(initialBalance.getActualBalance() - feeAmount, managedAccountBalance.getActualBalance());
    }

    @Test
    public void ReverseFee_AdminTokenNoImpersonation_TokenNotImpersonated(){

        final long feeAmount = 100L;

        final String managedAccountId = createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken);
        final BalanceModel initialBalance = simulateManagedAccountDeposit(managedAccountId, corporateCurrency, 10000L, corporateAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(corporateCurrency, feeAmount)))).build();

        final String transactionId =
                AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.DEPOSIT.name(), transactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        AdminFeesService.reverseFee(reverseFeeModel, AdminService.loginAdmin())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TOKEN_NOT_IMPERSONATED"));

        final BalanceModel managedAccountBalance = ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, corporateAuthenticationToken);

        assertEquals(initialBalance.getAvailableBalance() - feeAmount, managedAccountBalance.getAvailableBalance());
        assertEquals(initialBalance.getActualBalance() - feeAmount, managedAccountBalance.getActualBalance());
    }

    @Test
    public void ReverseFee_TenantTokenNoImpersonation_TokenNotImpersonated(){

        final long feeAmount = 100L;

        final String managedAccountId = createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken);
        final BalanceModel initialBalance = simulateManagedAccountDeposit(managedAccountId, corporateCurrency, 10000L, corporateAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(corporateCurrency, feeAmount)))).build();

        final String transactionId =
                AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.DEPOSIT.name(), transactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        AdminFeesService.reverseFee(reverseFeeModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword))
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TOKEN_NOT_IMPERSONATED"));

        final BalanceModel managedAccountBalance = ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, corporateAuthenticationToken);

        assertEquals(initialBalance.getAvailableBalance() - feeAmount, managedAccountBalance.getAvailableBalance());
        assertEquals(initialBalance.getActualBalance() - feeAmount, managedAccountBalance.getActualBalance());
    }

    @Test
    public void ReverseFee_CrossTenantManagedCardFee_InstrumentNotFound(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String impersonatedToken = AdminService.impersonateTenant(innovator.getLeft(), AdminService.loginAdmin());

        final long initialBalance = 1000L;
        final long feeAmount = 100L;

        final ManagedCardDetails managedCard = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);
        transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                consumerCurrency, initialBalance, 1);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedCard.getManagedCardId(), ManagedInstrumentType.MANAGED_CARDS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(corporateCurrency, feeAmount)))).build();

        final String transactionId =
                AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.DEPOSIT.name(), transactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(managedCard.getManagedCardId(), ManagedInstrumentType.MANAGED_CARDS))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        AdminFeesService.reverseFee(reverseFeeModel, impersonatedToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_FOUND"));

        final BalanceModel managedCardBalance = ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(), secretKey, corporateAuthenticationToken);

        assertEquals((int)(initialBalance - feeAmount), managedCardBalance.getAvailableBalance());
        assertEquals((int)(initialBalance - feeAmount), managedCardBalance.getActualBalance());
    }

    @Test
    public void ReverseFee_CrossTenantManagedAccountFee_InstrumentNotFound(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String impersonatedToken = AdminService.impersonateTenant(innovator.getLeft(), AdminService.loginAdmin());

        final long feeAmount = 100L;

        final String managedAccountId = createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken);
        final BalanceModel initialBalance = simulateManagedAccountDeposit(managedAccountId, corporateCurrency, 10000L, corporateAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(corporateCurrency, feeAmount)))).build();

        final String transactionId =
                AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.DEPOSIT.name(), transactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        AdminFeesService.reverseFee(reverseFeeModel, impersonatedToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_FOUND"));

        final BalanceModel managedAccountBalance = ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, corporateAuthenticationToken);

        assertEquals(initialBalance.getAvailableBalance() - feeAmount, managedAccountBalance.getAvailableBalance());
        assertEquals(initialBalance.getActualBalance() - feeAmount, managedAccountBalance.getActualBalance());
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
