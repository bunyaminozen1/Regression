package opc.enums.opc;

import org.apache.commons.lang3.RandomStringUtils;

public enum ManagedInstrumentType {
    MANAGED_ACCOUNTS("managed_accounts"),
    MANAGED_CARDS("managed_cards"),
    UNKNOWN(RandomStringUtils.randomAlphabetic(6));

    private final String value;

    ManagedInstrumentType(final String value){
        this.value = value;
    }

    public String getValue(){
        return value;
    }
}
