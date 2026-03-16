# TaxCore Mobile POS — Project Specification

> **Purpose**: Platform-agnostic specification for reimplementing TaxCore Mobile POS in any language or framework.
> This document describes **what** the application does, not **how** it is currently built on Android.

---

## Table of Contents

1. [Product Overview](#1-product-overview)
2. [Glossary](#2-glossary)
3. [Architecture Overview](#3-architecture-overview)
4. [Data Models](#4-data-models)
5. [API Contracts](#5-api-contracts)
6. [Business Rules and Calculations](#6-business-rules-and-calculations)
7. [Feature Specifications](#7-feature-specifications)
8. [Security Requirements](#8-security-requirements)
9. [Configuration and Preferences](#9-configuration-and-preferences)
10. [Internationalization](#10-internationalization)
11. [Import / Export Formats](#11-import--export-formats)
12. [Error Codes and Messages](#12-error-codes-and-messages)
13. [UI/UX Requirements](#13-uiux-requirements)
14. [Non-Functional Requirements](#14-non-functional-requirements)

---

## 1. Product Overview

**TaxCore Mobile POS** is a point-of-sale application for small-to-medium businesses operating in tax-regulated markets. It lets users manage a product catalog, create and fiscalize invoices, and keep a searchable transaction journal — all while remaining compliant with the TaxCore fiscal system.

### 1.1 Supported Markets

| Market | Country Code | Currency | Currency Symbol |
|--------|-------------|----------|-----------------|
| Fiji | FJ | Fijian Dollar | FJ$ |
| Samoa | WS | Samoan Tālā | WS$ |
| Washington State, USA | US | US Dollar | US$ |
| Serbia | RS | Serbian Dinar | RSD |

### 1.2 Core Capabilities

| Capability | Description |
|-----------|-------------|
| **Catalog Management** | Create, edit, delete, search, import, and export products |
| **Invoice Creation** | Build invoices from catalog items, apply taxes, submit to fiscal server |
| **Fiscal Compliance** | Sign invoices via VSDC or ESDC, generate QR verification codes |
| **Transaction Journal** | Searchable, filterable history of all submitted invoices |
| **Cashier Management** | Create and select cashier/operator identities |
| **Multi-language** | English, French, Serbian, Bosnian |
| **Offline Awareness** | Read-only access to catalog and journal when offline; invoice submission requires network |

### 1.3 License

MIT

---

## 2. Glossary

| Term | Definition |
|------|-----------|
| **VSDC** | Virtual Secure Device Controller — a certificate-authenticated fiscal server operated by the tax authority |
| **ESDC** | Electronic Secure Device Controller — a PIN-authenticated fiscal device/server |
| **PAC** | Payment Authentication Code — a 6-character credential used for VSDC sessions |
| **PIN** | Personal Identification Number — a 4-digit credential used for ESDC sessions |
| **TIN** | Taxpayer Identification Number |
| **OID** | Object Identifier — a dot-separated number identifying certificate extensions |
| **PFX / P12** | PKCS#12 file containing a certificate and private key |
| **SDC** | Secure Device Controller — generic term for either VSDC or ESDC |
| **Fiscal Invoice** | An invoice signed by the fiscal authority, with a unique counter, QR code, and verification URL |
| **GTIN / EAN** | Global Trade Item Number / European Article Number — product barcode |

---

## 3. Architecture Overview

The application follows a layered architecture with clear separation of concerns.

```
┌──────────────────────────────────────────────────┐
│                   UI Layer                        │
│  Screens · Dialogs · Navigation · Input Handling  │
├──────────────────────────────────────────────────┤
│               Business Logic Layer                │
│  Invoice Calc · Validation · Workflow Orchestration│
├──────────────────────────────────────────────────┤
│                 Service Layer                      │
│  SDC Service · Download Service · Preference Svc  │
├──────────────────────────────────────────────────┤
│                 Data Layer                         │
│  Local DB · API Client · Certificate Store        │
└──────────────────────────────────────────────────┘
```

### 3.1 Key Architectural Patterns

- **Presenter / ViewModel**: UI delegates business logic to a presenter or view-model; the UI layer only renders state.
- **Singleton Managers**: `CatalogManager`, `JournalManager`, `TaxesManager`, `CertManager`, `InvoiceManager` are application-scoped singletons that encapsulate data access.
- **Callback / Async pattern**: All network operations use an async callback pattern: `onStart → (onSuccess | onError) → onEnd`.
- **Session State**: A lightweight in-memory session (`AppSession`) holds transient state like cached credentials and configuration flags.

---

## 4. Data Models

### 4.1 Catalog Item (persistent)

| Field | Type | Constraints | Description |
|-------|------|------------|-------------|
| `uuid` | string | **Primary key**, auto-generated UUID | Unique identifier |
| `name` | string | Required, max 1000 chars | Product name |
| `barcode` | string | Optional; if set: digits only, 8–16 chars, unique | EAN/GTIN barcode |
| `price` | float64 | ≥ 0.01, 2 decimal places | Unit price |
| `quantity` | float64 | Default 1.0 | Default quantity (used on invoice) |
| `tax` | list\<Tax\> | At least one required | Associated tax labels |
| `type` | string | Default `"Catalog"` | Record type marker |
| `isFavorite` | boolean | Default false | Favorite flag for quick access |
| `isSelected` | boolean | Default false | Transient selection state |

### 4.2 Tax

| Field | Type | Description |
|-------|------|-------------|
| `code` | string | Tax label code (e.g., `"Е"`, `"Г"`) |
| `name` | string | Tax category name (e.g., `"VAT"`) |
| `rate` | float64 | Tax rate as percentage (e.g., `17.0`) |
| `value` | string | Display indicator — `"%"` or a currency symbol |
| `isChecked` | boolean | Whether this tax is actively applied |

### 4.3 Tax Settings (persistent)

Same fields as Tax. Stored as the authoritative list of all available tax rates fetched from the server.

### 4.4 Cashier (persistent)

| Field | Type | Constraints | Description |
|-------|------|------------|-------------|
| `uuid` | string | **Primary key**, auto-generated UUID | Unique identifier |
| `id` | string | — | Business ID for the cashier |
| `name` | string | Required | Display name |
| `isChecked` | boolean | Default false | Whether this cashier is currently selected (only one at a time) |

### 4.5 Certificate (persistent)

| Field | Type | Description |
|-------|------|-------------|
| `uuid` | string | **Primary key**, auto-generated UUID |
| `uid` | string | Certificate UID from server provisioning |
| `name` | string | Original filename (e.g., `"taxpayer.nochain.p12"`) |
| `pfxData` | string | Base64-encoded PKCS#12 certificate bytes |

### 4.6 Journal Entry (persistent)

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Entry ID |
| `date` | string | ISO 8601 date-time of the transaction |
| `invoiceNumber` | string | Fiscal invoice number assigned by server |
| `total` | float64 | Invoice total amount |
| `rec` | integer | Record counter |
| `message` | string | Server response message |
| `qrCode` | string | Base64-encoded QR code image data |
| `VerificationUrl` | string | URL for invoice verification |
| `SignedBy` | string? | Signing authority identifier |
| `RequestedBy` | string? | Requesting party |
| `ID` | string? | Encrypted internal data |
| `S` | string? | Digital signature |
| `IC` | string? | Invoice counter |
| `InvoiceCounterExtension` | string? | Counter extension |
| `TotalCounter` | integer | Running total counter |
| `TransactionTypeCounter` | integer? | Per-transaction-type counter |
| `TaxGroupRevision` | integer? | Tax group version number |
| `buyerTin` | string | Buyer TIN (searchable) |
| `buyerCostCenter` | string | Buyer cost center |
| `transactionType` | string | `"sale"` or `"refund"` |
| `paymentType` | string | Payment method code |
| `invoiceType` | string | `"normal"`, `"proforma"`, `"copy"`, or `"training"` |
| `invoiceItemsData` | string | JSON-serialized array of line items |
| `type` | string | Record type marker, default `"Journal"` |

### 4.7 Certificate Data (derived, in-memory)

Extracted from an X.509 certificate:

| Field | Source | Description |
|-------|--------|-------------|
| `subject` | Certificate subject DN | Full distinguished name |
| `commonName` | `CN=` from subject | Common name |
| `serialNumber` | `SERIALNUMBER=` from subject | Certificate serial |
| `organisationUnit` | `OU=` from subject | Organization unit |
| `organisationName` | `O=` from subject | Organization name |
| `tinOid` | OID extension `*.*.6` | Taxpayer ID extracted from certificate |
| `countryName` | Derived from TIN OID | Country name mapped from OID country code |
| `vsdcEndpoint` | OID extension `*.*.7` | VSDC server URL embedded in certificate |

### 4.8 Environment Data (derived, cached)

| Field | Description |
|-------|-------------|
| `uid` | Environment unique identifier |
| `name` | Environment name (VSDC) |
| `esdcEnvName` | Environment name (ESDC) |
| `esdcEndpoint` | ESDC base endpoint |
| `esdcApiEndpoint` | ESDC TaxCore API endpoint |
| `apiEndpoint` | VSDC TaxCore API endpoint |
| `country` | Country code |
| `logo` | Logo URL or base64 data |

---

## 5. API Contracts

The application communicates with two types of fiscal servers.

### 5.1 VSDC (Virtual SDC) Endpoints

Base URL: extracted from the X.509 certificate OID extension (`*.*.7`).

Authentication: mutual TLS with client certificate + PAC passed as HTTP header (`PAC: <value>`).

| Method | Path | Auth | Request Body | Response Body | Description |
|--------|------|------|-------------|--------------|-------------|
| POST | `api/v3/invoices` | mTLS + `PAC` header | `InvoiceRequest` | `InvoiceResponse` | Submit fiscal invoice |
| GET | `api/v3/status` | mTLS + `PAC` header | — | `StatusResponse` | Fetch tax rates and status |
| GET | `api/v3/environment-parameters` | mTLS + `PAC` header | — | `EnvResponse` | Fetch environment configuration |

### 5.2 ESDC (Electronic SDC) Endpoints

Base URL: user-configured IP address and port.

Authentication: PIN code verified separately.

| Method | Path | Auth | Request Body | Response Body | Description |
|--------|------|------|-------------|--------------|-------------|
| POST | `api/v3/invoices` | None / PIN | `InvoiceRequest` | `InvoiceResponse` | Submit fiscal invoice |
| GET | `api/v3/status` | None | — | `StatusResponse` | Fetch tax rates and status |
| GET | `api/v3/environment-parameters` | None | — | `EnvResponse` | Fetch environment configuration |
| GET | `api/v3/attention` | None | — | — (HTTP 200) | Health check / ping |
| POST | `api/v3/pin` | None | PIN string (body) | Status code string | Verify PIN code |

### 5.3 Invoice Request Schema

```json
{
  "cashier": "string | null",
  "buyerId": "string | null",
  "buyerCostCenterId": "string | null",
  "invoiceType": "normal | proforma | copy | training",
  "transactionType": "sale | refund",
  "paymentType": "cash | card | check | wiretransfer | voucher | mobilemoney | other",
  "payment": [
    {
      "amount": 0.00,
      "paymentType": "cash | card | ..."
    }
  ],
  "invoiceNumber": "string | null",
  "referentDocumentNumber": "string | null",
  "referentDocumentDT": "string | null (ISO 8601)",
  "items": [
    {
      "name": "string",
      "gtin": "string | null",
      "quantity": 0.000,
      "labels": ["string"],
      "unitPrice": 0.00,
      "totalAmount": 0.00
    }
  ],
  "hash": "string (MD5 of serialized request without this field)"
}
```

### 5.4 Invoice Response Schema

```json
{
  "requestedBy": "string | null",
  "sdcDateTime": "string | null (ISO 8601)",
  "invoiceNumber": "string | null",
  "journal": "string | null (monospace receipt text)",
  "messages": "string | null",
  "verificationQRCode": "string | null (Base64 image)",
  "verificationUrl": "string (URL)",
  "invoiceCounter": "string | null",
  "invoiceCounterExtension": "string | null",
  "totalCounter": 0,
  "transactionTypeCounter": "integer | null",
  "totalAmount": 0.00,
  "taxGroupRevision": "integer | null",
  "taxItems": [
    {
      "label": "string",
      "categoryType": "string | null",
      "rate": 0.0,
      "amount": 0.0
    }
  ],
  "items": [
    {
      "itemId": "string | null",
      "invoiceId": "string | null",
      "barcode": "string | null",
      "name": "string | null",
      "quantity": 0.0,
      "unitPrice": 0.0,
      "totalAmount": 0.0,
      "taxLabels": ["string"]
    }
  ],
  "encryptedInternalData": "string | null",
  "signature": "string | null",
  "signedBy": "string | null",
  "businessName": "string",
  "tin": "string",
  "locationName": "string",
  "address": "string",
  "district": "string",
  "mrc": "string"
}
```

### 5.5 Status Response Schema

```json
{
  "sdcDateTime": "string",
  "supportedLanguages": ["string"],
  "uid": "string",
  "taxCoreApi": "string (URL)",
  "currentTaxRates": {
    "validFrom": "string (ISO 8601)",
    "groupId": 0,
    "taxCategories": [
      {
        "name": "string",
        "categoryType": "string",
        "orderId": 0,
        "taxRates": [
          {
            "rate": 0.0,
            "label": "string"
          }
        ]
      }
    ]
  },
  "allTaxRates": ["(same structure as currentTaxRates, array of all historical rates)"],
  "gsc": ["string (status/error codes — codes starting with '2' indicate errors)"]
}
```

**Tax label mapping**: When `categoryType` equals the amount-per-quantity category type, the tax `value` display is the currency symbol; otherwise it is `"%"`.

### 5.6 Environment Response Schema

```json
{
  "organizationName": "string",
  "serverTimeZone": "string",
  "street": "string",
  "city": "string",
  "country": "string",
  "environmentName": "string",
  "logo": "string (URL or Base64)",
  "ntpServer": "string",
  "supportedLanguages": ["string"],
  "endpoints": {
    "taxpayerAdminPortal": "string (URL)",
    "taxCoreApi": "string (URL)",
    "vsdc": "string (URL)",
    "root": "string (URL)"
  }
}
```

### 5.7 Error Response Schema

```json
{
  "Message": "string | null",
  "MessageDetails": "string | null"
}
```

> Note: Field names use PascalCase (`Message`, `MessageDetails`) as returned by the server.

### 5.8 JSON Serialization Convention

- All request/response models use **camelCase** field names by default (e.g., `invoiceType`, `totalAmount`).
- Exception: `ErrorResponse` uses **PascalCase** (`Message`, `MessageDetails`) — mapped via explicit annotations.
- JSON serialization must include `null` values (`serializeNulls` mode) for hash computation.

### 5.9 File Download Endpoint

Used for certificate provisioning.

| Method | Path | Response | Description |
|--------|------|----------|-------------|
| GET | `{provisioning-url}/` | `application/zip` | Download ZIP containing `.nochain.p12` certificate |

---

## 6. Business Rules and Calculations

### 6.1 Invoice Total Calculation

```
invoiceTotal = Σ (item.quantity × item.unitPrice)   for all items
```

- All amounts rounded to **2 decimal places** using **half-up** rounding.
- Quantity supports up to **3 decimal places** (minimum `0.001`).
- Tax calculations are performed **server-side only** — the client sends item tax labels, not computed tax amounts.

### 6.2 Invoice Request Hash

Before submitting an invoice, the client computes an integrity hash:

1. Set the `hash` field of the `InvoiceRequest` to `null`.
2. Serialize the `InvoiceRequest` to JSON with **null values included** (serialize-nulls mode).
3. Compute the **MD5** hash of the UTF-8 bytes of the JSON string.
4. Convert the digest to a lowercase hex string (e.g., `"%02x"` per byte).
5. Assign the hex string to the `hash` field.
6. Send the complete request (with hash populated).

### 6.3 Invoice Types

| Type | Value | Description |
|------|-------|-------------|
| Normal | `"normal"` | Standard fiscal invoice |
| Proforma | `"proforma"` | Draft invoice — not fiscally registered |
| Copy | `"copy"` | Copy of a previously submitted invoice |
| Training | `"training"` | Test/training invoice |

### 6.4 Transaction Types

| Type | Value | Description |
|------|-------|-------------|
| Sale | `"sale"` | Standard sale transaction |
| Refund | `"refund"` | Refund/return transaction — requires a referent document |

### 6.5 Payment Types

| Type | API Value | Description |
|------|-----------|-------------|
| Cash | `"cash"` | Cash payment |
| Card | `"card"` | Debit or credit card |
| Check | `"check"` | Paper check |
| Wire Transfer | `"wiretransfer"` | Electronic bank transfer |
| Voucher | `"voucher"` | Voucher or gift card |
| Mobile Money | `"mobilemoney"` | Mobile payment |
| Other | `"other"` | Other payment method |

### 6.6 Referent Document Validation

Required when invoice type is `"copy"` or transaction type is `"refund"`:

- **Referent Document Number**: Must match pattern `[0-9a-zA-Z]{8}-[0-9a-zA-Z]{8}-[0-9]+`
- **Referent Document DateTime**: ISO 8601 format `yyyy-MM-dd'T'HH:mm:ssZZ`; displayed as `yyyy-MM-dd HH:mm:ss`

### 6.7 Buyer Fields

- **Buyer TIN**: Optional free-text field.
- **Buyer Cost Center**: Only enabled when Buyer TIN is non-empty.

### 6.8 Catalog Item Validation

| Field | Rule |
|-------|------|
| Name | Required, non-blank, < 1000 characters |
| Price | ≥ 0.01 |
| Barcode | Optional; if provided: digits only, 8–16 characters, unique across catalog |
| Tax Labels | At least one tax must be assigned |

### 6.9 Credential Caching

- **PAC** (VSDC) and **PIN** (ESDC) are cached in-memory after verification.
- Cache expires after **15 minutes** (credential staleness check based on timestamp).
- On expiry, the user is re-prompted for the credential.

### 6.10 Currency Determination

- **VSDC mode**: Currency determined by the TIN OID country code from the certificate.
- **ESDC mode**: Currency determined by the country code from the environment response.

### 6.11 Catalog Search Logic

When searching by a pattern string:

- If the pattern is **all digits** → search by barcode (contains, case-insensitive).
- Otherwise → search by item name (contains, case-insensitive).

---

## 7. Feature Specifications

### 7.1 Application Startup

```
App Launch
  → Load secure preferences (with corruption recovery)
  → Set session flags:
      isAppConfigured, shouldAskForConfiguration,
      hasCertInstalled, useESDCServer, useVSDCServer
  → Check network connectivity
      → If offline AND not configured: show "setup requires internet" message
      → If offline AND configured: show "working offline" message
      → If online AND configured: fetch latest tax rates from server
  → Navigate to Dashboard
```

**Corrupted Preference Recovery**: If the encrypted preference store is unreadable, the app deletes the corrupted file, regenerates the encryption key, and marks the app as not configured.

### 7.2 Dashboard

The main navigation hub. Displays configuration status and provides navigation to all features.

**Header States**:

| State | Display |
|-------|---------|
| Not configured | Blue overlay: "App Not Configured" with a "Configure Now" button |
| VSDC configured | Organization Unit + Certificate Serial Number |
| ESDC configured | Environment Name + UID |

**Navigation Buttons**:

| Button | Enabled When | Destination |
|--------|-------------|-------------|
| New Invoice | App is configured | Invoice creation screen |
| Catalog | App is configured | Catalog management |
| Journal | App is configured AND journal has entries | Transaction journal |
| Settings | Always | Settings menu |

**Exit Behavior**: Double-press back within 3 seconds to exit. Session credentials are cleared on exit.

### 7.3 Invoice Creation

#### 7.3.1 Default State

- Invoice type: Normal
- Transaction type: Sale
- Payment type: Cash
- Items list: empty
- Total: 0.00

#### 7.3.2 Adding Items

Three methods:

1. **From catalog**: Open item picker → select items → add to invoice.
2. **Create new item**: Open inline item form → enter name, price, quantity, taxes, optional barcode → add to invoice and catalog.
3. **Barcode scan**: Scan barcode → if found in catalog, add to invoice; if not found, redirect to item creation form.

**Item picker search**: Supports real-time filtering by name or barcode within the picker dialog (see §6.11).

#### 7.3.3 Editing Invoice Items

- Tap an item to edit quantity or price.
- Quantity: 3 decimal places, minimum 0.001.
- Price: 2 decimal places.
- Total updates in real-time.

#### 7.3.4 Favorite Items

- Items marked as favorite appear in a quick-access section on the invoice screen.
- Tapping a favorite item adds it to the invoice.
- Favorites are hidden during Copy and Refund flows.

#### 7.3.5 Invoice Submission Flow

```
User taps "Sign Invoice"
  → Validate: at least 1 item present
  → Validate: all required fields populated
  → If Copy/Refund: validate referent document fields
  → Determine server type (VSDC or ESDC)

  VSDC Flow:
    → Check cached PAC (valid within 15 minutes?)
    → If expired: show PAC input dialog (6 characters, supports clipboard paste)
    → Build InvoiceRequest with MD5 hash (see §6.2)
    → POST to VSDC endpoint with mutual TLS + PAC HTTP header
    → On success: show fiscal dialog → save to journal → reset invoice

  ESDC Flow:
    → Check cached PIN (valid within 15 minutes?)
    → If expired: show PIN input dialog (4 digits, supports clipboard paste)
    → Verify PIN via `POST api/v3/pin` endpoint
    → If PIN response status ≠ `"0100"`: show error, return to dialog
    → Build InvoiceRequest
    → POST to ESDC endpoint
    → On success: show fiscal dialog → save to journal → reset invoice
```

#### 7.3.6 Fiscal Dialog (Post-submission)

Displays:
- Receipt text (monospace font)
- QR code (decoded from Base64 response)

Actions:
- **Print**: Generate PDF, send to system print service
- **Share**: Export PDF, share via system share dialog
- **Close**: Dismiss dialog

#### 7.3.7 Copy Invoice Mode

- Triggered from journal entry → "Copy" action.
- Pre-populates all fields from the original journal entry.
- Locks: referent document number, date-time, invoice type, transaction type, payment type.
- Hides: add item, create item, scan, reset, favorite items.
- Items are read-only.

#### 7.3.8 Refund Invoice Mode

- Triggered from journal entry → "Refund" action.
- Pre-populates items from the original journal entry.
- Sets referent document number to original invoice number.
- Transaction type forced to `"refund"` (not editable).
- Hides favorite items and reset button.

### 7.4 Catalog Management

#### 7.4.1 Browse and Search

- List all items with lazy-loading.
- Real-time search by name or barcode (see §6.11).

#### 7.4.2 Advanced Filtering

Filter by any combination of (AND logic):

| Filter | Type | Matching |
|--------|------|---------|
| Item name | Text | Contains, case-insensitive |
| Unit price | Number | Exact match (auto-rounded to 2 decimals) |
| Barcode | Text | Contains, case-insensitive |
| Tax labels | Multi-select | Item has any of selected tax codes |

#### 7.4.3 Item CRUD

- **Create**: All fields per §6.8 validation.
- **Read**: Tap item to view details.
- **Update**: Edit any field; save updates.
- **Delete**: Delete from detail view.

#### 7.4.4 Favorite Toggle

Toggle the `isFavorite` flag on any item.

#### 7.4.5 Import Catalog

Supports CSV and JSON formats (see §11 for format details).

Flow:
1. User selects file.
2. If catalog is non-empty, show confirmation dialog ("This will replace existing catalog").
3. Parse and validate file.
4. On validation error: show error message with details.
5. On success: replace entire catalog, update UI.

#### 7.4.6 Export Catalog

Supports CSV and JSON formats.

Flow:
1. User selects format.
2. User enters filename.
3. User picks save location.
4. Stream-write to file.
5. Show success message.

Disabled when: app is not configured OR catalog is empty.

### 7.5 Transaction Journal

#### 7.5.1 Journal List

- Display all transactions in reverse chronological order (newest first).
- Each entry shows: invoice number, date, total, transaction type.
- Tap to view details.
- Supports lazy loading for large datasets to prevent memory exhaustion.

#### 7.5.2 Journal Detail

Displays all fields from the journal entry including:
- Full invoice data and metadata
- Line-by-line items (name, quantity, price, taxes)
- Receipt text (monospace)
- QR code
- Clickable verification URL

#### 7.5.3 Journal Filtering

All filters combine with AND logic:

| Filter | Type | Matching |
|--------|------|---------|
| Invoice number | Text | Contains, case-insensitive |
| Buyer TIN | Text | Contains, case-insensitive |
| Invoice type | Dropdown | Exact match |
| Transaction type | Dropdown | Exact match |
| Date range | Date picker (from/to) | Inclusive range |

Results sorted by date (descending).

#### 7.5.4 Copy / Refund from Journal

- **Copy**: Opens invoice creation in Copy mode (see §7.3.7).
- **Refund**: Opens invoice creation in Refund mode (see §7.3.8).

#### 7.5.5 Journal Export

- Format: JSON array of journal objects.
- Stream-write implementation (handles large datasets without memory exhaustion).
- Show progress every 100 items.

#### 7.5.6 Journal Import

- Format: JSON array matching the export format.
- Validates first entry has `type == "Journal"` and non-empty `id`.
- Deduplicates by `invoiceNumber` — skips entries already in the database.
- Saves in batches of 100 with progress callback.

#### 7.5.7 Export and Clear Journal (Hidden Feature)

- Triggered by 5-second long press on the export button.
- Shows warning: "This will clear all journal entries after export."
- Exports all entries to file, then deletes all journal records from database.

### 7.6 Settings

#### 7.6.1 Server Configuration

**Two mutually exclusive server modes:**

**VSDC (Certificate-based)**:
1. User enters or follows a provisioning URL.
2. System downloads ZIP file from the URL.
3. Extracts `.nochain.p12` certificate file from ZIP.
4. Base64-encodes and stores certificate in local database.
5. User enters certificate password (8 characters).
6. User enters PAC (6 characters).
7. System loads and validates certificate.
8. System extracts VSDC endpoint, TIN OID, country from certificate extensions.
9. System fetches tax rates and environment parameters from VSDC.
10. App is marked as configured.

**ESDC (URL-based)**:
1. User enters IP address (4 octets), port, and protocol (HTTP/HTTPS).
2. System pings the server via `attention` endpoint.
3. System fetches status and environment parameters.
4. Tax labels extracted from status response.
5. App is marked as configured.

**Server Switch Warning**: If already configured, switching server type resets the entire configuration (clears certificate, PAC, environment data).

#### 7.6.2 SDC Details (Read-only)

| VSDC | ESDC |
|------|------|
| Organization Unit | Server URL |
| Certificate Serial Number | Environment Name |
| Active Certificate Name | UID |
| — | API URL |

#### 7.6.3 Cashier Management

- **Add**: Name + optional PIN (8 characters).
- **List**: Show all cashiers with selection indicator.
- **Select**: Only one cashier selected at a time (radio-button behavior).
- **Delete**: Remove cashier.
- Enabled only when app is configured.

#### 7.6.4 Tax Configuration

- View all available tax rates.
- Tax rates are fetched from the server during configuration and on startup.
- Display: code, name, rate, type indicator (`%` or currency symbol).
- Enabled only when app is configured.

#### 7.6.5 Language Selection

- Options: English (default), French, Serbian, Bosnian.
- Changing language requires app restart.
- Always available regardless of configuration state.

#### 7.6.6 About

Displays: app version, build number, license info, credits.

---

## 8. Security Requirements

### 8.1 Encrypted Preference Storage

All sensitive configuration data must be encrypted at rest:

| Data | Encrypted |
|------|-----------|
| Certificate TIN OID | Yes |
| Certificate subject | Yes |
| Certificate country | Yes |
| VSDC endpoint URL | Yes |
| ESDC endpoint URL | Yes |
| PAC codes | Yes |
| PFX passwords | Yes |
| Environment URLs | Yes |
| Environment UID | Yes |
| App locale | No |
| Configuration flag | No |

Encryption: AES-256 with separate key/value encryption schemes (AES-256-SIV for keys, AES-256-GCM for values).

### 8.2 Certificate Handling

- Certificates are stored as Base64-encoded PFX data in the local database.
- PFX passwords are stored encrypted in preferences (keyed by filename).
- Certificate provisioning links are single-use (second download attempt returns non-ZIP content type).
- Custom SSL/TLS with mutual authentication for VSDC communication.
- Certificate extensions use OID prefix `1.3.6.1.4.1.49952` with the structure:

```
1.3.6.1.4.1.49952.X.Y.Z
  X = Environment: 1=Internal, 2=Staging, 3=Production, 4=Pilot, 5=Sandbox
  Y = Country: 2=Fiji, 5=WA US, 6=Samoa, 8=Serbia
  Z = Data: 5=Backend API URL, 6=TIN, 7=VSDC URL
```

### 8.3 Credential Lifecycle

- PAC/PIN cached in memory only — never persisted to disk.
- Cache timeout: 15 minutes.
- Cleared on: app exit, session timeout, server switch, or configuration reset.

### 8.4 Data Backup

- Application data backup must be disabled to prevent sensitive data exposure.

---

## 9. Configuration and Preferences

### 9.1 Preference Keys Reference

| Key | Encrypted | Type | Description |
|-----|-----------|------|-------------|
| `IS_APP_CONFIGURED` | No | boolean | Whether initial setup is complete |
| `APP_LOCALE` | No | string | Language locale tag (e.g., `"en"`, `"fr"`) |
| `LAST_CREDENTIALS_TIME` | No | long | Timestamp of last credential entry |
| `USE_VSDC_SERVER` | No | boolean | VSDC mode enabled |
| `USE_ESDC_SERVER` | No | boolean | ESDC mode enabled |
| `CASHIER_NAME` | No | string | Active cashier name |
| `CERT_TIN_OID` | Yes | string | TIN OID from certificate |
| `CERT_ALIAS_VALUE` | Yes | string | Active certificate filename |
| `CERT_SUBJECT` | Yes | string | Certificate subject DN |
| `CERT_COUNTRY` | Yes | string | Country from certificate |
| `CERT_CERT_PAC` | Yes | string | Per-certificate PAC |
| `CERT_GLOBAL_PAC` | Yes | string | Global PAC |
| `VSDC_ENDPOINT_URL` | Yes | string | VSDC server URL |
| `ESDC_ENDPOINT_URL` | Yes | string | ESDC server URL |
| `ENV_LOGO_URL` | Yes | string | Environment logo URL |
| `ENV_COUNTRY` | Yes | string | Environment country code |
| `ENV_NAME` | Yes | string | VSDC environment name |
| `ENV_UID` | Yes | string | Environment unique ID |
| `ENV_API_ADDRESS` | Yes | string | TaxCore API URL |
| `ENV_ESDC_API_URL` | Yes | string | ESDC-specific API URL |
| `ENV_ESDC_NAME` | Yes | string | ESDC environment name |

### 9.2 Session State (In-Memory)

| Variable | Type | Description |
|----------|------|-------------|
| `isAppConfigured` | boolean | Mirrors preference |
| `shouldAskForConfiguration` | boolean | Show setup prompt on dashboard |
| `pacCode` | string | Cached VSDC PAC |
| `pinCode` | string | Cached ESDC PIN |

---

## 10. Internationalization

### 10.1 Supported Locales

| Language | Locale Tag | Resource Qualifier |
|----------|-----------|-------------------|
| English | `en` | Default |
| French | `fr` | `values-fr` |
| Bosnian | `bs` | `values-bs` |
| Serbian | `sr` | `values-sr` |

### 10.2 Locale Behavior

- Language preference is persisted.
- Changing locale requires a full app restart.
- All user-facing strings must be externalized into locale-specific resource files.
- Number formatting (decimal separator) follows the selected locale.

---

## 11. Import / Export Formats

### 11.1 Catalog CSV Format

**Delimiter**: Comma (`,`)

**Header row**: `EAN,Name,UnitPrice,TaxLabels,isFavorite`

**Example**:
```csv
EAN,Name,UnitPrice,TaxLabels,isFavorite
1234567890123,Widget,10.50,"VAT17,VAT5",true
,Simple Item,5.00,"VAT17",false
```

**Field rules**:
- EAN: optional (may be empty); if present, digits only, 8–16 characters.
- Name: required, non-blank, < 1000 characters.
- UnitPrice: required, ≥ 0.01.
- TaxLabels: comma-separated within quotes, at least one required.
- isFavorite: `true` or `false`.

### 11.2 Catalog JSON Format

```json
[
  {
    "uuid": "unique-id",
    "barcode": "1234567890123",
    "name": "Widget",
    "price": 10.50,
    "tax": [
      { "code": "VAT17" }
    ],
    "isFavorite": true,
    "type": "Catalog"
  }
]
```

**Validation**: First item must have `type == "Catalog"`.

### 11.3 Journal JSON Format

```json
[
  {
    "id": "uuid",
    "type": "Journal",
    "date": "2025-01-15T10:30:00+00:00",
    "invoiceNumber": "ABC12345-XYZ98765-1",
    "total": 150.00,
    "rec": 1,
    "message": "...",
    "qrCode": "base64...",
    "VerificationUrl": "https://...",
    "SignedBy": "...",
    "RequestedBy": "...",
    "ID": "...",
    "S": "...",
    "IC": "...",
    "InvoiceCounterExtension": "...",
    "TotalCounter": 42,
    "TransactionTypeCounter": 10,
    "TaxGroupRevision": 1,
    "buyerTin": "",
    "buyerCostCenter": "",
    "transactionType": "sale",
    "paymentType": "cash",
    "invoiceType": "normal",
    "invoiceItemsData": "[{\"name\":\"Widget\",\"quantity\":2.0,\"unitPrice\":75.00}]"
  }
]
```

**Validation**: First item must have `type == "Journal"` and non-empty `id`.

**Deduplication**: Entries with an `invoiceNumber` already in the database are skipped.

**Batch size**: Import saves in batches of 100, reporting progress after each batch.

### 11.4 PDF Receipt Format

- Font: Monospaced (e.g., Consolas or Courier equivalent).
- Content: Server-returned receipt text split by delimiter `========================================\r\n`.
- Sections: Header (first 5 lines), body (remaining lines), QR code.
- QR code: Scaled to 163×163 pixels.
- Page dimensions: 174 points wide, height calculated dynamically as `163 + (row_count × 7.7)` points.
- Margins: 5pt left/right, 0pt top/bottom.

---

## 12. Error Codes and Messages

### 12.1 Server Error Codes

Error codes returned in the `gsc` field of `StatusResponse` (codes starting with `"2"` indicate errors):

| Code | Description |
|------|-------------|
| `2100` | Invalid PIN code |
| `2210` | Secure Element locked |
| `2220` | SE connection failed |
| `2230` | Unsupported protocol version |
| `2310` | Tax labels not defined |
| `2400` | Device not configured |
| `2801` | Field too short |
| `2802` | Field too long |
| `2803` | Field length out of range |
| `2804` | Value out of range |
| `2805` | Invalid value |
| `2806` | Invalid data format |
| `2807` | List too small |
| `2808` | List too large |

### 12.2 Catalog Errors

| Error | Condition |
|-------|-----------|
| `WRONG_CSV_TYPE` | CSV file does not match expected format |
| `WRONG_JSON_TYPE` | JSON file first item `type` ≠ `"Catalog"` |
| `FILE_NOT_FOUND` | Selected file does not exist |
| `WRONG_DATA_TYPE` | Field type mismatch (e.g., non-numeric price) |
| `INVALID_FILE_CONTENT` | File structure is invalid |
| `UNABLE_TO_COMPLETE` | General import failure |
| `NOT_ENOUGH_SPACE` | Insufficient storage for export |
| `EXPORT_FILE_NOT_FOUND` | Export destination inaccessible |
| `UNABLE_TO_EXPORT` | General export failure |

### 12.3 Journal Errors

| Error | Condition |
|-------|-----------|
| `WRONG_JSON_TYPE` | First item `type` ≠ `"Journal"` |
| `INVALID_JSON_FORMAT` | JSON syntax error |
| `FILE_NOT_FOUND` | Selected file does not exist |
| `UNABLE_TO_IMPORT` | General import failure |

### 12.4 Certificate Download Errors

| Error | Condition |
|-------|-----------|
| `REQUEST_INVALID` | Could not construct download request |
| `REQUEST_FAILED` | Network error during download |
| `RESPONSE_INVALID` | HTTP error response |
| `NO_CERT_FILE_FOUND` | ZIP does not contain `.nochain.p12` file |
| `INVALID_OR_USED_LINK` | Content-Type is not `application/zip` (link may be expired/used) |

---

## 13. UI/UX Requirements

### 13.1 Screen Map

```
Splash Screen
  └─→ Dashboard
        ├─→ New Invoice
        │     ├─→ Item Picker (bottom sheet dialog)
        │     ├─→ Barcode Scanner
        │     ├─→ Item Creation Form
        │     └─→ Fiscal Receipt Dialog
        ├─→ Catalog
        │     ├─→ Item List (with search)
        │     ├─→ Filter Panel
        │     ├─→ Item Detail / Edit
        │     └─→ Import / Export dialogs
        ├─→ Journal
        │     ├─→ Transaction List (with search)
        │     ├─→ Filter Panel
        │     ├─→ Transaction Detail
        │     └─→ Import / Export dialogs
        └─→ Settings
              ├─→ Server Configuration (VSDC / ESDC)
              ├─→ SDC Details (read-only)
              ├─→ Cashier Management
              ├─→ Tax Configuration
              ├─→ Language Selection
              └─→ About
```

### 13.2 Required Permissions

| Permission | Purpose | Required |
|-----------|---------|----------|
| Internet | API communication, certificate download | Yes |
| Network State | Connectivity detection | Yes |
| Camera | Barcode scanning | Optional |
| File Read/Write | Catalog/journal import/export | Yes |

### 13.3 Common UI Patterns

- **Confirmation dialogs** before destructive operations (delete, replace catalog, clear journal).
- **Progress dialogs** during import/export operations showing `current/total` progress.
- **Toast messages** for success/error notifications.
- **Pull-to-refresh** is not used; explicit refresh actions where needed.
- **Lazy loading / pagination** for large lists (journal, catalog).
- **Monospaced font** for receipt display.

### 13.4 Input Constraints

| Field | Constraint |
|-------|-----------|
| PAC | Exactly 6 characters |
| PIN | Exactly 4 digits |
| Certificate password | Exactly 8 characters |
| Quantity | 3 decimal places, minimum 0.001 |
| Price | 2 decimal places, minimum 0.01 |
| IP Address | 4 octets, each 0–255 |
| Barcode | Digits only, 8–16 characters |
| Item name | Non-blank, < 1000 characters |
| Referent document number | Pattern: `[0-9a-zA-Z]{8}-[0-9a-zA-Z]{8}-[0-9]+` |

---

## 14. Non-Functional Requirements

### 14.1 Performance

- Journal and catalog must handle **10,000+ records** without memory exhaustion.
- Import/export operations must use **streaming** (not load-all-into-memory) for large datasets.
- Import batch size: **100 records** per database transaction.

### 14.2 Reliability

- Graceful handling of corrupted preference stores with automatic recovery.
- Network timeout handling with user-friendly error messages.
- Offline mode: read-only access to local data; invoice submission queued or blocked.

### 14.3 Data Integrity

- MD5 hash on invoice requests to detect tampering.
- Deduplication on journal import (by invoice number).
- Certificate chain validation for VSDC communication.

### 14.4 Storage

- Check available disk space before export operations.
- Clean up temporary files (downloaded ZIPs, extracted directories) after certificate import.

---

*End of specification.*
