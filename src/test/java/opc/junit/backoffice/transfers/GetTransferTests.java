package opc.junit.backoffice.transfers;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.transfers.TransferFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.services.backoffice.multi.BackofficeMultiService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.TransfersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.*;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class GetTransferTests extends BaseTransfersSetup{
    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateId;
    private static String consumerId;
    private static Pair<String, CreateManagedAccountModel> corporateManagedAccount;
    private static Pair<String, CreateManagedCardModel> corporateManagedCard;
    private static List<Pair<String, CreateManagedAccountModel>> consumerManagedAccounts;
    private static final List<Pair<String, TransferFundsModel>> corporateTransferFunds = new ArrayList<>();
    private static final List<Pair<String, TransferFundsModel>> consumerTransferFunds = new ArrayList<>();
    private static String corporateImpersonateToken;
    private static String consumerImpersonateToken;

    @BeforeAll
    public static void Setup() {

        corporateSetup();
        consumerSetup();

        corporateManagedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);
        corporateManagedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);
        consumerManagedAccounts =
                createManagedAccounts(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken, 2);
        corporateDepositAndTransferMaToMc(corporateAuthenticationToken);
        consumerDepositAndTransferMaToMa(consumerAuthenticationToken);

        corporateImpersonateToken = BackofficeHelper.impersonateIdentity(corporateId, IdentityType.CORPORATE, secretKey);
        consumerImpersonateToken = BackofficeHelper.impersonateIdentity(consumerId, IdentityType.CONSUMER, secretKey);
    }

    @Test
    public void GetTransfer_Corporate_Success(){

        final Pair<String, TransferFundsModel> transfer = corporateTransferFunds.get(0);

        BackofficeMultiService.getTransfer(secretKey, transfer.getLeft(), corporateImpersonateToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(transfer.getLeft()))
                .body("profileId", equalTo(transfersProfileId))
                .body("tag", equalTo(transfer.getRight().getTag()))
                .body("source.type", equalTo(transfer.getRight().getSource().getType()))
                .body("source.id", equalTo(transfer.getRight().getSource().getId()))
                .body("destination.type", equalTo(transfer.getRight().getDestination().getType()))
                .body("destination.id", equalTo(transfer.getRight().getDestination().getId()))
                .body("destinationAmount.currency", equalTo(transfer.getRight().getDestinationAmount().getCurrency()))
                .body("destinationAmount.amount", equalTo(transfer.getRight().getDestinationAmount().getAmount().intValue()))
                .body("state", equalTo("COMPLETED"))
                .body("creationTimestamp", notNullValue())
                .body("description", notNullValue());
    }

    @Test
    public void GetTransfer_Consumer_Success(){

        final Pair<String, TransferFundsModel> transfer = consumerTransferFunds.get(0);

        BackofficeMultiService.getTransfer(secretKey, transfer.getLeft(), consumerImpersonateToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(transfer.getLeft()))
                .body("profileId", equalTo(transfersProfileId))
                .body("tag", equalTo(transfer.getRight().getTag()))
                .body("source.type", equalTo(transfer.getRight().getSource().getType()))
                .body("source.id", equalTo(transfer.getRight().getSource().getId()))
                .body("destination.type", equalTo(transfer.getRight().getDestination().getType()))
                .body("destination.id", equalTo(transfer.getRight().getDestination().getId()))
                .body("destinationAmount.currency", equalTo(transfer.getRight().getDestinationAmount().getCurrency()))
                .body("destinationAmount.amount", equalTo(transfer.getRight().getDestinationAmount().getAmount().intValue()))
                .body("state", equalTo("COMPLETED"))
                .body("creationTimestamp", notNullValue())
                .body("description", notNullValue());
    }

    @Test
    public void GetTransfer_UnknownTransfer_NotFound(){

        BackofficeMultiService.getTransfer(secretKey, RandomStringUtils.randomNumeric(18), consumerImpersonateToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetTransfer_InvalidTransferId_BadRequest(){

        BackofficeMultiService.getTransfer(secretKey, RandomStringUtils.randomAlphanumeric(6), consumerImpersonateToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("REGEX"))
                .body("validationErrors[0].fieldName", equalTo("id"));
    }

    @Test
    public void GetTransfer_InvalidApiKey_Unauthorised(){
        BackofficeMultiService.getTransfer("abc", consumerManagedAccounts.get(0).getLeft(), consumerImpersonateToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetTransfer_NoApiKey_BadRequest(){
        BackofficeMultiService.getTransfer("", consumerManagedAccounts.get(0).getLeft(), consumerImpersonateToken)
                .then().statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("REQUIRED"))
                .body("validationErrors[0].fieldName", equalTo("api-key"));
    }

    @Test
    public void GetTransfer_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        BackofficeMultiService.getTransfer(secretKey, corporateManagedAccount.getLeft(), corporateImpersonateToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetTransfer_RootUserLoggedOut_Forbidden(){
        final String token = ConsumersHelper.createUnauthenticatedConsumer(consumerProfileId, secretKey).getRight();

        BackofficeMultiService.getTransfer(secretKey, consumerManagedAccounts.get(0).getLeft(), token)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetTransfer_CrossIdentityChecks_NotFound(){

        BackofficeMultiService.getTransfer(secretKey, corporateTransferFunds.get(0).getLeft(), consumerImpersonateToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetTransfer_CorporateAuthToken_Forbidden(){

        BackofficeMultiService.getTransfer(secretKey, corporateTransferFunds.get(0).getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetTransfer_ConsumerAuthToken_Forbidden(){

        BackofficeMultiService.getTransfer(secretKey, consumerTransferFunds.get(0).getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private static void corporateDepositAndTransferMaToMc(final String token){
        fundManagedAccount(corporateManagedAccount.getLeft(), corporateCurrency, 10000L);

        IntStream.range(0, 2).forEach(i -> {
            final TransferFundsModel transferFundsModel =
                    TransferFundsModel.newBuilder()
                            .setProfileId(transfersProfileId)
                            .setTag(RandomStringUtils.randomAlphabetic(5))
                            .setDestinationAmount(new CurrencyAmount(corporateCurrency, 100L))
                            .setSource(new ManagedInstrumentTypeId(corporateManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                            .setDestination(new ManagedInstrumentTypeId(corporateManagedCard.getLeft(), MANAGED_CARDS))
                            .setDescription(RandomStringUtils.randomAlphabetic(10))
                            .build();

            final String id =
                    TransfersService.transferFunds(transferFundsModel, secretKey, token, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .extract()
                            .jsonPath()
                            .get("id");

            corporateTransferFunds.add(Pair.of(id, transferFundsModel));
        });
    }

    private static void consumerDepositAndTransferMaToMa(final String token){
        fundManagedAccount(consumerManagedAccounts.get(0).getLeft(), consumerCurrency, 10000L);

        IntStream.range(0, 2).forEach(i -> {
            final TransferFundsModel transferFundsModel =
                    TransferFundsModel.newBuilder()
                            .setProfileId(transfersProfileId)
                            .setTag(RandomStringUtils.randomAlphabetic(5))
                            .setDestinationAmount(new CurrencyAmount(consumerCurrency, 100L))
                            .setSource(new ManagedInstrumentTypeId(consumerManagedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                            .setDestination(new ManagedInstrumentTypeId(consumerManagedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                            .setDescription(RandomStringUtils.randomAlphabetic(10))
                            .build();

            final String id =
                    TransfersService.transferFunds(transferFundsModel, secretKey, token, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .extract()
                            .jsonPath()
                            .get("id");

            consumerTransferFunds.add(Pair.of(id, transferFundsModel));
        });
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);
    }

    private static void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }
}
