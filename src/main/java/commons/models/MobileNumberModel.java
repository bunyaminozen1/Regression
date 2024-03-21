package commons.models;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;
import java.util.Random;

public class MobileNumberModel {
    private String countryCode;
    private String number;

    public MobileNumberModel(final String countryCode, final String number) {
        this.countryCode = countryCode;
        this.number = number;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public MobileNumberModel setCountryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public String getNumber() {
        return number;
    }

    public MobileNumberModel setNumber(String number) {
        this.number = number;
        return this;
    }

    public static MobileNumberModel random() {
        final List<String> mobileList = List.of("99", "79", "77", "21");
        final int i = new Random().nextInt(mobileList.size());
        return new MobileNumberModel("+356", String.format("%s%s", mobileList.get(i), RandomStringUtils.randomNumeric(6)));
    }

    public static MobileNumberModel randomGerman() {
        return new MobileNumberModel("+49", String.format("30%s", RandomStringUtils.randomNumeric(6)));
    }

    public static MobileNumberModel randomUK() {
        return new MobileNumberModel("+44", String.format("1212%s", RandomStringUtils.randomNumeric(6)));
    }
}
