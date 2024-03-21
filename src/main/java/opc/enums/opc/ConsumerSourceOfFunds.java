package opc.enums.opc;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public enum ConsumerSourceOfFunds {
    PERSONAL_SAVINGS,
    FAMILY_SAVINGS,
    LABOUR_CONTRACT,
    CIVIL_CONTRACT,
    RENT,
    FUNDS_FROM_OTHER_AUXILIARY_SOURCES,
    SALE_OF_MOVABLE_ASSETS,
    SALE_OF_REAL_ESTATE,
    ORDINARY_BUSINESS_ACTIVITY,
    DIVIDENDS,
    LOAN_FROM_FINANCIAL_INSTITUTIONS_CREDIT_UNIONS,
    LOAN_FROM_THIRD_PARTIES,
    INHERITANCE,
    SALE_OF_COMPANY_SHARES_BUSINESS,
    OTHER,

    // Used for test purposes
    UNKNOWN;

    public static ConsumerSourceOfFunds getRandomSourceOfFunds() {
        final List<ConsumerSourceOfFunds> enums =
                Arrays.stream(values()).filter(x -> !x.equals(UNKNOWN)).collect(Collectors.toList());
        final Random random = new Random();
        return enums.get(random.nextInt(enums.size()));
    }
}
