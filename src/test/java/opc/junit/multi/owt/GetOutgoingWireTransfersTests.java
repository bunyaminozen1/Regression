package opc.junit.multi.owt;

import io.restassured.response.ValidatableResponse;
import commons.enums.Currency;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.OwtType;
import commons.enums.State;
import opc.junit.database.OwtDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multi.outgoingwiretransfers.SepaBankDetailsModel;
import opc.models.shared.VerificationModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.OutgoingWireTransfersService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag(MultiTags.OWT)
public class GetOutgoingWireTransfersTests extends BaseOutgoingWireTransfersSetup {

    private static final String CHANNEL = EnrolmentChannel.SMS.name();
    private static final String VERIFICATION_CODE = "123456";
    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateId;
    private static String consumerId;
    private static Pair<String, CreateManagedAccountModel> corporateManagedAccount;
    private static Pair<String, CreateManagedAccountModel> consumerManagedAccount;
    private static final List<Pair<String, OutgoingWireTransfersModel>> corporateOWTs = new ArrayList<>();
    private static final List<Pair<String, OutgoingWireTransfersModel>> consumerOWTs = new ArrayList<>();

    @BeforeAll
    public static void Setup() throws SQLException {
        corporateSetup();
        consumerSetup();

        corporateManagedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);
        consumerManagedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);
        corporateDepositAndSendOwt(corporateAuthenticationToken);
        consumerDepositAndSendOwt(consumerAuthenticationToken);

        ensureOWTsAreCompleted(corporateOWTs.get(0).getRight().getSourceInstrument().getId());
        ensureOWTsAreCompleted(consumerOWTs.get(0).getRight().getSourceInstrument().getId());

        Collections.reverse(corporateOWTs);
        Collections.reverse(consumerOWTs);
    }

    @Test
    public void GetOWTs_Corporate_Success(){
        final ValidatableResponse response =
                OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.empty(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("count", equalTo(corporateOWTs.size()))
                        .body("responseCount", equalTo(corporateOWTs.size()));

        assertSuccessfulResponse(response, corporateOWTs);
    }

    @Test
    public void GetOWTs_Consumer_Success(){
        final ValidatableResponse response =
                OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.empty(), consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("count", equalTo(consumerOWTs.size()))
                        .body("responseCount", equalTo(consumerOWTs.size()));

        assertSuccessfulResponse(response, consumerOWTs);
    }

    @Test
    public void GetOWTs_WithAllFilters_Success(){
        final OutgoingWireTransfersModel expectedFunds =
                corporateOWTs.get(0).getRight();

        final Map<String, Object> filters = new HashMap<>();
        filters.put("offset", 0);
        filters.put("limit", 1);
        filters.put("profileId", outgoingWireTransfersProfileId);
        filters.put("instrumentId", expectedFunds.getSourceInstrument().getId());
        filters.put("state", Collections.singletonList(State.COMPLETED));
        filters.put("createdFrom", Instant.now().minus(5, ChronoUnit.MINUTES).toEpochMilli());
        filters.put("createdTo", Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli());
        filters.put("tag", expectedFunds.getTag());
        filters.put("sourceInstrument.type", MANAGED_ACCOUNTS);
        filters.put("sourceInstrument.id",expectedFunds.getSourceInstrument().getId());
        final ValidatableResponse response =
            OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.of(filters), corporateAuthenticationToken)
                    .then()
                    .statusCode(SC_OK)
                    .body("count", equalTo(1))
                    .body("responseCount", equalTo(1));

        assertSuccessfulResponse(response, Collections.singletonList(corporateOWTs.get(0)));
    }

    @Test
    public void GetOWTs_FilterByPendingChallengeState_Success(){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> corporate =
                CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, secretKey);

        final String managedAccountId =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), corporate.getRight())
                        .getLeft();

        fundManagedAccount(managedAccountId, Currency.EUR.name(), 100000L);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        createCorporateModel.getBaseCurrency(), 100L, OwtType.SEPA).build();

        final String id =
                OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel,
                        secretKey, corporate.getRight(), Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("id");

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(State.PENDING_CHALLENGE));

        final ValidatableResponse response =
                OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.of(filters), corporate.getRight())
                        .then()
                        .statusCode(SC_OK)
                        .body("count", equalTo(1))
                        .body("responseCount", equalTo(1));

        assertSuccessfulResponse(response, Collections.singletonList(Pair.of(id, outgoingWireTransfersModel)), State.PENDING_CHALLENGE);

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, EnrolmentChannel.SMS.name(), secretKey, corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse responseAfterStartVerification =
                OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.of(filters), corporate.getRight())
                        .then()
                        .statusCode(SC_OK)
                        .body("count", equalTo(1))
                        .body("responseCount", equalTo(1));

        assertSuccessfulResponse(responseAfterStartVerification, Collections.singletonList(Pair.of(id, outgoingWireTransfersModel)), State.PENDING_CHALLENGE);
    }

    @Test
    public void GetOWTs_LimitFilterCheck_Success(){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("limit", 1);

        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.of(filters), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("transfer[0].id", equalTo(corporateOWTs.get(0).getLeft()))
                .body("count", equalTo(corporateOWTs.size()))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetOWTs_FilterByUnknownState_ReturnAll(){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(State.UNKNOWN));

        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.of(filters), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(corporateOWTs.size()))
                .body("responseCount", equalTo(corporateOWTs.size()));
    }

    @Test
    public void GetOWTs_FilterOutState_Success(){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(State.REJECTED));

        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.of(filters), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetOWTs_FilterMultipleOWTs_Success(){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("profileId", outgoingWireTransfersProfileId);
        filters.put("instrumentId", consumerOWTs.get(0).getRight().getSourceInstrument().getId());

        final ValidatableResponse response =
            OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.of(filters), consumerAuthenticationToken)
                    .then()
                    .statusCode(SC_OK)
                    .body("count", equalTo(consumerOWTs.size()))
                    .body("responseCount", equalTo(consumerOWTs.size()));

        assertSuccessfulResponse(response, consumerOWTs);
    }

    @Test
    public void GetOWTs_NoEntries_Success(){
        final Map<String, Object> filters = new HashMap<>();
        filters.put("createdFrom", Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli());

        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.of(filters), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetOWTs_InvalidApiKey_Unauthorised(){
        OutgoingWireTransfersService.getOutgoingWireTransfers("abc", Optional.empty(), corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetOWTs_NoApiKey_BadRequest(){
        OutgoingWireTransfersService.getOutgoingWireTransfers("", Optional.empty(), corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetOWTs_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.empty(), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetOWTs_RootUserLoggedOut_Unauthorised(){
        final String token = ConsumersHelper.createUnauthenticatedConsumer(consumerProfileId, secretKey).getRight();

        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.empty(), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetOWTs_BackofficeCorporateImpersonation_Forbidden(){
        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.empty(), getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetOWTs_BackofficeConsumerImpersonation_Forbidden(){
        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.empty(), getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetOWTs_SourceInstrumentTypeMissing_BadRequest(){
        final OutgoingWireTransfersModel expectedFunds =
                corporateOWTs.get(0).getRight();
        final Map<String, Object> filters = new HashMap<>();
        filters.put("sourceInstrument.id", expectedFunds.getSourceInstrument().getId());
        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.of(filters), corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetOWTs_SourceInstrumentIdMissing_BadRequest(){
        final Map<String, Object> filters = new HashMap<>();
        filters.put("sourceInstrument.type", MANAGED_ACCOUNTS);
        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.of(filters), corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    private void assertSuccessfulResponse(final ValidatableResponse response,
                                          final List<Pair<String, OutgoingWireTransfersModel>> transfers) {
        assertSuccessfulResponse(response, transfers, State.COMPLETED);
    }

    private void assertSuccessfulResponse(final ValidatableResponse response,
                                          final List<Pair<String, OutgoingWireTransfersModel>> transfers,
                                          final State state) {
        IntStream.range(0, transfers.size()).forEach(i ->
                response.body(String.format("transfer[%s].id", i), notNullValue())
                        .body(String.format("transfer[%s].profileId", i), equalTo(transfers.get(i).getRight().getProfileId()))
                        .body(String.format("transfer[%s].tag", i), equalTo(transfers.get(i).getRight().getTag()))
                        .body(String.format("transfer[%s].sourceInstrument.type", i), equalTo(transfers.get(i).getRight().getSourceInstrument().getType()))
                        .body(String.format("transfer[%s].sourceInstrument.id", i), equalTo(transfers.get(i).getRight().getSourceInstrument().getId()))
                        .body(String.format("transfer[%s].transferAmount.currency", i), equalTo(transfers.get(i).getRight().getTransferAmount().getCurrency()))
                        .body(String.format("transfer[%s].transferAmount.amount", i), equalTo(transfers.get(i).getRight().getTransferAmount().getAmount().intValue()))
                        .body(String.format("transfer[%s].description", i), equalTo(transfers.get(i).getRight().getDescription()))
                        .body(String.format("transfer[%s].destination.name", i), equalTo(transfers.get(i).getRight().getDestinationBeneficiary().getName()))
                        .body(String.format("transfer[%s].destination.address", i), equalTo(transfers.get(i).getRight().getDestinationBeneficiary().getAddress()))
                        .body(String.format("transfer[%s].destination.bankName", i), equalTo(transfers.get(i).getRight().getDestinationBeneficiary().getBankName()))
                        .body(String.format("transfer[%s].destination.bankAddress", i), equalTo(transfers.get(i).getRight().getDestinationBeneficiary().getBankAddress()))
                        .body(String.format("transfer[%s].destination.bankCountry", i), equalTo(transfers.get(i).getRight().getDestinationBeneficiary().getBankCountry()))
                        .body(String.format("transfer[%s].state", i), equalTo(state.name()))
                        .body(String.format("transfer[%s].creationTimestamp", i), notNullValue()));

        assertBankAccountDetails(response, transfers);
    }

    private void assertBankAccountDetails(final ValidatableResponse response,
                                          final List<Pair<String, OutgoingWireTransfersModel>> transfers) {

        IntStream.range(0, transfers.size()).forEach(i ->{
            final SepaBankDetailsModel sepaBankDetails =
                    (SepaBankDetailsModel) transfers.get(i).getRight().getDestinationBeneficiary().getBankAccountDetails();

            response
                    .body(String.format("transfer[%s].type", i), equalTo(OwtType.SEPA.name()))
                    .body(String.format("transfer[%s].destination.bankAccountDetails.iban", i), equalTo(sepaBankDetails.getIban()))
                    .body(String.format("transfer[%s].destination.bankAccountDetails.bankIdentifierCode", i), equalTo(sepaBankDetails.getBankIdentifierCode()));
        });
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency("EUR").build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, CHANNEL, secretKey, consumerAuthenticationToken);
    }

    private static void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency("EUR").build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, CHANNEL, secretKey, corporateAuthenticationToken);
    }

    private static void corporateDepositAndSendOwt(final String token){
        fundManagedAccount(corporateManagedAccount.getLeft(), corporateCurrency, 10000L);

        IntStream.range(0, 2).forEach(i -> {
            final OutgoingWireTransfersModel outgoingWireTransfersModel =
                    OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                            corporateManagedAccount.getLeft(),
                            corporateCurrency, 100L, OwtType.SEPA).build();

            final String id =
                    OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, token, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .extract()
                            .jsonPath()
                            .get("id");

            verifyOwt(id, token);

            corporateOWTs.add(Pair.of(id, outgoingWireTransfersModel));
        });
    }

    private static void consumerDepositAndSendOwt(final String token){
        fundManagedAccount(consumerManagedAccount.getLeft(), consumerCurrency, 10000L);

        IntStream.range(0, 2).forEach(i -> {
            final OutgoingWireTransfersModel outgoingWireTransfersModel =
                    OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                            consumerManagedAccount.getLeft(),
                            consumerCurrency, 100L, OwtType.SEPA).build();

            final String id =
                    OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, token, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .extract()
                            .jsonPath()
                            .get("id");

            verifyOwt(id, token);

            consumerOWTs.add(Pair.of(id, outgoingWireTransfersModel));
        });
    }

    private static void ensureOWTsAreCompleted(final String sourceInstrumentId) throws SQLException {

        final List<String> owtTransferIds = new ArrayList<>();
        final Map<Integer, Map<String, String>> owtTransfers = OwtDatabaseHelper.getOwt(sourceInstrumentId);
        owtTransferIds.add(owtTransfers.get(0).get("id"));
        owtTransferIds.add(owtTransfers.get(1).get("id"));

        owtTransferIds.forEach(transferId -> OutgoingWireTransfersHelper.checkOwtStateById(transferId, "COMPLETED"));
    }

    private static void verifyOwt(final String id,
                           final String token) {

        TestHelper.ensureAsExpected(15,
                () -> OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, CHANNEL, secretKey, token),
                SC_NO_CONTENT);

        TestHelper.ensureAsExpected(15,
                () -> OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), id,
                        CHANNEL, secretKey, token),
                SC_NO_CONTENT);
    }
}
