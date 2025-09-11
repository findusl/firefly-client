# AGENTS

This repository contains tools for exploring the Firefly III OpenAPI specification and making demo requests.

## openapi_helper.py
- `python openapi_helper.py` lists all endpoints in `firefly-iii-6.3.0-v1.yaml`.
- `python openapi_helper.py <path>` prints the full, `$ref`-resolved details for the specified endpoint.
- With `BASE_URL` and `ACCESS_TOKEN` set, `python openapi_helper.py <path> --request` performs a request against the demo API. The helper prefixes the path with `/api` and consults the OpenAPI spec to choose `Accept` and `Content-Type` headers. Use `--accept` or `--content-type` to override the spec when multiple media types are available, and `--method` to select an HTTP method.
- The helper censors any occurrence of `BASE_URL` or `ACCESS_TOKEN` in its output so these values never appear in logs or files.

## Demo environment
The variables `BASE_URL` and `ACCESS_TOKEN` point to a Firefly III demo system. The system contains no valuable data and can be changed freely.

Example:
```bash
# Environment already provides BASE_URL and ACCESS_TOKEN
curl -X GET --location "$BASE_URL/api/v1/accounts" \
    -H "Accept: application/vnd.api+json" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -o accounts.json
```

The above command writes the list of accounts to `accounts.json`. You can also
use `python openapi_helper.py /v1/accounts --request` to fetch the same data.

## Kotlin Multiplatform development
- Share business logic in `commonMain` so Android, iOS, and future targets like WebAssembly reuse the same code.
- Prefer multiplatform libraries:
  - `kotlinx.datetime` for dates and times.
  - `kotlinx.io` (built on Okio) or other multiplatform file-access libraries for filesystem interactions.
  - `kotlinx.serialization` for structured I/O.
- Use the Ktor HTTP client for calling the Firefly API. Select the engine per platform (e.g. CIO for JVM/Android, Darwin for iOS, JS/Wasm for browser or WebAssembly) so a single client can support all targets.

## Testing
- Add tests for new features whenever possible.
- Use `kotlin.test` for unit tests in shared code.
- Use the Compose UI Test toolkit for UI testing.
- Use `kotlinx.coroutines.test` (e.g. `runTest`) for coroutine-based code.
