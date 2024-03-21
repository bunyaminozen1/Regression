package opc.services.openbanking;

import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import opc.enums.opc.UrlType;

import static io.restassured.RestAssured.given;

public class OpenBankingService {

    public static Response getCryptoVariables() {

        RestAssured.baseURI = "https://raw.githubusercontent.com/kjur/jsrsasign/10.4.0/jsrsasign-all-min.js";
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.urlEncodingEnabled = false;

        return given()
                .config(RestAssuredConfig.config()
                        .encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
                .get();
    }

}
