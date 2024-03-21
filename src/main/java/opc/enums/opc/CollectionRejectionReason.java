package opc.enums.opc;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public enum CollectionRejectionReason {

    ADVANCE_NOTICE_DISPUTED,
    AMOUNT_DIFFERS,
    AMOUNT_NOT_YET_DUE,
    PRESENTATION_OVERDUE,
    SERVICE_USER_DIFFERS,
    REFER_TO_PAYER,

    // Used for test purposes
    UNKNOWN;

    public static CollectionRejectionReason random() {
        final List<CollectionRejectionReason> enums =
                Arrays.stream(values()).filter(x -> !x.equals(UNKNOWN)).collect(Collectors.toList());
        final Random random = new Random();
        return enums.get(random.nextInt(enums.size()));
    }
}
