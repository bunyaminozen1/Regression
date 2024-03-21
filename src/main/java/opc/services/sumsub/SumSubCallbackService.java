package opc.services.sumsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import opc.models.sumsub.SumSubCallbackModel;
import commons.services.BaseService;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class SumSubCallbackService extends BaseService {

    private final static String SECRET_KEY = "59uw6d8wrw";

    public static Response sumsubCallback(final SumSubCallbackModel sumSubCallbackModel){

        return getRequest()
                .header("x-payload-digest", createSignature(sumSubCallbackModel))
                .body(sumSubCallbackModel)
                .when()
                .post("/sumsub/callback");
    }


    private static String createSignature(final Object body)  {
        try {
            final Mac hmacSHA1 = Mac.getInstance("HmacSHA1");
            hmacSHA1.init(new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.US_ASCII), "HmacSHA1"));
            byte[] bytes = body == null ? hmacSHA1.doFinal() : hmacSHA1.doFinal(new ObjectMapper().writeValueAsBytes(body));
            return Hex.encodeHexString(bytes);
        } catch (Exception e){
            throw new IllegalStateException(e.getMessage());
        }
    }
}
