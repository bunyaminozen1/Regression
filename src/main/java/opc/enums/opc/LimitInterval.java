package opc.enums.opc;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public enum LimitInterval {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY,
    ALWAYS;

    public static LimitInterval random() {
        final List<LimitInterval> enums =
                Arrays.stream(values()).collect(Collectors.toList());
        final Random random = new Random();
        return enums.get(random.nextInt(enums.size()));
    }
}
