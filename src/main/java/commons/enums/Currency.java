package commons.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Currency {
    EUR,
    GBP,
    USD;

    public static Currency getRandomCurrency() {
        final Random random = new Random();
        return values()[random.nextInt(values().length)];
    }

    public static Currency getRandomWithExcludedCurrency(final Currency currency) {

        final List<Currency> enums =
                Arrays.stream(values()).filter(x -> !x.equals(currency)).collect(Collectors.toList());
        final Random random = new Random();
        return enums.get(random.nextInt(enums.size()));
    }

    public static Currency getRandomWithExcludedCurrencies(final List<Currency> currencies) {

        final List<Currency> enums =
                Arrays.stream(values()).filter(x -> !currencies.contains(x)).collect(Collectors.toList());
        final Random random = new Random();
        return enums.get(random.nextInt(enums.size()));
    }

    public static List<String> getSupportedCurrencies() {
        return Stream.of(Currency.values()).map(Currency::name).collect(Collectors.toList());
    }
}
