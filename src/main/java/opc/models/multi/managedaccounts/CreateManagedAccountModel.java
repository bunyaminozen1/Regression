package opc.models.multi.managedaccounts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import commons.enums.Currency;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;

public class CreateManagedAccountModel {
    private final String profileId;
    private final String friendlyName;
    private final String currency;
    private final String tag;

    public CreateManagedAccountModel(final Builder builder) {
        this.profileId = builder.profileId;
        this.friendlyName = builder.friendlyName;
        this.currency = builder.currency;
        this.tag = builder.tag;
    }

    public String getProfileId() {
        return profileId;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public String getCurrency() {
        return currency;
    }

    public String getTag() {
        return tag;
    }

    public static class Builder {
        private String profileId;
        private String friendlyName;
        private String currency;
        private String tag;

        public Builder setProfileId(String profileId) {
            this.profileId = profileId;
            return this;
        }

        public Builder setFriendlyName(String friendlyName) {
            this.friendlyName = friendlyName;
            return this;
        }

        public Builder setCurrency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder setTag(String tag) {
            this.tag = tag;
            return this;
        }

        public CreateManagedAccountModel build(){ return new CreateManagedAccountModel(this); }
    }

    public static Builder DefaultCreateManagedAccountModel(final String managedAccountProfileId, final String currency) {
        return new Builder()
                .setProfileId(managedAccountProfileId)
                .setFriendlyName(RandomStringUtils.randomAlphabetic(5))
                .setCurrency(currency)
                .setTag(RandomStringUtils.randomAlphabetic(5));
    }

    public static Builder DefaultCreateManagedAccountModel(final String managedAccountProfileId, final Currency currency) {
        return new Builder()
                .setProfileId(managedAccountProfileId)
                .setFriendlyName(RandomStringUtils.randomAlphabetic(5))
                .setCurrency(currency.name())
                .setTag(RandomStringUtils.randomAlphabetic(5));
    }

    @SneakyThrows
    public static String createManagedAccountStringModel(final String managedCardProfileId, final String currency) {
        return new ObjectMapper()
                .writeValueAsString(DefaultCreateManagedAccountModel(managedCardProfileId, currency).build());
    }

    public static Builder dataCreateManagedAccountModel(final String managedAccountProfileId,
                                                        final Currency currency) {

        final Faker faker = new Faker();

        return new Builder()
                .setProfileId(managedAccountProfileId)
                .setFriendlyName(faker.cat().name())
                .setCurrency(currency.name())
                .setTag(faker.dog().name());
    }
}