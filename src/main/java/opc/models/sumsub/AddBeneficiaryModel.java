package opc.models.sumsub;

import com.github.javafaker.Faker;
import commons.models.DateOfBirthModel;
import fpi.paymentrun.models.CreateBuyerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Collections;
import java.util.List;

public class AddBeneficiaryModel {

    private final ApplicantModel applicant;
    private final List<String> positions;
    private final String type;
    private final boolean inRegistry;
    private final Integer shareSize;

    public AddBeneficiaryModel(final Builder builder) {
        this.applicant = builder.applicant;
        this.positions = builder.positions;
        this.type = builder.type;
        this.inRegistry = builder.inRegistry;
        this.shareSize = builder.shareSize;
    }

    public ApplicantModel getApplicant() {
        return applicant;
    }

    public List<String> getPositions() {
        return positions;
    }

    public String getType() {
        return type;
    }

    public boolean isInRegistry() {
        return inRegistry;
    }

    public Integer getShareSize() {
        return shareSize;
    }

    public static class Builder {
        public Integer shareSize;
        private ApplicantModel applicant;
        private List<String> positions;
        private String type;
        private boolean inRegistry;

        public Builder setApplicant(ApplicantModel applicant) {
            this.applicant = applicant;
            return this;
        }

        public Builder setPositions(List<String> positions) {
            this.positions = positions;
            return this;
        }

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setInRegistry(boolean inRegistry) {
            this.inRegistry = inRegistry;
            return this;
        }

        public Builder setShareSize(Integer shareSize) {
            this.shareSize = shareSize;
            return this;
        }

        public AddBeneficiaryModel build() { return new AddBeneficiaryModel(this); }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder defaultAddBeneficiaryModel(final String type) {

        final FixedInfoModel fixedInfoModel =
                FixedInfoModel.builder()
                        .setFirstName(RandomStringUtils.randomAlphabetic(5))
                        .setLastName(RandomStringUtils.randomAlphabetic(5))
                        .setDob("1990-01-01")
                        .setNationality("MLT")
                        .build();

        return new Builder()
                .setApplicant(ApplicantModel.defaultApplicantModel(fixedInfoModel).build())
                .setType(type);
    }

    public static Builder defaultAddBeneficiaryModel(final List<String> positions) {

        final FixedInfoModel fixedInfoModel =
                FixedInfoModel.builder()
                        .setFirstName(RandomStringUtils.randomAlphabetic(5))
                        .setLastName(RandomStringUtils.randomAlphabetic(5))
                        .setDob("1990-01-01")
                        .setNationality("MLT")
                        .build();

        return new Builder()
                .setApplicant(ApplicantModel.defaultApplicantModel(fixedInfoModel).build())
                .setPositions(positions)
                .setType("ubo");
    }


    public static Builder defaultAddShareholderModel() {
        Faker faker=new Faker();
        final FixedInfoModel fixedInfoModel =
                FixedInfoModel.builder()
                        .setCompanyInfo(CompanyInfoModel.builder()
                                .setCompanyName(faker.company().name())
                                .setCountry("MLT")
                                .setRegistrationNumber("123456789")
                                .setAddress(new CompanyAddressModel("Valletta", "MLT", "MLT01"))
                                .setIncorporatedOn("1970-01-01")
                                .setLegalAddress(faker.address().fullAddress())
                                .build())
                        .build();

        return new Builder()
                .setApplicant(ApplicantModel.defaultApplicantModel(fixedInfoModel)
                        .setLang("en")
                        .setType("company")
                        .build())
                .setType("shareholder")
                .setInRegistry(false)
                .setShareSize(50);
    }

    public static Builder defaultAddRepresentativeModel() {

        final FixedInfoModel fixedInfoModel =
                FixedInfoModel.builder()
                        .setFirstName(RandomStringUtils.randomAlphabetic(5))
                        .setLastName(RandomStringUtils.randomAlphabetic(5))
                        .setDob("1990-01-01")
                        .setNationality("MLT")
                        .setPhone("+35679888888")
                        .build();

        return new Builder()
                .setApplicant(ApplicantModel.defaultApplicantModel(fixedInfoModel).build())
                .setInRegistry(true)
                .setType("representative");
    }

    public static Builder rootUserAddBeneficiaryModel(final List<String> positions,
                                                      final CreateCorporateModel createCorporateModel) {

        final DateOfBirthModel dateOfBirth = createCorporateModel.getRootUser().getDateOfBirth();

        final FixedInfoModel fixedInfoModel =
                FixedInfoModel.builder()
                        .setFirstName(createCorporateModel.getRootUser().getName())
                        .setLastName(createCorporateModel.getRootUser().getSurname())
                        .setDob(String.format("%s-%s-%s", dateOfBirth.getYear(), dateOfBirth.getMonth(), dateOfBirth.getDay()))
                        .setNationality("MLT")
                        .setPhone(String.format("%s%s", createCorporateModel.getRootUser().getMobile().getCountryCode(), createCorporateModel.getRootUser().getMobile().getNumber()))
                        .setAddresses(Collections.singletonList(SumSubAddressModel.builder()
                                .setStreet(createCorporateModel.getCompany().getBusinessAddress().getAddressLine1())
                                .setSubStreet(createCorporateModel.getCompany().getBusinessAddress().getAddressLine2())
                                .setTown(createCorporateModel.getCompany().getBusinessAddress().getCity())
                                .setPostCode(createCorporateModel.getCompany().getBusinessAddress().getPostCode())
                                .setCountry("MLT")
                                .setState(createCorporateModel.getCompany().getBusinessAddress().getState())
                                .build()))
                        .setPlaceOfBirth("Malta")
                        .setCountry("MLT")
                        .build();

        return new Builder()
                .setApplicant(ApplicantModel.builder()
                        .setEmail(createCorporateModel.getRootUser().getEmail())
                        .setInfo(fixedInfoModel)
                        .build())
                .setPositions(positions)
                .setType("ubo");
    }

