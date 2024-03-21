package opc.services.backoffice;

import io.restassured.response.Response;
import opc.models.backoffice.TransferModel;
import commons.services.BaseService;

public class BackofficeService extends BaseService {

    public static Response transfer(final TransferModel transferModel,
                                    final String secretKey){
        return getBodyRequest(transferModel)
                .header("Content-type", "application/json")
                .header("programme-key", secretKey)
                .and()
                .body(transferModel)
                .when()
                .post("/backoffice/api/bo/transfers/_/execute");
    }

    public static Response removeCard(final String managedCardId,
                                      final String secretKey){
        return getRequest()
                .header("Content-type", "application/json")
                .header("programme-key", secretKey)
                .pathParam("managedCardId", managedCardId)
                .when()
                .post("/backoffice/api/bo/managed_cards/{managedCardId}/remove");
    }

    public static Response getManagedAccountStatement(final String managedAccountId,
                                                      final String secretKey){
        return getRequest()
                .header("Content-type", "application/json")
                .header("programme-key", secretKey)
                .pathParam("managedAccountId", managedAccountId)
                .when()
                .post("/backoffice/api/bo/managed_accounts/{managedAccountId}/statement/get");
    }

    public static Response getManagedCardStatement(final String managedCardId,
                                                   final String secretKey){
        return getRequest()
                .header("Content-type", "application/json")
                .header("programme-key", secretKey)
                .pathParam("managedCardId", managedCardId)
                .when()
                .post("/backoffice/api/bo/managed_cards/{managedCardId}/statement/get");
    }
}
