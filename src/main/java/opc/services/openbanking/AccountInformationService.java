package opc.services.openbanking;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import opc.enums.opc.AcceptedResponse;
import commons.services.BaseService;

import java.util.Map;
import java.util.Optional;

public class AccountInformationService extends BaseService {

    public static RequestSpecification getOpenBankingRequest(final String sharedKey) {
        return getRequest()
                .header("programme-key", sharedKey);
    }

    public static Response createConsent(final String sharedKey,
                                         final Map<String, String> headers) {

        return getOpenBankingRequest(sharedKey)
                .header("Date", headers.get("date"))
                .header("Digest", headers.get("digest"))
                .header("TPP-Signature", headers.get("TPP-Signature"))
                .when()
                .post("/openbanking/account_information/consents");
    }

    public static Response getConsent(final String sharedKey,
                                      final Map<String, String> headers,
                                      final String consentId) {

        return getOpenBankingRequest(sharedKey)
                .header("Date", headers.get("date"))
                .header("Digest", headers.get("digest"))
                .header("TPP-Signature", headers.get("TPP-Signature"))
                .pathParam("consent_id",consentId)
                .when()
                .get("/openbanking/account_information/consents/{consent_id}");
    }

    public static Response getConsents(final String sharedKey,
                                       final Map<String, String> headers,
                                       final Optional<Map<String, Object>> filters) {

        return assignQueryParams(getOpenBankingRequest(sharedKey), filters)
                .header("Date", headers.get("date"))
                .header("Digest", headers.get("digest"))
                .header("TPP-Signature", headers.get("TPP-Signature"))
                .when()
                .get("/openbanking/account_information/consents");
    }

    public static Response getManagedAccounts(final String sharedKey,
                                              final Map<String, String> headers,
                                              final Optional<Map<String, Object>> filters) {

        return assignQueryParams(getOpenBankingRequest(sharedKey), filters)
                .header("Date", headers.get("date"))
                .header("Digest", headers.get("digest"))
                .header("TPP-Signature", headers.get("TPP-Signature"))
                .header("tpp-consent-id", headers.get("tpp-consent-id"))
                .when()
                .get("/openbanking/account_information/managed_accounts");
    }

    public static Response getManagedAccount(final String sharedKey,
                                             final Map<String, String> headers,
                                             final String managedAccountId) {

        return getOpenBankingRequest(sharedKey)
                .header("Date", headers.get("date"))
                .header("Digest", headers.get("digest"))
                .header("TPP-Signature", headers.get("TPP-Signature"))
                .header("tpp-consent-id", headers.get("tpp-consent-id"))
                .pathParam("managed_account_id", managedAccountId)
                .when()
                .get("/openbanking/account_information/managed_accounts/{managed_account_id}");
    }

    public static Response getManagedAccountIban(final String sharedKey,
                                                 final Map<String, String> headers,
                                                 final String managedAccountId) {

        return getOpenBankingRequest(sharedKey)
                .header("Date", headers.get("date"))
                .header("Digest", headers.get("digest"))
                .header("TPP-Signature", headers.get("TPP-Signature"))
                .header("tpp-consent-id", headers.get("tpp-consent-id"))
                .pathParam("managed_account_id", managedAccountId)
                .when()
                .get("/openbanking/account_information/managed_accounts/{managed_account_id}/iban");
    }

    public static Response getManagedAccountStatement(final String sharedKey,
                                                      final Map<String, String> headers,
                                                      final String managedAccountId,
                                                      final Optional<Map<String, Object>> filters,
                                                      final AcceptedResponse acceptedResponse) {

        return assignQueryParams(getOpenBankingRequest(sharedKey), filters)
                .header("Date", headers.get("date"))
                .header("Digest", headers.get("digest"))
                .header("TPP-Signature", headers.get("TPP-Signature"))
                .header("tpp-consent-id", headers.get("tpp-consent-id"))
                .header("accept", acceptedResponse.getAccept())
                .pathParam("managed_account_id", managedAccountId)
                .when()
                .get("/openbanking/account_information/managed_accounts/{managed_account_id}/statement");
    }

