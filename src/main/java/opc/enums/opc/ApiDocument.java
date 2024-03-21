package opc.enums.opc;

public enum ApiDocument {

    MULTI(""),
    WEBHOOK("webhooks.yaml"),
    PLUGIN_WEBHOOK("plugin_webhooks.yaml");

    private final String filename;

    ApiDocument(final String filename) {
        this.filename = filename;
    }

    public String getFilename(){
        return filename;
    }
}
