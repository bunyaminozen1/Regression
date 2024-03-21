package opc.junit.multi.sca;

import opc.enums.opc.OwtType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.junit.helpers.multi.SendsHelper;
import opc.junit.helpers.multi.TransfersHelper;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.multi.transfers.TransferFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.multi.OutgoingWireTransfersService;
import opc.services.multi.SendsService;
import opc.services.multi.TransfersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public abstract class AbstractTransfersScaTests extends BaseTransactionsScaSetup {

    protected abstract String getDestinationManagedCardScaAppId();
    protected abstract String getIdentityManagedAccountsProfileScaApp();
    protected abstract String getIdentityManagedCardsProfileScaApp();
    protected abstract String getDestinationManagedCardScaMaAppId();

    protected abstract String getIdentityManagedAccountsProfileScaMaApp();
    protected abstract String getIdentityManagedCardsProfileScaMaApp();

    protected abstract Pair<String, String> createSteppedUpIdentity(final ProgrammeDetailsModel programme);

    protected abstract Pair<String, String> createIdentity(final ProgrammeDetailsModel programme);

    protected final static String OTP_VERIFICATION_CODE = TestHelper.OTP_VERIFICATION_CODE;
    protected final static String CHANNEL = "SMS";
    protected final static String CURRENCY = "EUR";
    protected final static String INNOVATOR_ID = scaApp.getInnovatorId();
    private final static int SCA_EXPIRED_TIME = 61000;

    @Test
    public void GetSends_OneSessionStepUpTrue_Success() {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        sendFundsMaToMc(managedAccountId, getDestinationManagedCardScaMaAppId(), identity.getRight(),
                sendsProfileIdScaMaApp, secretKeyScaMaApp);

        SendsService.getSends(secretKeyScaMaApp, Optional.empty(), identity.getRight())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void GetSends_OneSessionStepUpFalse_Forbidden() {

        AdminHelper.setSca(adminToken, programmeIdScaApp, false, false);

        final Pair<String, String> identity = createIdentity(scaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaApp(), CURRENCY, secretKeyScaApp, identity.getRight(), INNOVATOR_ID);

        sendFundsMaToMc(managedAccountId, getDestinationManagedCardScaAppId(), identity.getRight(),
                sendsProfileIdScaApp, secretKeyScaApp);

        AdminHelper.setSca(adminToken, programmeIdScaApp, true, false);
        SendsService.getSends(secretKeyScaApp, Optional.empty(), identity.getRight())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetSends_StepUpExpired_Forbidden() throws InterruptedException {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        sendFundsMaToMc(managedAccountId, getDestinationManagedCardScaMaAppId(), identity.getRight(),
                sendsProfileIdScaMaApp, secretKeyScaMaApp);
        SendsHelper.getSends(secretKeyScaMaApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        SendsService.getSends(secretKeyScaMaApp, Optional.empty(), newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetSends_StepUpActive_Success() {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        sendFundsMaToMc(managedAccountId, getDestinationManagedCardScaMaAppId(), identity.getRight(),
                sendsProfileIdScaMaApp, secretKeyScaMaApp);
        SendsHelper.getSends(secretKeyScaMaApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        SendsService.getSends(secretKeyScaMaApp, Optional.empty(), newToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetSends_StepUpValidEntryLimit_SuccessLastEntry() throws InterruptedException {
// initial session: first transaction
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);
        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        sendFundsMaToMc(managedAccountId, getDestinationManagedCardScaMaAppId(), identity.getRight(),
                sendsProfileIdScaMaApp, secretKeyScaMaApp);
        SendsHelper.getSends(secretKeyScaMaApp, identity.getRight());

//second session: second transaction and new StepUp token
        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        final String secondSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        SendsHelper.getSendsForbidden(secretKeyScaMaApp, secondSessionToken);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMaApp, secondSessionToken);
        sendFundsMaToMc(managedAccountId, getDestinationManagedCardScaMaAppId(), secondSessionToken,
                sendsProfileIdScaMaApp, secretKeyScaMaApp);
        SendsHelper.getSends(secretKeyScaMaApp, secondSessionToken);

//third session: StepUp token from the second session (no step-up token is provided, SCA was performed in the last SCA_EXPIRED_TIME).
//Retrieve last transaction -> provided only transactions posted in the last SCA_EXPIRED_TIME
        AuthenticationHelper.logout(secondSessionToken, secretKeyScaMaApp);
        final String thirdSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("limit", 1);
        SendsService.getSends(secretKeyScaMaApp, Optional.of(filters), thirdSessionToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(2))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetSends_StepUpValidEntryLimit_ForbiddenAllEntries() throws InterruptedException {
// initial session: first transaction
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);
        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        sendFundsMaToMc(managedAccountId, getDestinationManagedCardScaMaAppId(), identity.getRight(),
                sendsProfileIdScaMaApp, secretKeyScaMaApp);
        SendsHelper.getSends(secretKeyScaMaApp, identity.getRight());

//second session: second transaction and new StepUp token
        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        final String secondSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        SendsHelper.getSendsForbidden(secretKeyScaMaApp, secondSessionToken);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMaApp, secondSessionToken);
        sendFundsMaToMc(managedAccountId, getDestinationManagedCardScaMaAppId(), secondSessionToken,
                sendsProfileIdScaMaApp, secretKeyScaMaApp);
        SendsHelper.getSends(secretKeyScaMaApp, secondSessionToken);

//third session: StepUp token from the second session (no step-up token is provided, SCA was performed in the last SCA_EXPIRED_TIME).
//Retrieve last transaction -> provided only transactions posted in the last SCA_EXPIRED_TIME
        AuthenticationHelper.logout(secondSessionToken, secretKeyScaMaApp);
        final String thirdSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("limit", 2);
        SendsService.getSends(secretKeyScaMaApp, Optional.of(filters), thirdSessionToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetSends_NewStepUp_Success() throws InterruptedException {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        sendFundsMaToMc(managedAccountId, getDestinationManagedCardScaMaAppId(), identity.getRight(),
                sendsProfileIdScaMaApp, secretKeyScaMaApp);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        final String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        SendsHelper.getSendsForbidden(secretKeyScaMaApp, newToken);

        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMaApp, newToken);
        SendsService.getSends(secretKeyScaMaApp, Optional.empty(), newToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetTransfers_OneSessionStepUpTrue_Success() {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());

        transferFundsMaToMc(managedAccountId, managedCardId, identity.getRight(),
                transfersProfileIdScaMaApp, secretKeyScaMaApp);

        TransfersService.getTransfers(secretKeyScaMaApp, Optional.empty(), identity.getRight())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void GetTransfers_OneSessionStepUpFalse_Forbidden() {

        final Pair<String, String> identity = createIdentity(scaApp);

        AdminHelper.setSca(adminToken, programmeIdScaApp, false, false);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaApp(), CURRENCY, secretKeyScaApp, identity.getRight(), INNOVATOR_ID);

        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaApp(), CURRENCY,
                secretKeyScaApp, identity.getRight());

        transferFundsMaToMc(managedAccountId, managedCardId, identity.getRight(),
                transfersProfileIdScaApp, secretKeyScaApp);

        AdminHelper.setSca(adminToken, programmeIdScaApp, true, false);
        TransfersService.getTransfers(secretKeyScaApp, Optional.empty(), identity.getRight())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetTransfers_StepUpExpired_Forbidden() throws InterruptedException {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());

        transferFundsMaToMc(managedAccountId, managedCardId, identity.getRight(),
                transfersProfileIdScaMaApp, secretKeyScaMaApp);

        TransfersHelper.getTransfers(secretKeyScaMaApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        final String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        TransfersService.getTransfers(secretKeyScaMaApp, Optional.empty(), newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetTransfers_StepUpActive_Success() {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());

        transferFundsMaToMc(managedAccountId, managedCardId, identity.getRight(),
                transfersProfileIdScaMaApp, secretKeyScaMaApp);

        TransfersHelper.getTransfers(secretKeyScaMaApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        final String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        TransfersService.getTransfers(secretKeyScaMaApp, Optional.empty(), newToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetTransfers_StepUpValidEntryLimit_SuccessLastEntry() throws InterruptedException {
// initial session: first transaction
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);
        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());

        transferFundsMaToMc(managedAccountId, managedCardId, identity.getRight(),
                transfersProfileIdScaMaApp, secretKeyScaMaApp);
        TransfersHelper.getTransfers(secretKeyScaMaApp, identity.getRight());

//second session: second transaction and new StepUp token
        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        final String secondSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        TransfersHelper.getTransfersForbidden(secretKeyScaMaApp, secondSessionToken);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMaApp, secondSessionToken);
        transferFundsMaToMc(managedAccountId, managedCardId, secondSessionToken,
                transfersProfileIdScaMaApp, secretKeyScaMaApp);
        TransfersHelper.getTransfers(secretKeyScaMaApp, secondSessionToken);

//third session: StepUp token from the second session (no step-up token is provided, SCA was performed in the last SCA_EXPIRED_TIME).
//Retrieve last transaction -> provided only transactions posted in the last SCA_EXPIRED_TIME
        AuthenticationHelper.logout(secondSessionToken, secretKeyScaMaApp);
        final String thirdSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("limit", 1);
        TransfersService.getTransfers(secretKeyScaMaApp, Optional.of(filters), thirdSessionToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(2))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetTransfers_StepUpValidEntryLimit_ForbiddenAllEntries() throws InterruptedException {
// initial session: first transaction
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);
        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());

        transferFundsMaToMc(managedAccountId, managedCardId, identity.getRight(),
                transfersProfileIdScaMaApp, secretKeyScaMaApp);
        TransfersHelper.getTransfers(secretKeyScaMaApp, identity.getRight());

//second session: second transaction and new StepUp token
        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        final String secondSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        TransfersHelper.getTransfersForbidden(secretKeyScaMaApp, secondSessionToken);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMaApp, secondSessionToken);
        transferFundsMaToMc(managedAccountId, managedCardId, secondSessionToken,
                transfersProfileIdScaMaApp, secretKeyScaMaApp);
        TransfersHelper.getTransfers(secretKeyScaMaApp, secondSessionToken);

//third session: StepUp token from the second session (no step-up token is provided, SCA was performed in the last SCA_EXPIRED_TIME).
//Retrieve last transaction -> provided only transactions posted in the last SCA_EXPIRED_TIME
        AuthenticationHelper.logout(secondSessionToken, secretKeyScaMaApp);
        final String thirdSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("limit", 2);
        TransfersService.getTransfers(secretKeyScaMaApp, Optional.of(filters), thirdSessionToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetTransfers_NewStepUp_Success() throws InterruptedException {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());

        transferFundsMaToMc(managedAccountId, managedCardId, identity.getRight(),
                transfersProfileIdScaMaApp, secretKeyScaMaApp);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        TransfersHelper.getTransfersForbidden(secretKeyScaMaApp, newToken);

        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMaApp, newToken);
        TransfersService.getTransfers(secretKeyScaMaApp, Optional.empty(), newToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetOutgoingWireTransfers_OneSessionStepUpTrue_Success() {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        sendOwt(managedAccountId, identity.getRight(),
                innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp, secretKeyScaMaApp);

        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKeyScaMaApp, Optional.empty(), identity.getRight())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void GetOutgoingWireTransfers_OneSessionStepUpFalse_Forbidden() {
        final Pair<String, String> identity = createIdentity(scaApp);

        AdminHelper.setSca(adminToken, programmeIdScaApp, false, false);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaApp(), CURRENCY,
                secretKeyScaApp, identity.getRight(), INNOVATOR_ID);

        sendOwt(managedAccountId, identity.getRight(),
                innovatorIdScaApp, outgoingWireTransfersProfileIdScaApp, secretKeyScaApp);

        AdminHelper.setSca(adminToken, programmeIdScaApp, true, false);
        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKeyScaApp, Optional.empty(), identity.getRight())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetOutgoingWireTransfers_StepUpExpired_Forbidden() throws InterruptedException {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        sendOwt(managedAccountId , identity.getRight(),
                innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp, secretKeyScaMaApp);

        OutgoingWireTransfersHelper.getOutgoingWireTransfers(secretKeyScaMaApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        final String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKeyScaMaApp, Optional.empty(), newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetOutgoingWireTransfers_StepUpActive_Success() {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        sendOwt(managedAccountId, identity.getRight(),
                innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp, secretKeyScaMaApp);

        OutgoingWireTransfersHelper.getOutgoingWireTransfers(secretKeyScaMaApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        final String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKeyScaMaApp, Optional.empty(), newToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetOutgoingWireTransfers_StepUpValidEntryLimit_SuccessLastEntry() throws InterruptedException {
// initial session: first transaction
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        sendOwt(managedAccountId, identity.getRight(),
                innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp, secretKeyScaMaApp);
        OutgoingWireTransfersHelper.getOutgoingWireTransfers(secretKeyScaMaApp, identity.getRight());

//second session: second transaction and new StepUp token
        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        final String secondSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        OutgoingWireTransfersHelper.getOutgoingWireTransfersForbidden(secretKeyScaMaApp, secondSessionToken);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMaApp, secondSessionToken);
        sendOwt(managedAccountId, secondSessionToken,
                innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp, secretKeyScaMaApp);
        OutgoingWireTransfersHelper.getOutgoingWireTransfers(secretKeyScaMaApp, secondSessionToken);

//third session: StepUp token from the second session (no step-up token is provided, SCA was performed in the last SCA_EXPIRED_TIME).
//Retrieve last transaction -> provided only transactions posted in the last SCA_EXPIRED_TIME
        AuthenticationHelper.logout(secondSessionToken, secretKeyScaMaApp);
        final String thirdSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("limit", 1);
        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKeyScaMaApp, Optional.of(filters), thirdSessionToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(2))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetOutgoingWireTransfers_StepUpValidEntryLimit_ForbiddenAllEntries() throws InterruptedException {
// initial session: first transaction
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        sendOwt(managedAccountId, identity.getRight(),
                innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp, secretKeyScaMaApp);
        OutgoingWireTransfersHelper.getOutgoingWireTransfers(secretKeyScaMaApp, identity.getRight());

//second session: second transaction and new StepUp token
        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        final String secondSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        OutgoingWireTransfersHelper.getOutgoingWireTransfersForbidden(secretKeyScaMaApp, secondSessionToken);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMaApp, secondSessionToken);
        sendOwt(managedAccountId, secondSessionToken,
                innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp, secretKeyScaMaApp);
        OutgoingWireTransfersHelper.getOutgoingWireTransfers(secretKeyScaMaApp, secondSessionToken);

//third session: StepUp token from the second session (no step-up token is provided, SCA was performed in the last SCA_EXPIRED_TIME).
//Retrieve last transaction -> provided only transactions posted in the last SCA_EXPIRED_TIME
        AuthenticationHelper.logout(secondSessionToken, secretKeyScaMaApp);
        final String thirdSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("limit", 2);

        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKeyScaMaApp, Optional.of(filters), thirdSessionToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetOutgoingWireTransfers_NewStepUp_Success() throws InterruptedException {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        sendOwt(managedAccountId, identity.getRight(),
                innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp, secretKeyScaMaApp);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        OutgoingWireTransfersHelper.getOutgoingWireTransfersForbidden(secretKeyScaMaApp, newToken);

        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMaApp, newToken);
        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKeyScaMaApp, Optional.empty(), newToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetOutgoingWireTransferById_OneSessionStepUpTrue_Success() {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        final String owtId = sendOwt(managedAccountId, identity.getRight(),
                innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp, secretKeyScaMaApp);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKeyScaMaApp, owtId, identity.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(owtId));
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void GetOutgoingWireTransferById_OneSessionStepUpFalse_Forbidden() {
        final Pair<String, String> identity = createIdentity(scaApp);

        AdminHelper.setSca(adminToken, programmeIdScaApp, false, false);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaApp(), CURRENCY,
                secretKeyScaApp, identity.getRight(), INNOVATOR_ID);

        final String owtId = sendOwt(managedAccountId, identity.getRight(),
                innovatorIdScaApp, outgoingWireTransfersProfileIdScaApp, secretKeyScaApp);

        AdminHelper.setSca(adminToken, programmeIdScaApp, true, false);
        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKeyScaApp, owtId, identity.getRight())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetOutgoingWireTransferById_StepUpExpired_Forbidden() throws InterruptedException {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        final String owtId = sendOwt(managedAccountId, identity.getRight(),
                innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp, secretKeyScaMaApp);

        OutgoingWireTransfersHelper.getOutgoingWireTransfer(secretKeyScaMaApp, owtId, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKeyScaMaApp, owtId, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetOutgoingWireTransferById_StepUpActive_Success() {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        final String owtId = sendOwt(managedAccountId, identity.getRight(),
                innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp, secretKeyScaMaApp);

        OutgoingWireTransfersHelper.getOutgoingWireTransfer(secretKeyScaMaApp, owtId, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKeyScaMaApp, owtId, newToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(owtId));
    }

    @Test
    public void GetOutgoingWireTransferById_NewStepUp_Success() throws InterruptedException {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        final String owtId = sendOwt(managedAccountId, identity.getRight(),
                innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp, secretKeyScaMaApp);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        OutgoingWireTransfersHelper.getOutgoingWireTransferForbidden(secretKeyScaMaApp, owtId, newToken);

        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMaApp, newToken);
        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKeyScaMaApp, owtId, newToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(owtId));
    }

    @Test
    public void GetOutgoingWireTransferById_TransactionExpired_Forbidden() throws InterruptedException {
// initial session: first transaction
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        final String owtId = sendOwt(managedAccountId, identity.getRight(),
                innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp, secretKeyScaMaApp);
        OutgoingWireTransfersHelper.getOutgoingWireTransfer(secretKeyScaMaApp, owtId, identity.getRight());

//second session: new StepUp token
        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        final String secondSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        OutgoingWireTransfersHelper.getOutgoingWireTransferForbidden(secretKeyScaMaApp, owtId, secondSessionToken);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMaApp, secondSessionToken);
        OutgoingWireTransfersHelper.getOutgoingWireTransfer(secretKeyScaMaApp, owtId, secondSessionToken);

//third session: StepUp token from the second session transaction was posted over SCA_EXPIRED_TIME
        AuthenticationHelper.logout(secondSessionToken, secretKeyScaMaApp);
        final String thirdSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKeyScaMaApp, owtId, thirdSessionToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetTransferById_OneSessionStepUpTrue_Success() {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());

        final String transactionId = transferFundsMaToMc(managedAccountId, managedCardId, identity.getRight(),
                transfersProfileIdScaMaApp, secretKeyScaMaApp);

        TransfersService.getTransfer(secretKeyScaMaApp, transactionId, identity.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(transactionId));
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void GetTransferById_OneSessionStepUpFalse_Forbidden() {
        final Pair<String, String> identity = createIdentity(scaApp);

        AdminHelper.setSca(adminToken, programmeIdScaApp, false, false);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaApp(), CURRENCY, secretKeyScaApp, identity.getRight(), INNOVATOR_ID);

        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaApp(), CURRENCY,
                secretKeyScaApp, identity.getRight());

        final String transactionId = transferFundsMaToMc(managedAccountId, managedCardId, identity.getRight(),
                transfersProfileIdScaApp, secretKeyScaApp);

        AdminHelper.setSca(adminToken, programmeIdScaApp, true, false);
        TransfersService.getTransfer(secretKeyScaApp, transactionId, identity.getRight())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetTransferById_StepUpExpired_Forbidden() throws InterruptedException {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());

        final String transactionId = transferFundsMaToMc(managedAccountId, managedCardId, identity.getRight(),
                transfersProfileIdScaMaApp, secretKeyScaMaApp);

        TransfersHelper.getTransfer(secretKeyScaMaApp, transactionId, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        TransfersService.getTransfer(secretKeyScaMaApp, transactionId, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetTransferById_StepUpActive_Success() {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());

        final String transactionId = transferFundsMaToMc(managedAccountId, managedCardId, identity.getRight(),
                transfersProfileIdScaMaApp, secretKeyScaMaApp);

        TransfersHelper.getTransfer(secretKeyScaMaApp, transactionId, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        TransfersService.getTransfer(secretKeyScaMaApp, transactionId, newToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(transactionId));
    }

    @Test
    public void GetTransferById_NewStepUp_Success() throws InterruptedException {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());
        
        final String transactionId = transferFundsMaToMc(managedAccountId, managedCardId, identity.getRight(),
                transfersProfileIdScaMaApp, secretKeyScaMaApp);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        TransfersHelper.getTransferForbidden(secretKeyScaMaApp, transactionId, newToken);

        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMaApp, newToken);
        TransfersService.getTransfer(secretKeyScaMaApp, transactionId, newToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(transactionId));
    }

    @Test
    public void GetTransferById_TransactionExpired_Forbidden() throws InterruptedException {
// initial session: first transaction
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);
        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);

        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());

        final String transactionId = transferFundsMaToMc(managedAccountId, managedCardId, identity.getRight(),
                transfersProfileIdScaMaApp, secretKeyScaMaApp);
        TransfersHelper.getTransfer(secretKeyScaMaApp, transactionId, identity.getRight());

//second session: new StepUp token
        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        final String secondSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        TransfersHelper.getTransferForbidden(secretKeyScaMaApp, transactionId, secondSessionToken);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMaApp, secondSessionToken);
        TransfersHelper.getTransfer(secretKeyScaMaApp, transactionId, secondSessionToken);

//third session: StepUp token from the second session transaction was posted over SCA_EXPIRED_TIME
        AuthenticationHelper.logout(secondSessionToken, secretKeyScaMaApp);
        final String thirdSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        TransfersService.getTransfer(secretKeyScaMaApp, transactionId, thirdSessionToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetSendById_OneSessionStepUpTrue_Success() {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);
        final String transactionId = sendFundsMaToMc(managedAccountId, getDestinationManagedCardScaMaAppId(), identity.getRight(),
                sendsProfileIdScaMaApp, secretKeyScaMaApp);

        SendsService.getSend(secretKeyScaMaApp, transactionId, identity.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(transactionId));
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void GetSendById_OneSessionStepUpFalse_Forbidden() {

        AdminHelper.setSca(adminToken, programmeIdScaApp, false, false);

        final Pair<String, String> identity = createIdentity(scaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaApp(), CURRENCY, secretKeyScaApp, identity.getRight(), INNOVATOR_ID);
        final String transactionId = sendFundsMaToMc(managedAccountId, getDestinationManagedCardScaAppId(), identity.getRight(),
                sendsProfileIdScaApp, secretKeyScaApp);

        AdminHelper.setSca(adminToken, programmeIdScaApp, true, false);
        SendsService.getSend(secretKeyScaApp, transactionId, identity.getRight())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetSendById_StepUpExpired_Forbidden() throws InterruptedException {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);
        final String transactionId = sendFundsMaToMc(managedAccountId, getDestinationManagedCardScaMaAppId(), identity.getRight(),
                sendsProfileIdScaMaApp, secretKeyScaMaApp);

        SendsHelper.getSend(secretKeyScaMaApp, transactionId, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        SendsService.getSend(secretKeyScaMaApp, transactionId, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetSendById_StepUpActive_Success() {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);
        final String transactionId = sendFundsMaToMc(managedAccountId, getDestinationManagedCardScaMaAppId(), identity.getRight(),
                sendsProfileIdScaMaApp, secretKeyScaMaApp);

        SendsHelper.getSend(secretKeyScaMaApp, transactionId, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        SendsService.getSend(secretKeyScaMaApp, transactionId, newToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(transactionId));
    }

    @Test
    public void GetSendById_NewStepUp_Success() throws InterruptedException {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);
        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);
        final String transactionId = sendFundsMaToMc(managedAccountId, getDestinationManagedCardScaMaAppId(), identity.getRight(),
                sendsProfileIdScaMaApp, secretKeyScaMaApp);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        SendsHelper.getSendForbidden(secretKeyScaMaApp, transactionId, newToken);

        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMaApp, newToken);
        SendsService.getSend(secretKeyScaMaApp, transactionId, newToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(transactionId));
    }

    @Test
    public void GetSendById_TransactionExpired_Forbidden() throws InterruptedException {
// initial session: first transaction
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);
        final String managedAccountId = ManagedAccountsHelper.createManagedAccountWithAdjustment(getIdentityManagedAccountsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight(), INNOVATOR_ID);
        final String transactionId = sendFundsMaToMc(managedAccountId, getDestinationManagedCardScaMaAppId(), identity.getRight(),
                sendsProfileIdScaMaApp, secretKeyScaMaApp);
        SendsHelper.getSend(secretKeyScaMaApp, transactionId, identity.getRight());

//second session: new StepUp token
        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        final String secondSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        SendsHelper.getSendForbidden(secretKeyScaMaApp, transactionId, secondSessionToken);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMaApp, secondSessionToken);
        SendsHelper.getSend(secretKeyScaMaApp, transactionId, secondSessionToken);

//third session: StepUp token from the second session transaction was posted over SCA_EXPIRED_TIME
        AuthenticationHelper.logout(secondSessionToken, secretKeyScaMaApp);
        final String thirdSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        SendsService.getSend(secretKeyScaMaApp, transactionId, thirdSessionToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }


    private static String sendFundsMaToMc(final String managedAccountId,
                                          final String managedCardId,
                                          final String identityToken,
                                          final String sendsProfileId,
                                          final String secretKey) {

        final SendFundsModel sendFundsModel = SendFundsModel.newBuilder()
                .setProfileId(sendsProfileId)
                .setTag(RandomStringUtils.randomAlphabetic(5))
                .setDestinationAmount(new CurrencyAmount(CURRENCY, 100L))
                .setSource(new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS))
                .setDestination(new ManagedInstrumentTypeId(managedCardId, MANAGED_CARDS))
                .build();

        return SendsService.sendFunds(sendFundsModel, secretKey, identityToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");
    }

    private static String transferFundsMaToMc(final String managedAccountId,
                                              final String managedCardId,
                                              final String identityToken,
                                              final String transfersProfileId,
                                              final String secretKey) {

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(CURRENCY, 100L))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCardId, MANAGED_CARDS))
                        .build();

        return TransfersService.transferFunds(transferFundsModel, secretKey, identityToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");
    }

    private static String sendOwt(final String managedAccountId,
                                  final String identityToken,
                                  final String innovatorId,
                                  final String outgoingWireTransfersProfileId,
                                  final String secretKey) {

        AdminHelper.fundManagedAccount(innovatorId, managedAccountId, CURRENCY, 1000L);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        CURRENCY, 100L, OwtType.SEPA).build();

        return OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, identityToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");
    }
}