    public static Builder rootUserAddRepresentativeModel(final CreateCorporateModel createCorporateModel) {

        final DateOfBirthModel dateOfBirth = createCorporateModel.getRootUser().getDateOfBirth();

        final FixedInfoModel fixedInfoModel =
                FixedInfoModel.builder()
                        .setFirstName(createCorporateModel.getRootUser().getName())
                        .setLastName(createCorporateModel.getRootUser().getSurname())
                        .setDob(String.format("%s-%s-%s", dateOfBirth.getYear(), dateOfBirth.getMonth(), dateOfBirth.getDay()))
                        .setNationality("MLT")
                        .setPhone(String.format("%s%s", createCorporateModel.getRootUser().getMobile().getCountryCode(), createCorporateModel.getRootUser().getMobile().getNumber()))
                        .setAddresses(Collections.singletonList(SumSubAddressModel.builder()
                                .setStreet(createCorporateModel.getCompany().getBusinessAddress().getAddressLine1())
                                .setSubStreet(createCorporateModel.getCompany().getBusinessAddress().getAddressLine2())
                                .setTown(createCorporateModel.getCompany().getBusinessAddress().getCity())
                                .setPostCode(createCorporateModel.getCompany().getBusinessAddress().getPostCode())
                                .setCountry("MLT")
                                .setState(createCorporateModel.getCompany().getBusinessAddress().getState())
                                .build()))
                        .setPlaceOfBirth("Malta")
                        .setCountry("MLT")
                        .build();

        return new Builder()
                .setApplicant(ApplicantModel.builder()
                        .setEmail(createCorporateModel.getRootUser().getEmail())
                        .setInfo(fixedInfoModel)
                        .build())
                .setInRegistry(true)
                .setType("representative");
    }

    public static Builder rootUserAddRepresentativeModel(final CreateBuyerModel createBuyerModel) {

        final DateOfBirthModel dateOfBirth = createBuyerModel.getAdminUser().getDateOfBirth();

        final FixedInfoModel fixedInfoModel =
                FixedInfoModel.builder()
                        .setFirstName(createBuyerModel.getAdminUser().getName())
                        .setLastName(createBuyerModel.getAdminUser().getSurname())
                        .setDob(String.format("%s-%s-%s", dateOfBirth.getYear(), dateOfBirth.getMonth(), dateOfBirth.getDay()))
                        .setNationality("MLT")
                        .setPhone(String.format("%s%s", createBuyerModel.getAdminUser().getMobile().getCountryCode(), createBuyerModel.getAdminUser().getMobile().getNumber()))
                        .setAddresses(Collections.singletonList(SumSubAddressModel.builder()
                                .setStreet(createBuyerModel.getCompany().getBusinessAddress().getAddressLine1())
                                .setSubStreet(createBuyerModel.getCompany().getBusinessAddress().getAddressLine2())
                                .setTown(createBuyerModel.getCompany().getBusinessAddress().getCity())
                                .setPostCode(createBuyerModel.getCompany().getBusinessAddress().getPostCode())
                                .setCountry("MLT")
                                .setState(createBuyerModel.getCompany().getBusinessAddress().getState())
                                .build()))
                        .setPlaceOfBirth("Malta")
                        .setCountry("MLT")
                        .build();

        return new Builder()
                .setApplicant(ApplicantModel.builder()
                        .setEmail(createBuyerModel.getAdminUser().getEmail())
                        .setInfo(fixedInfoModel)
                        .build())
                .setInRegistry(true)
                .setType("representative");
    }

    public static Builder defaultAddDirectorModel() {

        final FixedInfoModel fixedInfoModel =
                FixedInfoModel.builder()
                        .setFirstName(RandomStringUtils.randomAlphabetic(5))
                        .setLastName(RandomStringUtils.randomAlphabetic(5))
                        .setDob("1990-01-01")
                        .setNationality("MLT")
                        .build();

        return new Builder()
                .setApplicant(ApplicantModel.defaultApplicantModel(fixedInfoModel).build())
                .setInRegistry(false)
                .setType("director");
    }

    public static Builder addDirectorWithSpecialCharactersModel() {

        final FixedInfoModel fixedInfoModel =
                FixedInfoModel.builder()
                        .setFirstName(String.format("ӔÄ%sĪŽ", RandomStringUtils.randomAlphabetic(3)))
                        .setLastName(String.format("Ö%sßЭ", RandomStringUtils.randomAlphabetic(3)))
                        .setDob("1990-01-01")
                        .setNationality("MLT")
                        .build();

        return new Builder()
                .setApplicant(ApplicantModel.defaultApplicantModel(fixedInfoModel).build())
                .setInRegistry(false)
                .setType("director");
    }
}
