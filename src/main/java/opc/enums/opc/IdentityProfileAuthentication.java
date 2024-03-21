package opc.enums.opc;

import lombok.Getter;

@Getter
public enum IdentityProfileAuthentication {

    PASSWORD("PASSWORD", "passwords"),
    SMS("SMS_OTP", "weavr_authfactors"),
    AUTHY("AUTHY_PUSH", "authy"),
    OKAY("OKAY_PUSH", "okay");

    private final String type;
    private final String providerKey;

    IdentityProfileAuthentication(final String type, final String providerKey){

        this.type = type;
        this.providerKey = providerKey;
    }
}
