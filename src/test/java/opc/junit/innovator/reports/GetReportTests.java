package opc.junit.innovator.reports;

import opc.services.innovator.InnovatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class GetReportTests extends BaseReportsSetup {

    @Test
    public void GetReport_Success(){
        InnovatorService.getReport(TOP_REPORT_ID, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(TOP_REPORT_ID))
                .body("embed.url", notNullValue())
                .body("title", equalTo("Managed Accounts Outgoing Transfers"))
                .body("description", equalTo("Insights on managed account outgoing transfers, by currency"))
                .body("category", equalTo("MANAGED_ACCOUNTS"));
    }

    @Test
    public void GetReport_UnknownReport_NotFound(){
        InnovatorService.getReport(RandomStringUtils.randomNumeric(10), innovatorToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetReport_InvalidToken_Unauthorised(){
        InnovatorService.getReport(TOP_REPORT_ID, RandomStringUtils.randomAlphanumeric(10))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }
}
