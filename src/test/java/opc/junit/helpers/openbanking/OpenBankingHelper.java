package opc.junit.helpers.openbanking;

import com.fasterxml.jackson.databind.ObjectMapper;
import opc.enums.openbanking.SignatureHeader;
import opc.enums.openbanking.TppSignatureComponent;
import opc.helpers.OpenBankingKeys;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class OpenBankingHelper {

    public static String getSignature(final String signatureMessage) throws Exception {

        final String clientPrivateKey = OpenBankingKeys.CLIENT_PRIVATE_KEY;

        final StringBuilder publicKeyLines = new StringBuilder();
        final BufferedReader bufferedReader = new BufferedReader(new StringReader(clientPrivateKey));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            publicKeyLines.append(line);
        }

        String publicKeyPem = publicKeyLines.toString();
        publicKeyPem = publicKeyPem.replace("-----BEGIN PRIVATE KEY-----", "");
        publicKeyPem = publicKeyPem.replace("-----END PRIVATE KEY-----", "");
        publicKeyPem = publicKeyPem.replaceAll("\\s+","");

        final byte [] publicKeyEncodedBytes = Base64.getDecoder().decode(publicKeyPem);

        final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(publicKeyEncodedBytes);
        final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        final PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        final Signature signature = Signature.getInstance("SHA256WithRSA");
        signature.initSign(privateKey);
        signature.update(signatureMessage.getBytes());

        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private static String getDigest() {

        return "SHA-256=47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=";
    }

    public static String generateBodyDigest(final Object body) throws Exception {

        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return String.format("SHA-256=%s", Base64.getEncoder().encodeToString(digest.digest(
                new ObjectMapper().writeValueAsString(body).getBytes(StandardCharsets.UTF_8))));
    }

    public static String getDate(final Optional<Integer> seconds){

        final String datePattern = "EEE, dd MMM yyyy HH:mm:ss z";

        DateFormat formatter = new SimpleDateFormat(datePattern);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));

        final Date date = seconds.map(integer -> Date.from(Instant.now().plus(integer, ChronoUnit.SECONDS))).orElseGet(Date::new);

        return formatter.format(date);
    }

    private static String getSignatureHeaderValues(final Map.Entry<SignatureHeader, Optional<String>> signatureHeader){

        switch (signatureHeader.getKey()) {
            case DATE: return signatureHeader.getValue().orElse(getDate(Optional.empty()));
            case DIGEST: return signatureHeader.getValue().orElse(getDigest());
            case TPP_CONSENT_ID: return signatureHeader.getValue().orElseThrow();
            default: throw new IllegalStateException("Unknown header");
        }
    }

    public static Map<String, String> generateHeaders(final String clientKeyId) throws Exception {

        final StringBuilder signatureMessage = new StringBuilder();

        final Map<String, String> headers = new HashMap<>();
        headers.put("date", getDate(Optional.empty()));
        headers.put("digest", getDigest());

        headers.forEach((key, value) -> signatureMessage.append(String.format("%s: %s\n", key, value)));

        final String signature = getSignature(signatureMessage.toString().trim());

        final String tppSignature =
                String.format("keyId=\"%s\",algorithm=\"rsa-sha256\",headers=\"date digest\",signature=\"%s\"",
                        clientKeyId, signature);

        headers.put("TPP-Signature", tppSignature);

        return headers;
    }

    public static Map<String, String> generateHeaders(final String clientKeyId,
                                                      final Map<SignatureHeader, Optional<String>> headerMap) throws Exception {

        final StringBuilder signatureMessage = new StringBuilder();
        final StringBuilder signatureHeaders = new StringBuilder();

        final Map<String, String> headers = new HashMap<>();
        headerMap.entrySet().forEach(header -> headers.put(header.getKey().getName(),
                getSignatureHeaderValues(header)));

        headers.forEach((key, value) -> {
            signatureMessage.append(String.format("%s: %s\n", key, value));
            signatureHeaders.append(String.format("%s ", key));
        });

        final String signature = getSignature(signatureMessage.toString().trim());

        final String tppSignature =
                String.format("keyId=\"%s\",algorithm=\"rsa-sha256\",headers=\"%s\",signature=\"%s\"",
                        clientKeyId, signatureHeaders.toString().trim(), signature);

        headers.put("TPP-Signature", tppSignature);

        return headers;
    }

    public static void replaceHeaders(final Map<String,String> headers, ArrayList<SignatureHeader> signatureHeaders, final String replacement) {
        signatureHeaders.forEach(signatureHeader -> headers.replace(signatureHeader.getName(), replacement));
    }

    public static String generateRandomUUID() {
        return UUID.randomUUID().toString();
    }

    public static String generateInvalidSignatureComponent(Map<String, String> headers, TppSignatureComponent signatureComponent, String replacement) {
        final String signature = headers.get(SignatureHeader.TPP_SIGNATURE.getName());
        return replaceSignatureComponent(signature, signatureComponent, replacement);
    }

    private static String replaceSignatureComponent(String signature, TppSignatureComponent signatureComponent, String replacement) {
        switch (signatureComponent) {
            case KEY_ID:
                final String keyIdToReplace = StringUtils.substringBetween(signature, "keyId=", ",");
                signature = signature.replace(keyIdToReplace, String.format("\"%s\"",replacement));
                return signature;
            case ALGORITHM:
                final String algorithmToReplace = StringUtils.substringBetween(signature, "algorithm=", ",");
                signature = signature.replace(algorithmToReplace, String.format("\"%s\"",replacement));
                return signature;
            case HEADERS:
                final String headersToReplace = StringUtils.substringBetween(signature, "headers=", ",");
                signature = signature.replace(headersToReplace, String.format("\"%s\"",replacement));
                return signature;
            case SIGNATURE:
                final String signatureToReplace = StringUtils.substringAfterLast(signature, "signature=");
                signature = signature.replace(signatureToReplace, String.format("\"%s\"",replacement));
                return signature;
            default:
                return signature;
        }
    }
}

