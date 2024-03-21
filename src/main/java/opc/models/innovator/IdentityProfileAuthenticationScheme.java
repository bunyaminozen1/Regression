package opc.models.innovator;

import java.util.List;
import java.util.Map;

public class IdentityProfileAuthenticationScheme {

    private final Map<Integer, List<Factor>> factorLevels;

    public IdentityProfileAuthenticationScheme(final Map<Integer, List<Factor>> factorLevels) {
        this.factorLevels = factorLevels;
    }

    public static class Factor {

        private final String type;
        private final String providerKey;

        public Factor(final String type, final String providerKey) {
            this.type = type;
            this.providerKey = providerKey;
        }

        public String getType() {
            return type;
        }

        public String getProviderKey() {
            return providerKey;
        }
    }

    public static IdentityProfileAuthenticationScheme DefaultAccountInfoIdentityProfileAuthenticationScheme() {
        return new IdentityProfileAuthenticationScheme(Map.of(1,  List.of(new Factor("PASSWORD", "passwords"))));
    }

    public static IdentityProfileAuthenticationScheme DefaultPaymentInitIdentityProfileAuthenticationScheme() {
        return new IdentityProfileAuthenticationScheme(Map.of(1,  List.of(new Factor("SMS_OTP", "weavr-authfactors"))));
    }
}
