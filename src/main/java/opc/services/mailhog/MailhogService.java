package opc.services.mailhog;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import opc.enums.opc.UrlType;
import commons.services.BaseService;

public class MailhogService extends BaseService {

    public static Response getMailHogEmail(final String emailAddress) {
        return mailhogRestAssured()
                .pathParam("emailAddress", emailAddress)
                .when()
                .get("/api/v2/search?kind=to&query={emailAddress}");
    }

    public static Response getMailHogSms(final String mobileNumber) {
        return mailhogRestAssured()
                .pathParam("mobileNumber", mobileNumber)
                .when()
                .get("/api/v2/search?kind=containing&query={mobileNumber}");
    }

    protected static RequestSpecification mailhogRestAssured() {
        return restAssured(UrlType.MAILHOG);
    }
}
