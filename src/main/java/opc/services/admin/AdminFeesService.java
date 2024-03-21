package opc.services.admin;

import io.restassured.response.Response;
import opc.models.admin.ChargeFeeModel;
import opc.models.admin.GetFeesModel;
import opc.models.admin.GetReverseFeesModel;
import opc.models.admin.ReverseFeeModel;
import commons.services.BaseService;

public class AdminFeesService extends BaseService {

    public static Response chargeFee(final ChargeFeeModel chargeFeeModel,
                                     final String token){

        return getBodyAuthenticatedRequest(chargeFeeModel, token)
                .when()
                .post("/admin/api/fees/charge_fees/_/execute");
    }

    public static Response getChargeFees(final GetFeesModel getFeesModel,
                                         final String token){

        return getBodyAuthenticatedRequest(getFeesModel, token)
                .when()
                .post("/admin/api/fees/charge_fees/get");
    }

    public static Response getChargeFee(final String transactionId,
                                        final String token){

        return getAuthenticatedRequest(token)
                .pathParam("id", transactionId)
                .when()
                .post("/admin/api/fees/charge_fees/{id}/get");
    }

    public static Response reverseFee(final ReverseFeeModel chargeFeeModel,
                                      final String token){

        return getBodyAuthenticatedRequest(chargeFeeModel, token)
                .when()
                .post("/admin/api/fees/reverse_fees/_/execute");
    }

    public static Response getReverseFees(final GetReverseFeesModel getFeesModel,
                                          final String token){

        return getBodyAuthenticatedRequest(getFeesModel, token)
                .when()
                .post("/admin/api/fees/reverse_fees/get");
    }

    public static Response getReverseFee(final String transactionId,
                                         final String token){

        return getAuthenticatedRequest(token)
                .pathParam("id", transactionId)
                .when()
                .post("/admin/api/fees/reverse_fees/{id}/get");
    }
}
