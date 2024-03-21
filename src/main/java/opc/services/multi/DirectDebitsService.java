package opc.services.multi;

import io.restassured.response.Response;
import opc.models.multi.directdebit.RejectCollectionModel;
import commons.services.BaseService;

import java.util.Map;
import java.util.Optional;

public class DirectDebitsService extends BaseService {

    public static Response getDirectDebitMandates(final String secretKey,
                                                  final Optional<Map<String, Object>> filters,
                                                  final String token) {
        return assignQueryParams(getApiKeyAuthenticationRequest(secretKey, token, Optional.empty()), filters)
                .when()
                .get("/multi/outgoing_direct_debits/mandates");
    }

    public static Response getDirectDebitMandate(final String secretKey,
                                                 final String mandateId,
                                                 final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", mandateId)
                .when()
                .get("/multi/outgoing_direct_debits/mandates/{id}");
    }

    public static Response cancelDirectDebitMandate(final String secretKey,
                                                    final String mandateId,
                                                    final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", mandateId)
                .when()
                .post("/multi/outgoing_direct_debits/mandates/{id}/cancel");
    }

    public static Response getDirectDebitMandateCollections(final String secretKey,
                                                            final String mandateId,
                                                            final Optional<Map<String, Object>> filters,
                                                            final String token) {
        return assignQueryParams(getApiKeyAuthenticationRequest(secretKey, token, Optional.empty()), filters)
                .pathParam("id", mandateId)
                .when()
                .get("/multi/outgoing_direct_debits/mandates/{id}/collections");
    }

    public static Response getDirectDebitMandateCollection(final String secretKey,
                                                           final String mandateId,
                                                           final String collectionId,
                                                           final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", mandateId)
                .pathParam("collectionId", collectionId)
                .when()
                .get("/multi/outgoing_direct_debits/mandates/{id}/collections/{collectionId}");
    }

    public static Response rejectDirectDebitMandateCollection(final RejectCollectionModel rejectCollectionModel,
                                                              final String secretKey,
                                                              final String mandateId,
                                                              final String collectionId,
                                                              final String token){
        return getBodyApiKeyAuthenticationRequest(rejectCollectionModel, secretKey, token, Optional.empty())
                .pathParam("id", mandateId)
                .pathParam("collectionId", collectionId)
                .when()
                .post("/multi/outgoing_direct_debits/mandates/{id}/collections/{collectionId}/reject");
    }
}
