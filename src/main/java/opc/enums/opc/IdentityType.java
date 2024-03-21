package opc.enums.opc;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public enum IdentityType {

    CORPORATE("corporates"),
    CONSUMER("consumers"),
    BUYER("buyer");

    private final String value;

    IdentityType(final String value){
        this.value = value;
    }

    public String getValue(){
        return value;
    }

    public static IdentityType getRandom() {
        final List<IdentityType> enums =
                Arrays.stream(values()).collect(Collectors.toList());
        final Random random = new Random();
        return enums.get(random.nextInt(enums.size()));
    }
}
