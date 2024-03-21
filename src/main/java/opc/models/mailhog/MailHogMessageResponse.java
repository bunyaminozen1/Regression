package opc.models.mailhog;

import io.restassured.response.Response;
import org.jsoup.Jsoup;

public class MailHogMessageResponse {

    private final String from;
    private final String subject;
    private final String to;
    private final String body;

    public MailHogMessageResponse(final Builder builder) {
        this.from = builder.from;
        this.subject = builder.subject;
        this.to = builder.to;
        this.body = builder.body;
    }

    public String getFrom() {
        return from;
    }

    public String getSubject() {
        return subject;
    }

    public String getTo() {
        return to;
    }

    public String getBody() {
        return body;
    }

    public static class Builder {
        private String from;
        private String subject;
        private String to;
        private String body;

        public Builder setFrom(String from) {
            this.from = from;
            return this;
        }

        public Builder setSubject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder setTo(String to) {
            this.to = to;
            return this;
        }

        public Builder setBody(String body) {
            this.body = body;
            return this;
        }

        public MailHogMessageResponse build() { return new MailHogMessageResponse(this); }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MailHogMessageResponse convertEmail(final Response emailResponse){
        return new Builder()
                .setFrom(emailResponse.jsonPath().get("items.Content.Headers.From[0][0]"))
                .setSubject(emailResponse.jsonPath().get("items.Content.Headers.Subject[0][0]"))
                .setTo(emailResponse.jsonPath().get("items.Content.Headers.To[0][0]"))
                .setBody(Jsoup.parse(emailResponse.jsonPath().get("items.MIME[0].Parts[0].Body")).body().text().replace("%40= ", "@").replace("%40", "@"))
                .build();
    }

    public static MailHogMessageResponse convertSms(final Response smsResponse){
        return new Builder()
                .setFrom(smsResponse.jsonPath().get("items.Content.Headers.From[0][0]"))
                .setTo(smsResponse.jsonPath().get("items.Content.Headers.To[0][0]"))
                .setBody(smsResponse.jsonPath().get("items.Content.Body[0]"))
                .build();
    }
}
