package opc.models.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class RegisteredCountriesValue {

    private List<String> part;

    public static List<RegisteredCountriesValue> defaultValueModel(String country1, String country2){

        return Arrays.asList(RegisteredCountriesValue.builder().part(Arrays.asList(country1)).build(),
            RegisteredCountriesValue.builder().part(Arrays.asList(country2)).build()
            );
    }

    public static List<RegisteredCountriesValue> defaultValueModel(final List<String> countries){

        final List<RegisteredCountriesValue> registeredCountriesValues = new ArrayList<>();

        countries.forEach(country ->
                registeredCountriesValues.add(RegisteredCountriesValue.builder().part(Collections.singletonList(country)).build()));

        return registeredCountriesValues;
    }
}



