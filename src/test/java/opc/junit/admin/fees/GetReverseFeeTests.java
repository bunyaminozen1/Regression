package opc.junit.admin.fees;

import opc.enums.opc.AdminFeeType;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedInstrumentType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.admin.ChargeFeeModel;
import opc.models.admin.FeeSpecModel;
import opc.models.admin.GetFeesResponseModel;
import opc.models.admin.ReverseFeeModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.shared.TxTypeIdModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.admin.AdminFeesService;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GetReverseFeeTests extends BaseFeesSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static final Map<String, ReverseFeeModel> corporateFees = new HashMap<>();
    private static final Map<String, ReverseFeeModel> consumerFees = new HashMap<>();

    @BeforeAll
    public static void Setup() {
        corporateSetup();
        consumerSetup();

        final String corporateManagedAccountId = createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken);
        simulateManagedAccountDeposit(corporateManagedAccountId, corporateCurrency, 1000L, corporateAuthenticationToken);

        final String consumerManagedAccountId = createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);
        simulateManagedAccountDeposit(consumerManagedAccountId, consumerCurrency, 1000L, consumerAuthenticationToken);

        final ManagedCardDetails corporateDebitManagedCard = createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                corporateCurrency, corporateAuthenticationToken);
        simulateManagedAccountDeposit(corporateDebitManagedCard.getManagedCardModel().getParentManagedAccountId(),
                corporateCurrency, 1000L, corporateAuthenticationToken);

        final ManagedCardDetails consumerPrepaidManagedCard = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);
        transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, consumerPrepaidManagedCard.getManagedCardId(),
                consumerCurrency, 1000L, 1);

        Arrays.stream(AdminFeeType.values()).filter(x -> !x.equals(AdminFeeType.UNKNOWN)).limit(3).forEach(fee -> {
            reverseFee(fee, corporateManagedAccountId, MANAGED_ACCOUNTS, corporateCurrency, IdentityType.CORPORATE);
            reverseFee(fee, corporateDebitManagedCard.getManagedCardId(), MANAGED_CARDS, corporateCurrency, IdentityType.CORPORATE);

            reverseFee(fee, consumerManagedAccountId, MANAGED_ACCOUNTS, consumerCurrency, IdentityType.CONSUMER);
            reverseFee(fee, consumerPrepaidManagedCard.getManagedCardId(), MANAGED_CARDS, consumerCurrency, IdentityType.CONSUMER);
        });
    }

    @Test
    public void GetFee_CorporateManagedAccountFee_Success(){

        final Map.Entry<String, ReverseFeeModel>
                corporateFee = corporateFees.entrySet().stream().filter(x -> x.getValue().getInstrumentId().getType().equals(MANAGED_ACCOUNTS.getValue())).findFirst().orElseThrow();

        final GetFeesResponseModel retrievedFee =
                AdminFeesService.getReverseFee(corporateFee.getKey(), adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .as(GetFeesResponseModel.class);

        assertEquals(corporateFee.getKey(), retrievedFee.getId());
        assertEquals(corporateFee.getValue().getTxId().getId(), retrievedFee.getTxId().get("id"));
        assertEquals("FEE_REVERSAL", retrievedFee.getTxId().get("type"));
        assertEquals(corporateFee.getValue().getNote(), retrievedFee.getNote());
        assertEquals(corporateFee.getValue().getInstrumentId().getType(), retrievedFee.getInstrumentId().get("type"));
        assertEquals(corporateFee.getValue().getInstrumentId().getId(), retrievedFee.getInstrumentId().get("id"));
    }

    @Test
    public void GetFee_CorporateManagedCardFee_Success(){

        final Map.Entry<String, ReverseFeeModel>
                corporateFee = corporateFees.entrySet().stream().filter(x -> x.getValue().getInstrumentId().getType().equals(MANAGED_CARDS.getValue())).findFirst().orElseThrow();

        final GetFeesResponseModel retrievedFee =
                AdminFeesService.getReverseFee(corporateFee.getKey(), adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .as(GetFeesResponseModel.class);

        assertEquals(corporateFee.getKey(), retrievedFee.getId());
        assertEquals(corporateFee.getValue().getTxId().getId(), retrievedFee.getTxId().get("id"));
        assertEquals("FEE_REVERSAL", retrievedFee.getTxId().get("type"));
        assertEquals(corporateFee.getValue().getNote(), retrievedFee.getNote());
        assertEquals(corporateFee.getValue().getInstrumentId().getType(), retrievedFee.getInstrumentId().get("type"));
        assertEquals(corporateFee.getValue().getInstrumentId().getId(), retrievedFee.getInstrumentId().get("id"));
    }

    @Test
    public void GetFee_ConsumerManagedAccountFee_Success(){

        final Map.Entry<String, ReverseFeeModel>
                consumerFee = consumerFees.entrySet().stream().filter(x -> x.getValue().getInstrumentId().getType().equals(MANAGED_ACCOUNTS.getValue())).findFirst().orElseThrow();

        final GetFeesResponseModel retrievedFee =
                AdminFeesService.getReverseFee(consumerFee.getKey(), adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .as(GetFeesResponseModel.class);

        assertEquals(consumerFee.getKey(), retrievedFee.getId());
        assertEquals(consumerFee.getValue().getTxId().getId(), retrievedFee.getTxId().get("id"));
        assertEquals("FEE_REVERSAL", retrievedFee.getTxId().get("type"));
        assertEquals(consumerFee.getValue().getNote(), retrievedFee.getNote());
        assertEquals(consumerFee.getValue().getInstrumentId().getType(), retrievedFee.getInstrumentId().get("type"));
        assertEquals(consumerFee.getValue().getInstrumentId().getId(), retrievedFee.getInstrumentId().get("id"));
    }

    @Test
    public void GetFee_ConsumerManagedCardFee_Success(){

        final Map.Entry<String, ReverseFeeModel>
                consumerFee = consumerFees.entrySet().stream().filter(x -> x.getValue().getInstrumentId().getType().equals(MANAGED_CARDS.getValue())).findFirst().orElseThrow();

        final GetFeesResponseModel retrievedFee =
                AdminFeesService.getReverseFee(consumerFee.getKey(), adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .as(GetFeesResponseModel.class);

        assertEquals(consumerFee.getKey(), retrievedFee.getId());
        assertEquals(consumerFee.getValue().getTxId().getId(), retrievedFee.getTxId().get("id"));
        assertEquals("FEE_REVERSAL", retrievedFee.getTxId().get("type"));
        assertEquals(consumerFee.getValue().getNote(), retrievedFee.getNote());
        assertEquals(consumerFee.getValue().getInstrumentId().getType(), retrievedFee.getInstrumentId().get("type"));
        assertEquals(consumerFee.getValue().getInstrumentId().getId(), retrievedFee.getInstrumentId().get("id"));
    }

    @Test
    public void GetFee_UnknownTransactionId_NotFound(){

        AdminFeesService.getReverseFee(RandomStringUtils.randomNumeric(18), adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"", "abc"})
    public void GetFee_InvalidId_NotFound(final String transactionId){

        AdminFeesService.getReverseFee(transactionId, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetFee_CrossTenantFee_NotFound(){

        final Map.Entry<String, ReverseFeeModel>
                consumerFee = consumerFees.entrySet().stream().filter(x -> x.getValue().getInstrumentId().getType().equals(MANAGED_CARDS.getValue())).findFirst().orElseThrow();

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String impersonatedToken = AdminService.impersonateTenant(innovator.getLeft(), AdminService.loginAdmin());

        AdminFeesService.getReverseFee(consumerFee.getKey(), impersonatedToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetFee_AdminTokenNoImpersonation_TokenNotImpersonated(){

        final Map.Entry<String, ReverseFeeModel>
                consumerFee = consumerFees.entrySet().stream().filter(x -> x.getValue().getInstrumentId().getType().equals(MANAGED_CARDS.getValue())).findFirst().orElseThrow();

        AdminFeesService.getReverseFee(consumerFee.getKey(), AdminService.loginAdmin())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TOKEN_NOT_IMPERSONATED"));
    }

    @Test
    public void GetFee_TenantTokenNoImpersonation_TokenNotImpersonated(){

        final Map.Entry<String, ReverseFeeModel>
                consumerFee = consumerFees.entrySet().stream().filter(x -> x.getValue().getInstrumentId().getType().equals(MANAGED_CARDS.getValue())).findFirst().orElseThrow();

        AdminFeesService.getReverseFee(consumerFee.getKey(), InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword))
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TOKEN_NOT_IMPERSONATED"));
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

    private static void reverseFee(final AdminFeeType fee,
                                   final String instrumentId,
                                   final ManagedInstrumentType instrumentType,
                                   final String currency,
                                   final IdentityType identityType){

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(fee)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(instrumentId, instrumentType))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(currency, 10L)))).build();

        final String transactionId =
                AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(fee.name(), transactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(instrumentId, instrumentType))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        final String reversalTransactionId =
                AdminFeesService.reverseFee(reverseFeeModel, adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("id");

        if (identityType.equals(IdentityType.CORPORATE)){
            corporateFees.put(reversalTransactionId, reverseFeeModel);
        } else {
            consumerFees.put(reversalTransactionId, reverseFeeModel);
        }
    }
}