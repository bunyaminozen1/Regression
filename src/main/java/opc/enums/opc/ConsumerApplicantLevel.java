package opc.enums.opc;

import commons.config.ConfigHelper;

public enum ConsumerApplicantLevel {

    CONSUMER("%s-consumer"),
    CONSUMER_LEVEL_1("%s-consumer-level-1"),
    CONSUMER_NO_POA("%s-consumer-no-POA"),
    CONSUMER_1_POA("%s-consumer-1POA-residency");

    private final String levelName;

    ConsumerApplicantLevel(final String levelName) {
        this.levelName =
                String.format(levelName,
                        ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment());
    }

    public String getLevelName() {
        return levelName;
    }
}
