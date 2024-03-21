package opc.models.innovator;

public class CreatePasswordProfileModel {

    private IdentityPasswordModel configPerCredentialType;

    public IdentityPasswordModel getConfigPerCredentialType() {
        return configPerCredentialType;
    }

    public CreatePasswordProfileModel setConfigPerCredentialType(IdentityPasswordModel configPerCredentialType) {
        this.configPerCredentialType = configPerCredentialType;
        return this;
    }

    public static CreatePasswordProfileModel DefaultCreatePasswordProfileModel(){
        return new CreatePasswordProfileModel()
                .setConfigPerCredentialType(new IdentityPasswordModel()
                        .setRoot(new PasswordConfigModel().setMinimumLength(8).setMaximumLength(50).setComplexity(1))
                        .setUser(new PasswordConfigModel().setMinimumLength(8).setMaximumLength(50).setComplexity(1)));
    }

    public static CreatePasswordProfileModel setRandomPasswordProfileModel(final int minLength,
                                                                           final int maxLength){
        return new CreatePasswordProfileModel()
                .setConfigPerCredentialType(new IdentityPasswordModel()
                        .setRoot(new PasswordConfigModel()
                                .setMinimumLength(minLength).setMaximumLength(maxLength).setComplexity(1))
                        .setUser(new PasswordConfigModel()
                                .setMinimumLength(minLength).setMaximumLength(maxLength).setComplexity(1)));
    }
}
