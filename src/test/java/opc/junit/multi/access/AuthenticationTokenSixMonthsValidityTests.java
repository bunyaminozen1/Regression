package opc.junit.multi.access;

import opc.services.multi.ConsumersService;
import opc.services.multi.CorporatesService;
import opc.tags.MultiTags;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.apache.http.HttpStatus.SC_OK;

@Tag(MultiTags.MULTI)
@Tag(MultiTags.AUTHENTICATION)
public class AuthenticationTokenSixMonthsValidityTests {

    private final String SECRET_KEY = "W/knNm5TboUBfd2TqOcBCQ==";

    @Test
    public void ConsumerImpersonation_TokenGenerated_2024_03_07_Success() {

        final String consumerToken =
                "eyJraWQiOiJnZW5lcmF0b3IiLCJhbGciOiJFUzI1NiJ9.eyJTWVNURU0iOiJmYWxzZSIsInN1YiI6IlJPT1QsMTA5NDA0MzQyMjM0Nzc1NTY1IiwiVE9LRU5fUFJPVklERVIiOiJFTUFJTF9BTkRfUEFTU1dPUkQiLCJSQU5ET00iOiI3ODI3NTk3MzkzNTExNjc1NTk0IiwiSURFTlRJVFlfSUQiOiIxMDk0MDQzNDIyMzQ3NzU1NjUiLCJJREVOVElUWV9UWVBFIjoiY29uc3VtZXJzIiwiUEVSUEVUVUFMIjoidHJ1ZSIsIlRPS0VOX1RZUEUiOiJBQ0NFU1MiLCJURU5BTlRfSUQiOiI5ODQ2IiwiSU1QRVJTT05BVE9SX1NFU1NJT05fSUQiOiIwIiwiU0VTU0lPTl9JRCI6IjExMjA1NDA2NDk2MDExMDYxMSIsIlBST0dSQU1NRV9JRCI6IjEwNzQ4NTU5MjM4NDM3Mjc0NCIsImV4cCI6MTcyNTcwNjk0NiwiREVWSUNFX0lEIjoiIiwiSU1QRVJTT05BVEVEIjoiZmFsc2UiLCJBVVRIX0dST1VQX0lEIjoiIn0.zhIgnnjR3FxVFFTUD7z7iC1sgVqU6x1rHPe8CdvW4uwUluP6RxY2MASba2jYVdc8QaL25rVTT6EtHXc6dr80Qg";

        final int statusCode =
                ConsumersService.getConsumers(SECRET_KEY, consumerToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract().statusCode();

        if (LocalDate.now().isAfter(LocalDate.parse("2024-03-07", DateTimeFormatter.ofPattern( "yyyy-MM-dd" )).plusMonths(6)) && statusCode == SC_OK) {
            Assertions.fail("Token still valid after 6 Months");
        }
    }

    @Test
    public void CorporateImpersonation_TokenGenerated_2024_03_07_Success() {

        final String corporateToken =
                "eyJraWQiOiJnZW5lcmF0b3IiLCJhbGciOiJFUzI1NiJ9.eyJTWVNURU0iOiJmYWxzZSIsInN1YiI6IlJPT1QsMTA5NDA0MzM2NjY5OTE3MTk3IiwiVE9LRU5fUFJPVklERVIiOiJFTUFJTF9BTkRfUEFTU1dPUkQiLCJSQU5ET00iOiItNjczOTIyMDYxODc2NjYzODE1OSIsIklERU5USVRZX0lEIjoiMTA5NDA0MzM2NjY5OTE3MTk3IiwiSURFTlRJVFlfVFlQRSI6ImNvcnBvcmF0ZXMiLCJQRVJQRVRVQUwiOiJ0cnVlIiwiVE9LRU5fVFlQRSI6IkFDQ0VTUyIsIlRFTkFOVF9JRCI6Ijk4NDYiLCJJTVBFUlNPTkFUT1JfU0VTU0lPTl9JRCI6IjAiLCJQUk9HUkFNTUVfSUQiOiIxMDc0ODU1OTIzODQzNzI3NDQiLCJTRVNTSU9OX0lEIjoiMTEyMDU0MDYyNjQ3Mjc5NjM0IiwiZXhwIjoxNzI1NzA2OTExLCJERVZJQ0VfSUQiOiIiLCJJTVBFUlNPTkFURUQiOiJmYWxzZSIsIkFVVEhfR1JPVVBfSUQiOiIifQ.KbRvvBR1t7CKXEWLEQz1sH5xAkl89DGYOx7eu3yW1CBt6bkQy5ypWoFQdBV2j6wOZJk8k1nI2HbPCWTopu45cA";

        final int statusCode =
                CorporatesService.getCorporates(SECRET_KEY, corporateToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract().statusCode();

        if (LocalDate.now().isAfter(LocalDate.parse("2024-03-07", DateTimeFormatter.ofPattern( "yyyy-MM-dd" )).plusMonths(6)) && statusCode == SC_OK) {
            Assertions.fail("Token still valid after 6 Months");
        }
    }
}
