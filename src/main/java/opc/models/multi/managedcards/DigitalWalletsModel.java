package opc.models.multi.managedcards;

public class DigitalWalletsModel {

    private final boolean walletsEnabled;
    private final String artworkReference;
    private final boolean pushProvisioningEnabled;

    public DigitalWalletsModel(final Builder builder) {
        this.walletsEnabled = builder.walletsEnabled;
        this.artworkReference = builder.artworkReference;
        this.pushProvisioningEnabled = builder.pushProvisioningEnabled;
    }

    public boolean isWalletsEnabled() {
        return walletsEnabled;
    }

    public String getArtworkReference() {
        return artworkReference;
    }

    public boolean isPushProvisioningEnabled() {
        return pushProvisioningEnabled;
    }

    public static class Builder {
        private boolean walletsEnabled;
        private String artworkReference;
        private boolean pushProvisioningEnabled;

        public Builder setWalletsEnabled(boolean walletsEnabled) {
            this.walletsEnabled = walletsEnabled;
            return this;
        }

        public Builder setArtworkReference(String artworkReference) {
            this.artworkReference = artworkReference;
            return this;
        }

        public Builder setPushProvisioningEnabled(boolean pushProvisioningEnabled) {
            this.pushProvisioningEnabled = pushProvisioningEnabled;
            return this;
        }

        public DigitalWalletsModel build() { return new DigitalWalletsModel(this); }
    }

    public static Builder builder(){ return new Builder(); }

    public static Builder builder(final boolean isManualProvisioningEnabled,
                                  final boolean isPushProvisioningEnabled){
        return new Builder()
                .setWalletsEnabled(isManualProvisioningEnabled)
                .setPushProvisioningEnabled(isPushProvisioningEnabled);
    }
}
