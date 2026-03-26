# SpeleoDB REST API Integration

## Base Configuration

| Parameter | Value | Source |
|-----------|-------|--------|
| Base URL | `https://www.speleodb.org` (default) | `PREFERENCES.DEFAULT_INSTANCE` |
| API prefix | `/api/v1` | `API.BASE_PATH` |
| Auth scheme | `Token <token>` header | `HEADERS.TOKEN_PREFIX` |
| Content-Type | `application/json` | `HEADERS.APPLICATION_JSON` |
| Connect timeout | 30 seconds | `NETWORK.CONNECT_TIMEOUT_SECONDS` |
| Request timeout | 60 seconds | `NETWORK.REQUEST_TIMEOUT_SECONDS` |
| Download timeout | 120 seconds | `NETWORK.DOWNLOAD_TIMEOUT_SECONDS` |

## URL Resolution

The plugin normalizes user-provided URLs through `resolveInstanceUrl()`:
1. Strip `http://` or `https://` prefix
2. Remove trailing slashes
3. Lowercase (RFC 7230 compliance)
4. Apply protocol: `http://` for local addresses (localhost, 127.x, 10.x, 172.16-31.x, 192.168.x), `https://` for everything else

HTTP version selection: HTTP/1.1 for `http://`, HTTP/2 for `https://`.

## Endpoints

### Authentication

**POST** `/api/v1/user/auth-token/`

Email/password login:
```json
{"email": "user@example.com", "password": "secret"}
```

OAuth token login: GET with `Authorization: Token <oauth_token>` header.

Response: `{"token": "<session_token>"}`

### Project Listing

**GET** `/api/v1/projects/`

Response: `{"data": [<project objects>]}`

Client-side filtering:
- Only `type == "ARIANE"` projects
- Exclude `permission == "WEB_VIEWER"` entries

### Project Creation

**POST** `/api/v1/projects/`

```json
{
  "name": "My Cave",
  "description": "Survey of entrance zone",
  "country": "FR",
  "latitude": "43.1234",
  "longitude": "1.5678",
  "type": "ARIANE"
}
```

Latitude and longitude are optional.

### Project Upload

**PUT** `/api/v1/projects/{id}/upload/ariane_tml/`

Multipart form data with fields:
- `message`: commit message (text)
- `artifact`: TML file (binary)

Responses:
- `200 OK`: upload successful
- `304 Not Modified`: no changes detected (raises `NotModifiedException`)

Pre-upload validation: SHA-256 hash compared against empty template to reject blank uploads.

### Project Download

**GET** `/api/v1/projects/{id}/download/ariane_tml/`

Responses:
- `200 OK`: binary TML file content
- `422 Unprocessable Entity`: project exists but has no content (empty template created locally)

### Lock Management

**POST** `/api/v1/projects/{id}/acquire/` -- acquire or refresh mutex lock

**POST** `/api/v1/projects/{id}/release/` -- release mutex lock

Both return `200 OK` on success.

### Announcements (Unauthenticated)

**GET** `/api/v1/announcements/`

Client-side filtering:
- `is_active == true`
- `software == "ARIANE"`
- `expiracy_date` not expired (or absent)
- `version` matches current plugin version (or absent)

### Plugin Releases (Unauthenticated)

**GET** `/api/v1/plugin_releases/`

Client-side filtering:
- `software == "ARIANE"`
- Current Ariane version within `[min_software_version, max_software_version]` bounds

## Error Handling

All API methods throw exceptions on failure. The controller catches and displays errors via `SpeleoDBModals.showError()` or `SpeleoDBTooltips.showError()`.

Error response parsing: when status is non-success, the response body is parsed for an `"error"` JSON field to provide a detailed message.
