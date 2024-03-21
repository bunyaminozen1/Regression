package opc.models.sumsub;

import org.apache.commons.lang3.RandomStringUtils;

public class ApplicantModel {

    private final String email;
    private final FixedInfoModel info;
    private final String lang;
    private final String type;

    public ApplicantModel(final Builder builder) {
        this.email = builder.email;
        this.info = builder.info;
        this.lang = builder.lang;
        this.type = builder.type;
    }

    public String getEmail() {
        return email;
    }

    public FixedInfoModel getInfo() {
        return info;
    }

    public String getLang() {
        return lang;
    }

    public String getType() {
        return type;
    }

    public static class Builder {
        public String lang;
        public String type;
        private String email;
        private FixedInfoModel info;

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setInfo(FixedInfoModel info) {
            this.info = info;
            return this;
        }

        public Builder setLang(String lang) {
            this.lang = lang;
            return this;
        }

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public ApplicantModel build() { return new ApplicantModel(this); }
    }

    public static Builder builder(){
        return new Builder();
    }

    public static Builder defaultApplicantModel(final FixedInfoModel fixedInfoModel){

        return new Builder()
                .setEmail(String.format("%s@sstest.test", RandomStringUtils.randomAlphabetic(6)))
                .setInfo(fixedInfoModel);
    }
}