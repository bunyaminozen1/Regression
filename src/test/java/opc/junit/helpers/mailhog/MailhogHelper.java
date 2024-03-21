package opc.junit.helpers.mailhog;

import io.restassured.response.Response;
import opc.junit.helpers.TestHelper;
import opc.models.mailhog.MailHogMessageResponse;
import opc.services.mailhog.MailhogService;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.http.HttpStatus.SC_OK;

public class MailhogHelper {

    public static Pair<String,String> getUserInviteNonceAndInviteId(final String emailAddress) {

        final Response response = TestHelper.ensureAsExpected(15,
                () -> MailhogService.getMailHogEmail(emailAddress),
                SC_OK);

        final String emailBodyString = response
                .jsonPath()
                .getString("items[0].Content.Body");

        final Pattern noncePattern = Pattern.compile("nonce\\=([0-9a-z]*)");
        final Pattern invitesPattern = Pattern.compile("invites/([0-9]*)");
        final Matcher nonceMatcher = noncePattern.matcher(emailBodyString);
        final Matcher invitesMatcher = invitesPattern.matcher(emailBodyString);

        if (!nonceMatcher.find()) {
            throw new IllegalStateException("Nonce not found");
        }else if (!invitesMatcher.find()){
            throw new IllegalStateException("Invites not found");
        }
        return Pair.of(nonceMatcher.group(1), invitesMatcher.group(1));
    }


    public static int getMailhogEmailCount(final String emailAddress) {

        return Integer.parseInt(TestHelper.ensureAsExpected(15,
                () -> MailhogService.getMailHogEmail(emailAddress),
                SC_OK).jsonPath().getString("count"));
    }

    public static MailHogMessageResponse getMailHogEmail(final String emailAddress) {

        final Response response = TestHelper.ensureAsExpected(15,
                () -> MailhogService.getMailHogEmail(emailAddress),
                SC_OK);

        return MailHogMessageResponse.convertEmail(response);
    }

    public static MailHogMessageResponse getMailHogSms(final String mobileNumber) {

        final Response response = TestHelper.ensureAsExpected(15,
                () -> MailhogService.getMailHogSms(mobileNumber),
                SC_OK);

        return MailHogMessageResponse.convertSms(response);
    }
}
