package opc.services.multi;

import io.restassured.response.Response;
import commons.services.BaseService;

import java.util.Map;
import java.util.Optional;

public class SemiService extends BaseService {
    public static Response getLinkedIdentities(final String secretKey,
                                               final Optional<Map<String, Object>> filters,
                                               final String token) {

        return assignQueryParams(getApiKeyAuthenticationRequest(secretKey, token, Optional.empty()), filters)
                .when()
                .get("/multi/identities");
    }


}
