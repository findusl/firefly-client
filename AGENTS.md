# AGENTS

This repository contains tools for exploring the Firefly III OpenAPI specification and making demo requests.

## openapi_helper.py
- `python openapi_helper.py` lists all endpoints in `firefly-iii-6.3.0-v1.yaml`.
- `python openapi_helper.py <path>` prints the full, `$ref`-resolved details for the specified endpoint.
- With `BASE_URL` and `ACCESS_TOKEN` set, `python openapi_helper.py <path> --request` performs a GET request against the demo API and prints the JSON response. Redirect output to a file to save it.

## Demo environment
The variables `BASE_URL` and `ACCESS_TOKEN` point to a Firefly III demo system. The system contains no valuable data and can be changed freely.

Example:
```bash
export BASE_URL="https://demo.firefly-iii.org"
export ACCESS_TOKEN="demo_token"
python openapi_helper.py /api/v1/accounts --request > accounts.json
```

The above command writes the list of accounts to `accounts.json`.
