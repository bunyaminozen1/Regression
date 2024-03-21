package opc.enums.opc;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public enum CountryCode {

    // TODO enums per jurisdiction

    // Jurisdiction EEA
    BE,
    ES,
    HU,
    SK,
    BG,
    FR,
    MT,
    FI,
    CZ,
    HR,
    NL,
    SE,
    DK,
    IT,
    AT,
    DE,
    CY,
    PL,
    IS,
    EE,
    LV,
    PT,
    LI,
    IE,
    LT,
    RO,
    NO,
    GR,
    LU,
    SI,

    // Jurisdiction UK
    GB,
    IM,
    JE,
    GG,

    // Unsupported
    AF;

    public static CountryCode getRandomEeaCountry() {

        final List<CountryCode> exclusions = List.of(AF, GB, IM, JE, GG);

        final List<CountryCode> enums =
                Arrays.stream(values())
                        .filter(x -> exclusions.stream().noneMatch(y -> y.equals(x)))
                        .collect(Collectors.toList());

        final Random random = new Random();
        return enums.get(random.nextInt(enums.size()));
    }

    public static CountryCode getRandomWithExcludedCountry(final CountryCode countryCode) {

        final List<CountryCode> enums =
                Arrays.stream(values()).filter(x -> !x.equals(countryCode)).collect(Collectors.toList());
        final Random random = new Random();
        return enums.get(random.nextInt(enums.size()));
    }

    public static List<String> getAllEeaCountries(){
        final List<CountryCode> exclusions = List.of(AF, GB, IM, JE, GG);

        return Arrays.stream(CountryCode.values())
                        .filter(x -> exclusions.stream().noneMatch(y -> y.equals(x)))
                        .map(CountryCode::name)
                        .collect(Collectors.toList());
    }
    public static List<String> getAllUkCountries(){
        final List<CountryCode> ukCountries = List.of(GB, IM, JE, GG);

        return ukCountries.stream()
                .map(CountryCode::name).collect(Collectors.toList());
    }
}
