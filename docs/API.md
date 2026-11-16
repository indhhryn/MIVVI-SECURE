# MIVVI SECURE — API Integration

## Purpose

The optional Rules API distributes a versioned detector configuration to Android clients. The mobile app still performs all message parsing, URL checks, keyword matching, weighted scoring, and classification locally.

**No message body, sender identifier, phone number, notification payload, or analysis history is uploaded by this implementation.**

## Endpoints

### `GET /api/v1/health`

Response:

```json
{
  "status": "ok",
  "service": "mivvi-secure-rules-api",
  "version": "1.0.0"
}
```

### `GET /api/v1/rules`

Returns `server/config/rules.json`:

```json
{
  "version": "1.0.0",
  "weights": {
    "suspicious_keyword": 2,
    "url_outside_whitelist": 3,
    "shortlink": 2,
    "unknown_sender": 2,
    "urgency": 1,
    "sensitive_data": 3
  },
  "thresholds": {
    "suspicious_from": 3,
    "phishing_from": 6
  },
  "keywords": ["menang", "hadiah", "verifikasi akun"],
  "urgency_patterns": ["segera", "24 jam"],
  "sensitive_patterns": ["otp", "pin", "password"],
  "official_domains": ["bca.co.id", "pajak.go.id"],
  "shortlink_domains": ["bit.ly", "tinyurl.com"]
}
```

The actual response includes the full default pattern lists. The Android client caches the JSON in `SharedPreferences` and falls back to built-in rules if the API cannot be reached.

## Weight keys

| Key | Meaning | Default |
|---|---|---:|
| `suspicious_keyword` | One or more suspicious keyword patterns detected | 2 |
| `url_outside_whitelist` | At least one extracted URL host is outside configured whitelist | 3 |
| `shortlink` | At least one URL host matches configured shortlink domains | 2 |
| `unknown_sender` | Non-empty sender is not in user's local trusted-sender list | 2 |
| `urgency` | One or more urgency patterns detected | 1 |
| `sensitive_data` | One or more sensitive-data request patterns detected | 3 |

Each feature contributes its weight at most once per message. Multiple matches are shown in the explanation but do not multiply that feature's score.

## Update configuration

Edit `server/config/rules.json` and restart/reload FastAPI. Validate JSON before deploying. For public deployment, add authentication, authorization, HTTPS, rate limiting, change auditing, and a controlled domain-verification process; these controls are outside this prototype.

## Android configuration

In the app, enter the API base URL only, such as `http://192.168.1.10:8000`. The client appends `/api/v1/rules`. Android cleartext HTTP is enabled in this development prototype to support a private LAN API; production must use HTTPS and disable cleartext traffic.
