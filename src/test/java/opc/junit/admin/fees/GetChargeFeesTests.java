package opc.junit.admin.fees;

import com.fasterxml.jackson.databind.ObjectMapper;
import opc.enums.opc.AdminFeeType;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedInstrumentType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.admin.ChargeFeeModel;
import opc.models.admin.FeeSpecModel;
import opc.models.admin.GetFeesModel;
import opc.models.admin.GetFeesResponseModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
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

import java.util.*;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GetChargeFeesTests extends BaseFeesSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static final Map<String, ChargeFeeModel> corporateFees = new HashMap<>();
    private static final Map<String, ChargeFeeModel> consumerFees = new HashMap<>();

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
            chargeFee(fee, corporateManagedAccountId, MANAGED_ACCOUNTS, corporateCurrency, IdentityType.CORPORATE);
            chargeFee(fee, corporateDebitManagedCard.getManagedCardId(), MANAGED_CARDS, corporateCurrency, IdentityType.CORPORATE);

            chargeFee(fee, consumerManagedAccountId, MANAGED_ACCOUNTS, consumerCurrency, IdentityType.CONSUMER);
            chargeFee(fee, consumerPrepaidManagedCard.getManagedCardId(), MANAGED_CARDS, consumerCurrency, IdentityType.CONSUMER);
        });
    }

    @Test
    public void GetFees_Corporate_Success(){

        final List<String> corporateFeeList = new ArrayList<>(corporateFees.keySet());

        final List<Object> retrievedFeeList =
                AdminFeesService.getChargeFees(new GetFeesModel(corporateFeeList), adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .getList("chargeFees");


        final List<GetFeesResponseModel> retrievedFees = new ArrayList<>();
        retrievedFeeList.forEach(x -> retrievedFees.add(new ObjectMapper().convertValue(x, GetFeesResponseModel.class)));

        assertEquals(corporateFees.size(), retrievedFees.size());

        corporateFees.forEach((key, value) -> {
            final GetFeesResponseModel retrievedFee =
                    retrievedFees.stream().filter(x -> x.getId().equals(key)).findFirst().orElseThrow();
            assertEquals(value.getFeeType().name(), retrievedFee.getFeeType());
            assertEquals(value.getNote(), retrievedFee.getNote());
            assertEquals(value.getSource().getType(), retrievedFee.getSource().get("type"));
            assertEquals(value.getSource().getId(), retrievedFee.getSource().get("id"));
            assertEquals(value.getFeeSpec().getFlatAmount().get(0).getCurrency(), retrievedFee.getFeeSpec().getFlatAmount().get(0).get("currency"));
            assertEquals(value.getFeeSpec().getFlatAmount().get(0).getAmount().toString(), retrievedFee.getFeeSpec().getFlatAmount().get(0).get("amount"));
        });
    }

    @Test
    public void GetFees_Consumer_Success(){

        final List<String> consumerFeeList = new ArrayList<>(consumerFees.keySet());

        final List<Object> retrievedFeeList =
                AdminFeesService.getChargeFees(new GetFeesModel(consumerFeeList), adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .getList("chargeFees");


        final List<GetFeesResponseModel> retrievedFees = new ArrayList<>();
        retrievedFeeList.forEach(x -> retrievedFees.add(new ObjectMapper().convertValue(x, GetFeesResponseModel.class)));

        assertEquals(consumerFees.size(), retrievedFees.size());

        consumerFees.forEach((key, value) -> {
            final GetFeesResponseModel retrievedFee =
                    retrievedFees.stream().filter(x -> x.getId().equals(key)).findFirst().orElseThrow();
            assertEquals(value.getFeeType().name(), retrievedFee.getFeeType());
            assertEquals(value.getNote(), retrievedFee.getNote());
            assertEquals(value.getSource().getType(), retrievedFee.getSource().get("type"));
            assertEquals(value.getSource().getId(), retrievedFee.getSource().get("id"));
            assertEquals(value.getFeeSpec().getFlatAmount().get(0).getCurrency(), retrievedFee.getFeeSpec().getFlatAmount().get(0).get("currency"));
            assertEquals(value.getFeeSpec().getFlatAmount().get(0).getAmount().toString(), retrievedFee.getFeeSpec().getFlatAmount().get(0).get("amount"));
        });
    }

    @Test
    public void GetFees_AllFees_Success(){

        final Map<String, ChargeFeeModel> allFees = new HashMap<>();
        allFees.putAll(corporateFees);
        allFees.putAll(consumerFees);

        final List<String> allFeesList = new ArrayList<>(allFees.keySet());

        final List<Object> retrievedFeeList =
                AdminFeesService.getChargeFees(new GetFeesModel(allFeesList), adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .getList("chargeFees");


        final List<GetFeesResponseModel> retrievedFees = new ArrayList<>();
        retrievedFeeList.forEach(x -> retrievedFees.add(new ObjectMapper().convertValue(x, GetFeesResponseModel.class)));

        assertEquals(allFees.size(), retrievedFees.size());

        allFees.forEach((key, value) -> {
            final GetFeesResponseModel retrievedFee =
                    retrievedFees.stream().filter(x -> x.getId().equals(key)).findFirst().orElseThrow();
            assertEquals(value.getFeeType().name(), retrievedFee.getFeeType());
            assertEquals(value.getNote(), retrievedFee.getNote());
            assertEquals(value.getSource().getType(), retrievedFee.getSource().get("type"));
            assertEquals(value.getSource().getId(), retrievedFee.getSource().get("id"));
            assertEquals(value.getFeeSpec().getFlatAmount().get(0).getCurrency(), retrievedFee.getFeeSpec().getFlatAmount().get(0).get("currency"));
            assertEquals(value.getFeeSpec().getFlatAmount().get(0).getAmount().toString(), retrievedFee.getFeeSpec().getFlatAmount().get(0).get("amount"));
        });
    }

    @Test
    public void GetFees_EmptyTransactionIdList_NoEntries(){

        final List<Object> retrievedFeeList =
                AdminFeesService.getChargeFees(new GetFeesModel(new ArrayList<>()), adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath().get("chargeFees");

        assertEquals(0, retrievedFeeList.size());
    }

    @Test
    public void GetFees_UnknownTransactionId_NoEntries(){

        final List<Object> retrievedFeeList =
                AdminFeesService.getChargeFees(new GetFeesModel(Collections.singletonList(RandomStringUtils.randomNumeric(18))), adminImpersonatedTenantToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath().get("chargeFees");

        assertEquals(0, retrievedFeeList.size());
    }

    @ParameterizedTest()
    @ValueSource(strings = {"", "abc"})
    public void GetFees_InvalidTransactionId_BadRequest(final String transactionId){

        AdminFeesService.getChargeFees(new GetFeesModel(Collections.singletonList(transactionId)), adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetFees_CrossTenantFeeList_NoEntries(){

        final Map<String, ChargeFeeModel> allFees = new HashMap<>();
        allFees.putAll(corporateFees);
        allFees.putAll(consumerFees);

        final List<String> allFeesList = new ArrayList<>(allFees.keySet());

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String impersonatedToken = AdminService.impersonateTenant(innovator.getLeft(), AdminService.loginAdmin());

        final List<Object> retrievedFeeList =
                AdminFeesService.getChargeFees(new GetFeesModel(allFeesList), impersonatedToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath().get("chargeFees");

        assertEquals(0, retrievedFeeList.size());
    }

    @Test
    public void GetFees_AdminTokenNoImpersonation_TokenNotImpersonated(){

        final Map<String, ChargeFeeModel> allFees = new HashMap<>();
        allFees.putAll(corporateFees);
        allFees.putAll(consumerFees);

        final List<String> allFeesList = new ArrayList<>(allFees.keySet());

        AdminFeesService.getChargeFees(new GetFeesModel(allFeesList), AdminService.loginAdmin())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TOKEN_NOT_IMPERSONATED"));
    }

    @Test
    public void GetFees_TenantTokenNoImpersonation_TokenNotImpersonated(){

        final Map<String, ChargeFeeModel> allFees = new HashMap<>();
        allFees.putAll(corporateFees);
        allFees.putAll(consumerFees);

        final List<String> allFeesList = new ArrayList<>(allFees.keySet());

        AdminFeesService.getChargeFees(new GetFeesModel(allFeesList), InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword))
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

    private static void chargeFee(final AdminFeeType fee,
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

        if (identityType.equals(IdentityType.CORPORATE)){
            corporateFees.put(transactionId, chargeFeeModel);
        } else {
            consumerFees.put(transactionId, chargeFeeModel);
        }
    }
}
