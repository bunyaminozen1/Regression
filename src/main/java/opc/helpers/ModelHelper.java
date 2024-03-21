package opc.helpers;

import com.github.javafaker.Address;
import com.github.javafaker.Faker;
import commons.enums.Currency;
import commons.models.PersonalDetailsModel;
import opc.enums.opc.CountryCode;
import opc.enums.opc.TestMerchant;
import opc.models.shared.AddressModel;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static commons.enums.Currency.EUR;
import static commons.enums.Currency.GBP;
import static commons.enums.Currency.USD;

public class ModelHelper {

    public static TestMerchant merchantDetails(final String currency){

        final Map<Currency, TestMerchant> testMerchantMap =
                Map.of(EUR, TestMerchant.EUR,
                        GBP, TestMerchant.GBP,
                        USD, TestMerchant.USD);

        return testMerchantMap.get(Currency.valueOf(currency));
    }

    public static Pair<String,String> generateRandomValidSEPABankDetails() {
        return Pair.of(generateRandomValidIban(), generateRandomValidBankIdentifierNumber());
    }

    public static Pair<String,String> generateRandomValidFasterPaymentsBankDetails() {
        return Pair.of(generateRandomValidAccountNumber(), generateRandomValidSortCode());
    }

    public static String generateRandomValidIban() {
        final String ALLOWED_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        final SecureRandom secureRandom = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        // Add the first four characters that match the regex
        sb.append((char) (secureRandom.nextInt(26) + 'A'));
        sb.append((char) (secureRandom.nextInt(26) + 'A'));
        sb.append((char) (secureRandom.nextInt(10) + '0'));
        sb.append((char) (secureRandom.nextInt(10) + '0'));

        // Add the remaining characters (11 to 30 characters in length)
        int length = secureRandom.nextInt(20) + 11; // generate a random length between 11 and 30
        IntStream.range(0, length)
            .map(i -> ALLOWED_CHARACTERS.charAt(secureRandom.nextInt(ALLOWED_CHARACTERS.length())))
            .forEach(sb::append);

        String generatedString = sb.toString();
        if (generatedString.length() > 34) {
            generatedString = generatedString.substring(0, 34); // truncate the string to 34 characters if it is longer
        }

        // repeat the process until a valid IBAN is generated
        while (!generatedString.matches("^[a-zA-Z]{2}[0-9]{2}[a-zA-Z0-9]{11,30}$")) {
            sb = new StringBuilder();
            sb.append((char) (secureRandom.nextInt(26) + 'A'));
            sb.append((char) (secureRandom.nextInt(26) + 'A'));
            sb.append((char) (secureRandom.nextInt(10) + '0'));
            sb.append((char) (secureRandom.nextInt(10) + '0'));

            length = secureRandom.nextInt(20) + 11;
            IntStream.range(0, length)
                .map(i -> ALLOWED_CHARACTERS.charAt(secureRandom.nextInt(ALLOWED_CHARACTERS.length())))
                .forEach(sb::append);

            generatedString = sb.toString();
            if (generatedString.length() > 34) {
                generatedString = generatedString.substring(0, 34);
            }
        }

        return generatedString;
    }

    public static String generateRandomValidBankIdentifierNumber() {
        final String ALLOWED_BANK_IDENTIFIER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        final int MIN_LENGTH = 8;
        final int MAX_LENGTH = 11;
        Random random = new Random();

        int length = random.nextInt(MAX_LENGTH - MIN_LENGTH + 1) + MIN_LENGTH;
        String sb = IntStream.range(0, length)
            .map(i -> random.nextInt(ALLOWED_BANK_IDENTIFIER_CHARS.length()))
            .mapToObj(index -> String.valueOf(ALLOWED_BANK_IDENTIFIER_CHARS.charAt(index)))
            .collect(Collectors.joining());

        String result = sb;

        return result.matches("^[a-zA-Z0-9]{4}[a-zA-Z]{2}[a-zA-Z0-9]{2}[a-zA-Z0-9]{0,3}$") ? result
            : generateRandomValidBankIdentifierNumber();
    }

    public static String generateRandomValidAccountNumber() {
        final String DIGITS = "0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        }

        return sb.toString();
    }

    public static String generateRandomValidSortCode() {
        final String DIGITS = "0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        }

        return sb.toString();
    }

    public static List<Pair<String,String>> createMultipleValidSEPABankDetails(final int noOfSEPABankBeneficiaries) {
        final List<Pair<String,String>> SEPABankBeneficiaries = new ArrayList<>();
        IntStream.range(0, noOfSEPABankBeneficiaries).forEach(x -> SEPABankBeneficiaries.add(ModelHelper.generateRandomValidSEPABankDetails()));
        return SEPABankBeneficiaries;
    }

    public static List<Pair<String,String>> createMultipleValidFasterPaymentsBankDetails(final int noOfFasterPaymentsBankBeneficiaries) {
        final List<Pair<String,String>> FasterPaymentsBankBeneficiaries = new ArrayList<>();
        IntStream.range(0, noOfFasterPaymentsBankBeneficiaries).forEach(x -> FasterPaymentsBankBeneficiaries.add(ModelHelper.generateRandomValidFasterPaymentsBankDetails()));
        return FasterPaymentsBankBeneficiaries;
    }

    public static PersonalDetailsModel getPersonalDetails() {

        final Faker faker = new Faker();
        final String name = faker.name().firstName();
        final String surname = faker.name().lastName();
        final Address address = faker.address();
        return PersonalDetailsModel.builder()
                .name(name)
                .surname(surname)
                .email(String.format("%s%s%s@weavrtesting.io", name, surname, RandomStringUtils.randomNumeric(5)))
                .address(AddressModel.builder()
                        .setAddressLine1(address.streetAddress())
                        .setAddressLine2(address.secondaryAddress())
                        .setCity(address.city())
                        .setPostCode(address.zipCode())
                        .setCountry(CountryCode.getRandomEeaCountry())
                        .setState(null)
                        .build())
                .build();
    }
}