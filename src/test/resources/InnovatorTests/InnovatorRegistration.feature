Feature: CucumberJava

  @RegistrationSuccess
  Scenario: Register Innovator
    Given I register a new innovator
    Then the registration is successful with status code '200'