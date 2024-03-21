package spi.openbanking.services;

import commons.services.BaseService;
import io.restassured.response.Response;
import spi.openbanking.models.CreateAccountAuthorisationModel;
import spi.openbanking.models.CreateOutgoingWireTransferAuthorisationModel;

public class OpenBankingService extends BaseService {

    public static Response getInstitutions(final String authenticationToken){
        return getAuthorisationKeyRequest(authenticationToken)
                .when()
                .get("/open-banking/1.0/institutions");
    }

    public static Response extendConsent(final String consentId,
                                         final String authenticationToken){
        return getAuthorisationKeyRequest(authenticationToken)
                .pathParam("id", consentId)
                .when()
                .post("/open-banking/1.0/consents/{id}/extend");
    }

    public static Response getOutgoingWireTransfer(final String owtId,
                                                   final String authenticationToken){
        return getAuthorisationKeyRequest(authenticationToken)
                .pathParam("id", owtId)
                .when()
                .get("/open-banking/1.0/outgoing-wire-transfers/{id}");
    }

    public static Response createOutgoingWireTransferAuthorisation(final CreateOutgoingWireTransferAuthorisationModel createOutgoingWireTransferAuthorisationModel,
                                                                   final String authenticationToken){
        return getBodyAuthorisationKeyRequest(createOutgoingWireTransferAuthorisationModel, authenticationToken)
                .when()
                .post("/open-banking/1.0/outgoing-wire-transfers/auth");
    }

    public static Response createAccountAuthorisation(final CreateAccountAuthorisationModel createAccountAuthorisationModel,
                                                      final String authenticationToken){
        return getBodyAuthorisationKeyRequest(createAccountAuthorisationModel, authenticationToken)
                .when()
                .post("/open-banking/1.0/accounts/auth");
    }

    public static Response getAccounts(final String consentId,
                                       final String authenticationToken){
        return getConsentAuthorisationKeyRequest(consentId, authenticationToken)
                .when()
                .get("/open-banking/1.0/accounts");
    }

    public static Response getAccount(final String accountId,
                                      final String authenticationToken){
        return getAuthorisationKeyRequest(authenticationToken)
                .pathParam("id", accountId)
                .when()
                .get("/open-banking/1.0/accounts/{id}");
    }

    public static Response deleteAccount(final String accountId,
                                         final String authenticationToken){
        return getAuthorisationKeyRequest(authenticationToken)
                .pathParam("id", accountId)
                .when()
                .delete("/open-banking/1.0/accounts/{id}");
    }
}
