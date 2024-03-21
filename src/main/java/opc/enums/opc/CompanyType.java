package opc.enums.opc;

import commons.config.ConfigHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public enum CompanyType {
    SOLE_TRADER("%s-corporate-sole-trader"),
    LLC("%s-corporate"),
    PUBLIC_LIMITED_COMPANY("%s-corporate-plc"),
    LIMITED_LIABILITY_PARTNERSHIP("%s-corporate-partnership"),

    //Stopped onboarding for the time being, but this enum wasn't removed
    // as we might need to add this again in the near future
    NON_PROFIT_ORGANISATION("%s-charity");

    private final String levelName;

    CompanyType(final String levelName) {
        this.levelName = String.format(levelName, ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment());
    }

    public static CompanyType getRandomCompanyType() {
        Random random = new Random();
        return values()[random.nextInt(values().length)];
    }

    public static CompanyType getRandomWithExcludedCompanyType(final CompanyType companyType) {

        final List<CompanyType> enums =
                Arrays.stream(values()).filter(x -> !x.equals(companyType)).collect(Collectors.toList());
        final Random random = new Random();
        return enums.get(random.nextInt(enums.size()));
    }

    public String getLevelName() {
        return levelName;
    }

    public static String getLevelName(final String companyType) {
        return valueOf(companyType).getLevelName();
    }
}