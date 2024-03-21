package fpi.paymentrun.models;

import commons.models.DateOfBirthModel;
import commons.models.MobileNumberModel;
import fpi.paymentrun.enums.AuthorisedUserRole;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;

@Builder
@Getter
@Setter
public class BuyerAuthorisedUserModel {

    private String name;
    private String surname;
    private String email;
    private DateOfBirthModel dateOfBirth;
    private MobileNumberModel mobile;
    private List<String> roles;

    public static BuyerAuthorisedUserModelBuilder defaultUsersModel() {
        return BuyerAuthorisedUserModel.builder()
                .name(RandomStringUtils.randomAlphabetic(6))
                .surname(RandomStringUtils.randomAlphabetic(6))
                .email(String.format("%s@weavrusertest.io", RandomStringUtils.randomAlphabetic(10)))
                .dateOfBirth(new DateOfBirthModel(1992, 3, 3))
                .mobile(MobileNumberModel.random())
                .roles(List.of(AuthorisedUserRole.getRandomRole().name()));
    }

    public static BuyerAuthorisedUserModelBuilder controllerRoleUsersModel() {
        return BuyerAuthorisedUserModel.builder()
                .roles(List.of(AuthorisedUserRole.CONTROLLER.name()));
    }

    public static BuyerAuthorisedUserModelBuilder creatorRoleUsersModel() {
        return BuyerAuthorisedUserModel.builder()
                .roles(List.of(AuthorisedUserRole.CREATOR.name()));
    }

    public static BuyerAuthorisedUserModelBuilder allRolesUsersModel() {
        return BuyerAuthorisedUserModel.builder()
                .roles(List.of(AuthorisedUserRole.CONTROLLER.name(), AuthorisedUserRole.CREATOR.name()));
    }
}
