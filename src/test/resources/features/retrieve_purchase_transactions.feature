Feature: Retrieve Purchase Transactions
  As a user, I want to convert stored USD transactions to other currencies.

  @HappyPath
  Scenario Outline: Successfully convert a transaction with rounding
    Given a purchase exists with an amount of <amount> "USD" dated "<date>"
    And the Treasury API is stubbed to return <rate> for "<target>"
    When I request the conversion to "<target>" for that transaction
    Then the response status should be <statusCode>
    And the converted amount should be <expected>

    Examples:
      | amount | date                 | rate      | target | expected | statusCode |
      | 100.00 | 2026-03-31T10:15:30Z | 0.925     | EUR    | 92.50    | 200        |
      | 100.25 | 2026-03-31T10:15:30Z | 0.08254   | GBP    | 8.27     | 200        |
      | 50.00  | 2026-03-31T10:15:30Z | 150.1234  | JPY    | 7506.17  | 200        |
      | 0.99   | 2026-03-15T10:15:30Z | 0.12345   | CAD    | 0.12     | 200        |

  @UnhappyPath @NoRate
  Scenario: Fail conversion when no exchange rate is found
    Given a purchase exists with an amount of 100.00 "USD" dated "2020-03-31T10:15:30Z"
    And the Treasury API has no rate for "EUR" within the last 6 months
    When I request the conversion to "EUR" for that transaction
    Then the system should return an error "The purchase cannot be converted to the target currency."


  @UnhappyPath @TxNotFound
  Scenario: Fail conversion when the transaction does not exist in the database
    Given no purchase transaction exists for the provided ID
    When I request the conversion to "EUR" for an unknown transaction
    Then the system should return an error "Transaction not found"