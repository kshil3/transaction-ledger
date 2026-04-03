Feature: Store Purchase Transactions
  As a user, I want to store transactions in the ledger with an idempotency key.

  @HappyPath
  Scenario Outline: Successfully store a transaction
    Given a new transaction request with description "<desc>", amount <amount>, and datetime "<datetime>"
    And an idempotency key "<key>"
    When I submit the store transaction request
    Then the response status should be <status>
    And the database should contain a transaction with description "<desc>"

    Examples:
      | desc       | amount   | datetime                | key                                   |status     |
      | Tesla Car  | 49999.99 | 2026-03-31T10:15:30Z    | 1497C736-DB21-43EE-BEDE-F3E490AC11F2  |201        |
      | Mac Book   | 1999.00  | 2026-03-15T10:15:30Z    | 1497C736-DB21-43EE-BEDE-F3E490AC11F3  |201        |


  @SadPath @Validation
  Scenario Outline: Fail to store transaction with invalid data
    Given a new transaction request with description "<desc>", amount <amount>, and datetime "<datetime>"
    And an idempotency key "<key>"
    When I submit the store transaction request
    Then the response status should be <status>
    Examples:
      | desc       | amount   | datetime                | key                                   |status     |
      |            | -50      | 2026-03-31T10:15:30Z    | 2497C736-DB21-43EE-BEDE-F3E490AC11F2  |400        |

  @SadPath @IdempotencyValidation
  Scenario Outline: Fail to store transaction with Idempotency Validation
    Given a new transaction request with description "<desc>", amount <amount>, and datetime "<datetime>"
    And an idempotency key "<key>"
    When I submit the store transaction request
    Then the response status should be 201
    When I submit the store transaction request
    Then the response status should be 409
    Examples:
      | desc       | amount   | datetime                | key |
      | Tesla Car  | 49999.99 | 2026-03-31T10:15:30Z    | 1497C736-DB21-43EE-BEDE-F3E490AC11F4 |


