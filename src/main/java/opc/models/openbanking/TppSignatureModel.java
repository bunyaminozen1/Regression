package opc.models.openbanking;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;

@Builder
@Data
@Setter
public class TppSignatureModel {

    private final String keyId;
    private final String algorithm;
    private final String headers;
    @JsonProperty("signature")
    private final String signatureValue;

    public static TppSignatureModel getSignature(){
        return TppSignatureModel.builder()
                .keyId("108759344804331529")
                .algorithm("rsa-sha256")
                .headers("date digest")
                .signatureValue("CVSaSN6U1hTWaNCV4aFiLvtCM8YZXdmnfF5Al4U+sor+QL9Xxcw+T5CYS8GM5Lo7rpy9XB0pVAV2I6nxWLCbnWBjN1oNIE9I9Fk4TIWAUGGnjkPaW+sQXJg2WtqsGmuOI/VHqY95sr6QE+LQdUIfa9zvGKIYjAQ0ouciCcQgk+dTwMGFEyA/rPAfG8uvRr1R5KUpkKdUZ3oIFByWl2wtmJHCEgGupKxT+yOQk3soIl0df4FU3wxpJVhryY539mYaK8SSTMTtB9b9wc6rd3XcPNaQcKJR1awVfgrH4zaHcnjb/RVwHYrfLUZD8Z+XWwb+7K4UQLEKXIRICZbLgrYaQg==")
                .build();
    }
}