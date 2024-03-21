package opc.junit.multiprivate;

import commons.enums.Currency;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multiprivate.SendWithdrawalModel;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.services.multiprivate.MultiPrivateService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static opc.enums.opc.ManagedInstrumentType.UNKNOWN;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

public class SendWithdrawalTests extends BaseMultiPrivateSetup{
    private static String corporateAuthenticationToken;
    private static String corporateCurrency;

    @BeforeAll
    public static void Setup() {
        corporateSetup();
    }

    @Test
    public void SendWithdrawalOwt_Corporate_Success() {

        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        fundManagedAccount(managedAccount.getLeft(), corporateCurrency, depositAmount);

        final SendWithdrawalModel sendWithdrawalModel =
                SendWithdrawalModel.DefaultWithdrawalModel(outgoingWireTransfersProfileId,
                                managedAccount.getLeft(),
                                corporateCurrency, sendAmount)
                        .build();

        MultiPrivateService.sendWithdrawal(sendWithdrawalModel, fpiKey)
                .then()
                .statusCode(SC_OK);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class, names = {"USD", "GBP"})
    public void SendWithdrawalOwt_SepaUnsupportedCurrency_Conflict(final Currency currency) {

        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId,  currency.name(), corporateAuthenticationToken);

        fundManagedAccount(managedAccount.getLeft(),  currency.name(), depositAmount);

        final SendWithdrawalModel sendWithdrawalModel =
                SendWithdrawalModel.DefaultWithdrawalModel(outgoingWireTransfersProfileId,
                                managedAccount.getLeft(),
                                currency.name(), sendAmount)
                        .build();

        MultiPrivateService.sendWithdrawal(sendWithdrawalModel, fpiKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CURRENCY_MISMATCH"));
    }

    @Test
    public void SendWithdrawalOwt_NoFunds_FundsInsufficient(){

        final Long sendAmount = 200L;

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        final SendWithdrawalModel sendWithdrawalModel =
                SendWithdrawalModel.DefaultWithdrawalModel(outgoingWireTransfersProfileId,
                                managedAccount.getLeft(),
                                corporateCurrency, sendAmount)
                        .build();

        MultiPrivateService.sendWithdrawal(sendWithdrawalModel, fpiKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));
    }

    @Test
    public void SendWithdrawalOwt_UnknownSourceInstrumentId_SourceNotFound() {

        final SendWithdrawalModel sendWithdrawalModel =
                SendWithdrawalModel.DefaultWithdrawalModel(outgoingWireTransfersProfileId,
                                RandomStringUtils.randomNumeric(18),
                                corporateCurrency, 10L)
                        .build();

        MultiPrivateService.sendWithdrawal(sendWithdrawalModel, fpiKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_NOT_FOUND"));
    }

    @Test
    public void SendWithdrawalOwt_CorporateRandomFpiKey_Unauthorised(){

        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        fundManagedAccount(managedAccount.getLeft(), corporateCurrency, depositAmount);

        final SendWithdrawalModel sendWithdrawalModel =
                SendWithdrawalModel.DefaultWithdrawalModel(outgoingWireTransfersProfileId,
                                managedAccount.getLeft(),
                                corporateCurrency, sendAmount)
                        .build();

        MultiPrivateService.sendWithdrawal(sendWithdrawalModel, RandomStringUtils.randomAlphabetic(5))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendWithdrawalOwt_CorporateApiKey_Unauthorised(){

        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        fundManagedAccount(managedAccount.getLeft(), corporateCurrency, depositAmount);

        final SendWithdrawalModel sendWithdrawalModel =
                SendWithdrawalModel.DefaultWithdrawalModel(outgoingWireTransfersProfileId,
                                managedAccount.getLeft(),
                                corporateCurrency, sendAmount)
                        .build();

        MultiPrivateService.sendWithdrawal(sendWithdrawalModel, secretKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendWithdrawal_UnknownSourceInstrumentType_BadRequest() {

        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        fundManagedAccount(managedAccount.getLeft(), corporateCurrency, depositAmount);

        final SendWithdrawalModel sendWithdrawalModel =
                SendWithdrawalModel.DefaultWithdrawalModel(outgoingWireTransfersProfileId,
                                managedAccount.getLeft(),
                                corporateCurrency, sendAmount)
                        .sourceInstrument(new ManagedInstrumentTypeId(managedAccount.getLeft(),UNKNOWN))
                        .build();

        MultiPrivateService.sendWithdrawal(sendWithdrawalModel, fpiKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    private static void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }
}
