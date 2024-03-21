package commons.models.innovator;

import lombok.Getter;
import opc.enums.opc.IdentityProfileAuthentication;
import opc.models.innovator.AuthenticationFactorLevelsModel;
import opc.models.innovator.AuthenticationFactorsModel;
import opc.models.innovator.FactorModel;
import opc.models.shared.ProfileFactors;

import java.util.ArrayList;
import java.util.List;

@Getter
public class IdentityProfileAuthenticationModel {

    private AuthenticationFactorLevelsModel factorLevels;

    public IdentityProfileAuthenticationModel(AuthenticationFactorLevelsModel factorLevels) {
        this.factorLevels = factorLevels;
    }

    public IdentityProfileAuthenticationModel setFactorLevels(AuthenticationFactorLevelsModel factorLevels) {
        this.factorLevels = factorLevels;
        return this;
    }

    public static IdentityProfileAuthenticationModel DefaultAccountInfoIdentityProfileAuthenticationScheme() {
        return new IdentityProfileAuthenticationModel(new AuthenticationFactorLevelsModel(new AuthenticationFactorsModel(List.of(new FactorModel("PASSWORD", "passwords")))));
    }

    public static IdentityProfileAuthenticationModel SmsOtpAccountInfoIdentityProfileAuthenticationScheme() {
        return new IdentityProfileAuthenticationModel(new AuthenticationFactorLevelsModel(new AuthenticationFactorsModel(List.of(new FactorModel("PASSWORD", "passwords"),
                new FactorModel("SMS_OTP", "weavr_authfactors")))));
    }

    public static IdentityProfileAuthenticationModel DefaultSmsIdentityProfileAuthenticationScheme() {
        return new IdentityProfileAuthenticationModel(
                new AuthenticationFactorLevelsModel(
                        new AuthenticationFactorsModel(List.of(new FactorModel("SMS_OTP", "weavr_authfactors")))));
    }

    public static IdentityProfileAuthenticationModel DefaultPaymentInitIdentityProfileAuthenticationScheme() {
        return new IdentityProfileAuthenticationModel(
                new AuthenticationFactorLevelsModel(
                        new AuthenticationFactorsModel(List.of(new FactorModel("SMS_OTP", "weavr_authfactors"),
                                new FactorModel("AUTHY_PUSH", "authy")))));
    }

    public static IdentityProfileAuthenticationModel AllAccountInformationFactorsIdentityProfileAuthenticationScheme() {
        return new IdentityProfileAuthenticationModel(
                new AuthenticationFactorLevelsModel(
                        new AuthenticationFactorsModel(List.of(new FactorModel("PASSWORD", "passwords"))),
                        new AuthenticationFactorsModel(List.of(new FactorModel("SMS_OTP", "weavr_authfactors"),
                                new FactorModel("AUTHY_PUSH", "authy"),
                                new FactorModel("OKAY_PUSH", "okay")))));
    }

    public static IdentityProfileAuthenticationModel AllFactorsIdentityProfileAuthenticationScheme() {
        return new IdentityProfileAuthenticationModel(
                new AuthenticationFactorLevelsModel(
                        new AuthenticationFactorsModel(List.of(new FactorModel("SMS_OTP", "weavr_authfactors"),
                                new FactorModel("AUTHY_PUSH", "authy"),
                                new FactorModel("OKAY_PUSH", "okay")))));
    }

    public static IdentityProfileAuthenticationModel DefaultThreeDsIdentityProfileAuthenticationScheme() {
        return new IdentityProfileAuthenticationModel(
                new AuthenticationFactorLevelsModel(
                        new AuthenticationFactorsModel(List.of(new FactorModel("SMS_OTP", "weavr_authfactors"),
                                new FactorModel("OKAY_PUSH", "okay"))),
                        new AuthenticationFactorsModel(List.of(new FactorModel("SMS_OTP", "weavr_authfactors")))));
    }

    public static IdentityProfileAuthenticationModel SecondaryThreeDsIdentityProfileAuthenticationScheme() {
        return new IdentityProfileAuthenticationModel(
                new AuthenticationFactorLevelsModel(
                        new AuthenticationFactorsModel(List.of(new FactorModel("SMS_OTP", "weavr_authfactors"),
                                new FactorModel("AUTHY_PUSH", "authy"))),
                        new AuthenticationFactorsModel(List.of(new FactorModel("SMS_OTP", "weavr_authfactors")))));
    }

    public static IdentityProfileAuthenticationModel DefaultBeneficiaryManagementIdentityProfileAuthenticationScheme() {
        return new IdentityProfileAuthenticationModel(
                new AuthenticationFactorLevelsModel(
                        new AuthenticationFactorsModel(List.of(new FactorModel("OKAY_PUSH", "okay")))));
    }

    public static IdentityProfileAuthenticationModel PaymentInitIdentityProfileAuthenticationScheme(final String factorModelType) {
        return new IdentityProfileAuthenticationModel(new AuthenticationFactorLevelsModel(new AuthenticationFactorsModel(List.of(new FactorModel(factorModelType, "weavr_authfactors")))));
    }

    public static IdentityProfileAuthenticationModel BeneficiaryManagementIdentityProfileAuthenticationScheme(final String factorModelType) {
        return new IdentityProfileAuthenticationModel(new AuthenticationFactorLevelsModel(new AuthenticationFactorsModel(List.of(new FactorModel(factorModelType, "weavr_authfactors")))));
    }

    public static IdentityProfileAuthenticationModel dynamicIdentityProfileAuthenticationScheme(final ProfileFactors profileFactors) {

        return getAuthenticationModel(profileFactors);
    }

    private static IdentityProfileAuthenticationModel getAuthenticationModel(final ProfileFactors profileFactors) {

        final List<FactorModel> primaryFactors = new ArrayList<>();
        if (profileFactors.getPrimaryFactors() != null && !profileFactors.getPrimaryFactors().isEmpty()) {
            profileFactors.getPrimaryFactors().forEach(primaryFactor -> {
                final IdentityProfileAuthentication authFactor = IdentityProfileAuthentication.valueOf(primaryFactor.getFactor());
                primaryFactors.add(new FactorModel(authFactor.getType(), authFactor.getProviderKey()));
            });
        }

        final List<FactorModel> secondaryFactors = new ArrayList<>();
        if (profileFactors.getSecondaryFactors() != null && !profileFactors.getSecondaryFactors().isEmpty()) {
            profileFactors.getSecondaryFactors().forEach(secondaryFactor -> {
                final IdentityProfileAuthentication authFactor = IdentityProfileAuthentication.valueOf(secondaryFactor.getFactor());
                secondaryFactors.add(new FactorModel(authFactor.getType(), authFactor.getProviderKey()));
            });
        }

        final AuthenticationFactorLevelsModel authenticationFactorLevelsModel =
                secondaryFactors.isEmpty() ? new AuthenticationFactorLevelsModel(new AuthenticationFactorsModel(primaryFactors))
                        : new AuthenticationFactorLevelsModel(new AuthenticationFactorsModel(primaryFactors), new AuthenticationFactorsModel(secondaryFactors));

        return new IdentityProfileAuthenticationModel(authenticationFactorLevelsModel);


    }
}
