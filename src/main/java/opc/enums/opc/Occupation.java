package opc.enums.opc;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public enum Occupation {

    ACCOUNTING,
    AUDIT,
    FINANCE,
    PUBLIC_SECTOR_ADMINISTRATION,
    ART_ENTERTAINMENT,
    AUTO_AVIATION,
    BANKING_LENDING,
    BUSINESS_CONSULTANCY_LEGAL,
    CONSTRUCTION_REPAIR,
    EDUCATION_PROFESSIONAL_SERVICES,
    INFORMATIONAL_TECHNOLOGIES,
    TOBACCO_ALCOHOL,
    GAMING_GAMBLING,
    MEDICAL_SERVICES,
    MANUFACTURING,
    PR_MARKETING,
    PRECIOUS_GOODS_JEWELRY,
    NON_GOVERNMENTAL_ORGANIZATION,
    INSURANCE_SECURITY,
    RETAIL_WHOLESALE,
    TRAVEL_TOURISM,
    FREELANCER,
    STUDENT,
    UNEMPLOYED,
    RETIRED,
    OTHER,

    // For testing purposes
    UNKNOWN;

    public static Occupation getRandomOccupation() {
        final List<Occupation> enums =
                Arrays.stream(values()).filter(x -> !x.equals(UNKNOWN)).collect(Collectors.toList());
        final Random random = new Random();
        return enums.get(random.nextInt(enums.size()));
    }
}
