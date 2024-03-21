package opc.enums.opc;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public enum PluginStatus {
    ACTIVE,
    INACTIVE,
    BETA,
    UNKNOWN;

    public static PluginStatus getRandomWithExcludedPluginStatus(final PluginStatus pluginStatus) {
        final List<PluginStatus> enums =
                Arrays.stream(values()).filter(x -> !x.equals(pluginStatus)).collect(Collectors.toList());
        final Random random = new Random();
        return enums.get(random.nextInt(enums.size()));
    }
}
