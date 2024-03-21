package spi.openbanking.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import spi.openbanking.models.OpenBankingAccountResponseModel;
import spi.openbanking.models.OpenBankingInstitutionResponseModel;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OpenBankingHelper {

    public static String API_KEY = "8ju1biPHjbIhEa9yMX8dkRq3zW2Va1t9KDuS6";
    public static String CONSENT_ID = "64ef3a761fe8021e9e7568c6";

    public static List<OpenBankingAccountResponseModel> getAccountsByConsentId(final String consentId) {

        return getOpenBankingAccounts()
                .stream()
                .filter(x -> x.getConsentId().equals(consentId)).collect(Collectors.toList());
    }

    @SneakyThrows
    private static List<OpenBankingAccountResponseModel> getOpenBankingAccounts() {

        return Arrays.asList(new ObjectMapper()
                .readValue(new File("./src/test/resources/OpenBankingConfiguration/open_banking_accounts.json"),
                        OpenBankingAccountResponseModel[].class));
    }

    @SneakyThrows
    public static List<OpenBankingInstitutionResponseModel> getInstitutions() {

        return Arrays.asList(new ObjectMapper()
                .readValue(new File("./src/test/resources/OpenBankingConfiguration/open_banking_institutions.json"),
                        OpenBankingInstitutionResponseModel[].class));
    }
}
