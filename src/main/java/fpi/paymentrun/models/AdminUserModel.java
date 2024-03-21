package fpi.paymentrun.models;

import commons.enums.Roles;
import commons.models.CompanyPosition;
import commons.models.DateOfBirthModel;
import commons.models.MobileNumberModel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;

@Builder
@Getter
@Setter
public class AdminUserModel {

    private String name;
    private String surname;
    private String email;
    private MobileNumberModel mobile;
    private CompanyPosition companyPosition;
    private DateOfBirthModel dateOfBirth;
    private List<String> roles;

    public static AdminUserModel.AdminUserModelBuilder defaultAdminUserModel() {
        return AdminUserModel.builder()
                .name(RandomStringUtils.randomAlphabetic(6))
                .surname(RandomStringUtils.randomAlphabetic(6))
                .email(String.format("%s%s@weavrtest.io", System.currentTimeMillis(),
                        RandomStringUtils.randomAlphabetic(5)))
                .mobile(MobileNumberModel.random())
                .companyPosition(CompanyPosition.getRandomCompanyPosition())
                .dateOfBirth(new DateOfBirthModel(1990, 1, 1));
    }

    /**
     * Method for updateBuyerModel
     */
    public static AdminUserModel.AdminUserModelBuilder defaultUpdateBuyerAdminUser() {
        return AdminUserModel.builder()
                .name(RandomStringUtils.randomAlphabetic(6))
                .surname(RandomStringUtils.randomAlphabetic(6))
                .email(String.format("%s%s@weavrtest.io", System.currentTimeMillis(),
                        RandomStringUtils.randomAlphabetic(5)))
                .mobile(MobileNumberModel.random())
                .dateOfBirth(new DateOfBirthModel(1980, 12, 12));
    }

    public static AdminUserModel.AdminUserModelBuilder creatorRoleForUpdateBuyer() {
        return AdminUserModel.builder()
                .roles(List.of(Roles.ADMIN.name(), Roles.CREATOR.name()));
    }

    public static AdminUserModel.AdminUserModelBuilder controllerRoleForUpdateBuyer() {
        return AdminUserModel.builder()
                .roles(List.of(Roles.ADMIN.name(), Roles.CONTROLLER.name()));
    }

    public static AdminUserModel.AdminUserModelBuilder allRolesForUpdateBuyer() {
        return AdminUserModel.builder()
                .roles(List.of(Roles.ADMIN.name(), Roles.CONTROLLER.name(), Roles.CREATOR.name()));
    }

    public static AdminUserModel.AdminUserModelBuilder adminRoleForUpdateBuyer() {
        return AdminUserModel.builder()
                .roles(List.of(Roles.ADMIN.name()));
    }
}
