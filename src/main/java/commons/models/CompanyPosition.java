package commons.models;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public enum CompanyPosition {

    DIRECTOR,
    AUTHORISED_REPRESENTATIVE,

    // For testing purposes only
    UNKNOWN;

    public static CompanyPosition getRandomCompanyPosition() {

        final List<CompanyPosition> enums =
                Arrays.stream(values()).filter(x -> !x.equals(UNKNOWN)).collect(Collectors.toList());
        final Random random = new Random();
        return enums.get(random.nextInt(enums.size()));
    }
}