    public static Response getManagedAccountStatement(final String sharedKey,
                                                      final Map<String, String> headers,
                                                      final String managedAccountId,
                                                      final Optional<Map<String, Object>> filters,
                                                      final Optional<Map<String, Object>> acceptHeaders) {

        return assignHeaderParams(assignQueryParams(getOpenBankingRequest(sharedKey), filters), acceptHeaders)
                .header("Date", headers.get("date"))
                .header("Digest", headers.get("digest"))
                .header("TPP-Signature", headers.get("TPP-Signature"))
                .header("tpp-consent-id", headers.get("tpp-consent-id"))
                .pathParam("managed_account_id", managedAccountId)
                .when()
                .get("/openbanking/account_information/managed_accounts/{managed_account_id}/statement");
    }

    public static Response getManagedCards(final String sharedKey,
                                           final Map<String, String> headers,
                                           final Optional<Map<String, Object>> filters) {

        return assignQueryParams(getOpenBankingRequest(sharedKey), filters)
                .header("Date", headers.get("date"))
                .header("Digest", headers.get("digest"))
                .header("TPP-Signature", headers.get("TPP-Signature"))
                .header("tpp-consent-id", headers.get("tpp-consent-id"))
                .when()
                .get("/openbanking/account_information/managed_cards");
    }

    public static Response getManagedCard(final String sharedKey,
                                          final Map<String, String> headers,
                                          final String managedCardId) {

        return getOpenBankingRequest(sharedKey)
                .header("Date", headers.get("date"))
                .header("Digest", headers.get("digest"))
                .header("TPP-Signature", headers.get("TPP-Signature"))
                .header("tpp-consent-id", headers.get("tpp-consent-id"))
                .pathParam("managed_card_id", managedCardId)
                .when()
                .get("/openbanking/account_information/managed_cards/{managed_card_id}");
    }

    public static Response getManagedCardStatement(final String sharedKey,
                                                   final Map<String, String> headers,
                                                   final String managedCardId,
                                                   final Optional<Map<String, Object>> filters,
                                                   final AcceptedResponse acceptedResponse) {

        return assignQueryParams(getOpenBankingRequest(sharedKey), filters)
                .header("Date", headers.get("date"))
                .header("Digest", headers.get("digest"))
                .header("TPP-Signature", headers.get("TPP-Signature"))
                .header("tpp-consent-id", headers.get("tpp-consent-id"))
                .header("accept", acceptedResponse.getAccept())
                .pathParam("managed_card_id", managedCardId)
                .when()
                .get("/openbanking/account_information/managed_cards/{managed_card_id}/statement");
    }

    public static Response getManagedCardStatement(final String sharedKey,
                                                   final Map<String, String> headers,
                                                   final String managedCardId,
                                                   final Optional<Map<String, Object>> filters,
                                                   final Optional<Map<String, Object>> acceptHeaders) {

        return assignHeaderParams(assignQueryParams(getOpenBankingRequest(sharedKey), filters), acceptHeaders)
                .header("Date", headers.get("date"))
                .header("Digest", headers.get("digest"))
                .header("TPP-Signature", headers.get("TPP-Signature"))
                .header("tpp-consent-id", headers.get("tpp-consent-id"))
                .pathParam("managed_card_id", managedCardId)
                .when()
                .get("/openbanking/account_information/managed_cards/{managed_card_id}/statement");
    }

    public static Response revokeConsent(final String sharedKey,
        final Map<String, String> headers,
        final String consentId) {

        return getOpenBankingRequest(sharedKey)
            .header("Date", headers.get("date"))
            .header("Digest", headers.get("digest"))
            .header("TPP-Signature", headers.get("TPP-Signature"))
            .pathParam("consent_id", consentId)
            .when()
            .post("/openbanking/account_information/consents/{consent_id}/revoke");
    }

    protected static RequestSpecification assignQueryParams(final RequestSpecification request,
                                                            final Optional<Map<String, Object>> filters){
        filters.ifPresent(request::queryParams);

        return request;
    }
}
