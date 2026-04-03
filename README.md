# Transaction Ledger

A financial ledger application that stores transactions in USD and provides currency conversion using the U.S. Treasury Reporting Rates of Exchange API, during retrieval.

## Overview

The Transactions Ledger allows users to persist purchase data and retrieve those records converted into any currency supported by the [U.S. government](https:/ /fiscaldata.treasury.gov/datasets/treasury-reporting-rates-exchange/treasury-reporting-rates-of-exchange
). It features strict historical rate matching logic to ensure financial accuracy.

## Business Requirements

### 1. Store Purchase Transactions
* Description: Limited to 50 characters.
* Transaction Date: Valid date format.
* Purchase Amount: Positive value, stored in USD, rounded to the nearest cent.
* Identifier: Assigned a UUID upon storage.

### 2. Currency Conversion Logic
* The 6-Month Rule: Uses the exchange rate active for the date of purchase. If an exact match is not found, it pulls the most recent rate less than or equal to the purchase date, within a 6-month window.
* Error Handling: If no rate exists within that window, the API returns the specific message: "The purchase cannot be converted to the target currency."
* Rounding: All converted amounts are rounded to exactly two decimal places.

## Assumptions:

### 1. Currency Support & Naming
* **Naming Convention:** The retrieval endpoint requires targetCurrency as Query Param the **Full Form** of the country_currency_desc name (e.g., `Mexico-Peso`, `India-Rupee`) rather than 3-letter ISO codes (GBP, INR, EUR).
* **Rationale:** This ensures a direct and accurate match with the `country_currency_desc` field provided by the [Treasury Reporting Rates of Exchange API](https://fiscaldata.treasury.gov/datasets/treasury-reporting-rates-exchange/treasury-reporting-rates-of-exchange), which is the authoritative data source for this project. This is the only unique field in the specs to get a currency rate. We could set an internal mapping if ISO currency code is required.

### 2. Idempotency
* **Idempotency-Key* This is a required header to be provided by client
* **Rationale:** This ensures that there are no duplicates records inserted into db if client reties multiple times

### 3. transactionDate
* **format:**  The format for transactionDate is YYYY-MM-DDTHH:mmZ, which is ISO-8601 international standard
* **Rationale:** A transaction could have Date and Time with timezone

### 4. Difference between dev and prod config
* **Database* H2 in dev ; Postgres in Prod

## Technical Stack

* Java 17
* Spring Boot 3.5.12
* H2 Database (In-memory persistence)
* PostGres (for Prod Profile)
* Docker (Containerization)
* Cucumber (BDD Testing)
* JUnit 5 and Mockito

## Containerization and Setup

This project is Docker-ready, ensuring a consistent environment for development and testing.

This needs an env file that is read by docker
Create a .env file with the following details at the project root. This is mainly used for Prod
```bash
# Database Settings
POSTGRES_DB=purchase_db
POSTGRES_USER=<insert_user_name>
POSTGRES_PASSWORD=<insert_secure_password>
DB_URL=jdbc:postgresql://db:5432/purchase_db

```
### Running the app

#### Run with script that has prompts or follow the instructions below.
* Use run-app.sh 

#### Run locally:
```bash
./mvnw clean install
./mvnw spring-boot:run
```
#### Run with Docker

#### Dev - uses H2 (In Memory)
```bash
docker-compose up --build
docker-compose up -d app
```

#### To Run in Prod Profile - uses PostGres
Run it with prod profile in .env file 
```bash
docker-compose up --build
SPRING_PROFILES_ACTIVE=prod docker-compose up -d app
```

### Useful commands
To bring down the app
```bash
docker-compose down
````
To check logs use the following command
```bash
docker logs transaction-ledger
````
### API End Points
### 1. To Store a Transaction (POST)
Registers a new purchase into the ledger. Depending on your active profile, this will persist to either the H2 (In-Memory) or PostgreSQL database.

Endpoint: POST /api/v1/transactions
Sample Request and Response : 

```bash
curl -X POST http://localhost:8080/api/v1/transactions \
-H "Content-Type: application/json" \
-H "Idempotency-Key: 1497C736-DB21-43EE-BEDE-F3E490AC11F4" \
-d '{
  "description": "MacBook",                   
  "transactionDate": "2026-03-31T10:15:30Z",
  "amountUsd": 1500.00
}'
```
You should get a response like the following:
```bash
{"id":"158c1deb-c765-4be2-b4b2-64bd38e2efe3","idempotencyKey":"1497C736-DB21-43EE-BEDE-F3E490AC11F4","description":"Custom Mechanical Keyboard","transactionDate":"2026-02-01","amountUsd":150.00}
```
#### Response Status:
* Success Response: 201 Created

* Validation: 
  * All fields are mandatory. If any are missing or malformed, the API returns a 400 Bad Request.
  * If Same IdempotencyKey is present in the database, the API returns a 409 Conflict.

### 2. To Retrieve a Converted Transaction (GET)

Fetches a specific transaction and converts the USD amount into a target currency using the U.S. Treasury Reporting Rates of Exchange.

* Endpoint: GET /api/v1/transactions/{id}/convert
* Parameters:
   id: The UUID of the transaction (Path Variable).

targetCurrency: The name of the currency, e.g., Mexico-Peso (Query Parameter).
Sample Request and Response:
```bash
curl -v "http://localhost:8080/api/v1/transactions/6d8fc54e-39f2-4955-be24-44d5d7b61adf/convert?targetCurrency=Australia-Dollar"
```
```bash
{"id":"6d8fc54e-39f2-4955-be24-44d5d7b61adf","description":"NoteBook","transactionDate":"2026-01-31","originalAmountUsd":150.00,"convertedAmount":231.30,"targetCurrency":"Australia-Dollar"}
```

#### Response Status:
* Success Response: 200 OK
* Validations: 
    * If id malformed, the API returns a 400 Bad Request.
    * If no id is found in the database, it results in 404 with the message "Transaction not found with id: {id}"
    * If the currency is not found or exchange rate can't be found within the last 6 months, The api will result in 404 with The purchase cannot be converted to the target currency.

## TroubleShooting

#### 1. 404 Not Found: 
   Ensure the UUID exists in your currently active database (H2 data is wiped on restart, while Postgres is persistent).

#### 2. Currency Names: 
   Use the official Treasury format for the targetCurrency parameter (e.g., Canada-Dollar or United Kingdom-Pound) as the API is not supporting ISO Format
   


