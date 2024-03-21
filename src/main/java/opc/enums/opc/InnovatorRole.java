package opc.enums.opc;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public enum InnovatorRole {
    INNOVATOR_OPERATOR("INNOVATOR_OPERATOR"),
    INNOVATOR_FINANCE("INNOVATOR_FINANCE"),
    INNOVATOR_DEVELOPER("INNOVATOR_DEVELOPER"),
    INNOVATOR_OWNER("INNOVATOR_OWNER");

    private final String innovatorRole;

    InnovatorRole(String innovatorRole) {
        this.innovatorRole = innovatorRole;
    }

    public String getInnovatorRole() {
        return innovatorRole;
    }

    public static InnovatorRole getRandomInnovatorRole() {
        final Random random = new Random();
        return values()[random.nextInt(values().length)];
    }

    public static InnovatorRole getRandomWithExcludedInnovatorRole(final InnovatorRole innovatorRole) {
        final List<InnovatorRole> enums =
                Arrays.stream(values()).filter(x -> !x.equals(innovatorRole)).collect(Collectors.toList());
        final Random random = new Random();
        return enums.get(random.nextInt(enums.size()));
    }
}
