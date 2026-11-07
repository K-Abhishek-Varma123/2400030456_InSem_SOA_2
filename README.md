# Authentication Service - Contract Testing with Dynamic Matchers

This repository contains a Spring Boot starter project implementing Consumer-Driven Contract testing (Spring Cloud Contract) with dynamic matchers for an authentication service.

---

## Scenario Overview

An authentication service processes user credentials via `POST` requests and returns a JSON payload containing a dynamic JWT token. Because JWT tokens and security tokens change upon each generation (due to dynamic timestamps like `iat`, unique nonces, or digital signatures), a contract definition cannot rely on static value matching. A contract must be defined with dynamic matchers (JSONPath regular expression matchers) to validate non-static fields while asserting the protocol, schema, and payload rules.

---

## Tasks Completed

### Step 1: Create POST Contract File
Contract YAML files are defined under the standard contracts resource directory:
- `src/test/resources/contracts/ms1/signin.yml`
- `src/test/resources/contracts/signin.yml`

### Step 2: Specify Request Payload
The request section of the contract specifies:
- **HTTP Method:** `POST`
- **Target URL:** `/ms1/signin`
- **Request Header:** `Content-Type: application/json`
- **Request Body:** Contains credentials:
  ```json
  {
    "username": "user",
    "password": "password123"
  }
  ```

### Step 3: Configure Dynamic Matcher
The response section defines:
- **Expected Response Status:** `200`
- **Response Header:** `Content-Type: application/json`
- **Response Body Template:** Static sample token representation for stubbing.
- **Dynamic Matcher:** A JSONPath regular expression matcher targeting the dynamic `token` field using `by_regex`:
  ```yaml
  matchers:
    body:
      - path: $.token
        type: by_regex
        value: "^\\s*\\S.*$"
  ```
  *(Accepts any non-empty string with at least one non-whitespace character, accommodating dynamic JWT tokens).*

---

## Contract Definition (`src/test/resources/contracts/ms1/signin.yml`)

```yaml
name: should_return_dynamic_jwt_token_on_signin
description: |
  Authentication service processes user credentials via POST request
  and returns a JSON payload containing a dynamic JWT token.
  Contract uses a dynamic JSONPath regex matcher to validate the non-static token field.

# Step 2: Request Payload Specification
request:
  method: POST
  url: /ms1/signin
  headers:
    Content-Type: application/json
  body:
    username: "user"
    password: "password123"
  matchers:
    headers:
      - key: Content-Type
        regex: "application/json.*"

# Step 3: Response & Dynamic Matcher Configuration
response:
  status: 200
  headers:
    Content-Type: application/json
  body:
    token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyIiwiaWF0IjoxNzAwMDAwMDAwLCJleHAiOjE3MDAwMDM2MDB9.dynamicSignature123"
  matchers:
    headers:
      - key: Content-Type
        regex: "application/json.*"
    body:
      # JSONPath regular expression matcher targeting dynamic token field
      # Accepting any non-empty string (at least one non-whitespace character)
      - path: $.token
        type: by_regex
        value: "^\\s*\\S.*$"
```

---

## Viva Questions & Answers

### 1. Why are dynamic regex matchers necessary when validating fields like JWT tokens or timestamps in contract tests?
* **Non-Deterministic Values:** Fields like JWT tokens, UUIDs, transaction IDs, and timestamps are dynamically generated at runtime. For example, a JWT contains claims such as `iat` (issued-at) and `exp` (expiration), which change every second, producing a completely different cryptographic signature each time.
* **Avoiding Brittle Tests:** If a contract test enforces a strict literal equality check on a token or timestamp, tests will fail immediately on the second run because the generated token string will never match the hardcoded fixture.
* **Separation of Concerns (Contract vs. Implementation):** In contract testing, the goal is to verify the agreement between producer and consumer (i.e. *“Does the response contain a non-empty, validly formatted token string at the expected JSON path?”*), rather than whether the cryptographic signature matches a pre-recorded mock string.
* **Dual Role in Contract Testing:**
  - **On the Producer side:** Spring Cloud Contract tests verify that the actual backend service generates a dynamic value conforming to the regular expression (e.g., non-empty string / valid JWT format).
  - **On the Consumer side (WireMock stub):** WireMock returns the sample static body provided in the contract, allowing consumer tests to execute deterministically without hitting the real authentication service.

---

### 2. How does JSONPath syntax allow precise targeting of response fields?
* **Hierarchical Navigation:** JSONPath uses a standardized query notation starting with root `$` to traverse JSON object graphs cleanly:
  - `$.token`: Targets the top-level `token` property.
  - `$.user.id`: Traverses nested objects.
  - `$.items[*].id`: Applies matches across arrays or collections.
* **Isolation of Dynamic Fields:** By specifying the exact JSONPath expression (`path: $.token`), dynamic matching rules (such as regular expressions or type checks) are isolated strictly to that specific field without disturbing the strict equality checks of other static fields (such as status codes, flag booleans, or specific response codes).
* **Cross-Platform Compatibility:** JSONPath is widely supported across contract testing tools (Spring Cloud Contract, Pact, WireMock, RestAssured, and MockMvc), ensuring consistent validation behavior across consumers and producers.

---

## Running the Tests

To compile the application and execute the contract verification test suites:

```bash
mvn test
```

All tests execute and verify:
1. Standard MVC controller endpoints at `/ms1/signin`.
2. Dynamic JWT token generation and uniqueness across requests.
3. Strict compliance of request and response with the contract YAML definition and dynamic regex matcher.
