package fpi.paymentrun.services.admin;

import fpi.paymentrun.models.admin.ApplyProfilesModel;
import fpi.paymentrun.services.BaseService;
import io.restassured.response.Response;

public class AdminService extends BaseService {

    public static Response exportProfiles(final String programmeId,
                                          final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .when()
                .post(String.format("%s/admin_portal/v1/programmes/{programme_id}/export", getPaymentRunEnvironmentPrefix()));
    }

    public static Response applyProfiles(final ApplyProfilesModel applyProfilesModel,
                                         final String programmeId,
                                         final String token) {
        return getBodyAuthenticatedRequest(applyProfilesModel, token)
                .pathParam("programme_id", programmeId)
                .when()
                .post(String.format("%s/admin_portal/v1/programmes/{programme_id}/apply", getPaymentRunEnvironmentPrefix()));
    }

    public static Response getOwtData(final String owtId,
                                      final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("owt_id", owtId)
                .when()
                .get(String.format("%s/admin_portal/v1/owt/{owt_id}", getPaymentRunEnvironmentPrefix()));
    }

    public static Response getCorporateData(final String corporateId,
                                            final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("corporate_id", corporateId)
                .when()
                .get(String.format("%s/admin_portal/v1/corporates/{corporate_id}", getPaymentRunEnvironmentPrefix()));
    }
}
