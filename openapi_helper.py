#!/usr/bin/env python3
"""Simple helper to inspect the Firefly III OpenAPI specification.

Usage:
  python openapi_helper.py                # list all endpoints
  python openapi_helper.py <path>         # show details for a single endpoint
  python openapi_helper.py <path> --request  # perform GET request to the endpoint using BASE_URL and ACCESS_TOKEN
"""
import argparse
import os
import sys
import json
from pprint import pprint

import requests
from prance import ResolvingParser

SPEC_FILE = "firefly-iii-6.3.0-v1.yaml"


def load_spec():
    parser = ResolvingParser(SPEC_FILE)
    return parser.specification


def list_endpoints(spec):
    for path in spec.get("paths", {}):
        print(path)


def show_endpoint(spec, path):
    info = spec.get("paths", {}).get(path)
    if info is None:
        print(f"Endpoint {path} not found")
        return
    pprint(info)


def fetch_endpoint(path):
    base_url = os.environ.get("BASE_URL")
    token = os.environ.get("ACCESS_TOKEN")
    if not base_url or not token:
        raise RuntimeError("BASE_URL and ACCESS_TOKEN environment variables must be set")
    url = f"{base_url.rstrip('/')}{path}"
    headers = {"Authorization": f"Bearer {token}"}
    response = requests.get(url, headers=headers)
    response.raise_for_status()
    return response.json()


def main(argv):
    parser = argparse.ArgumentParser(description="Inspect Firefly III OpenAPI spec")
    parser.add_argument("path", nargs="?", help="Endpoint path to inspect")
    parser.add_argument("--request", action="store_true", help="Perform GET request to the given path using BASE_URL and ACCESS_TOKEN")
    args = parser.parse_args(argv)

    spec = load_spec()

    if args.path is None:
        list_endpoints(spec)
    else:
        show_endpoint(spec, args.path)
        if args.request:
            data = fetch_endpoint(args.path)
            print(json.dumps(data, indent=2))


if __name__ == "__main__":
    main(sys.argv[1:])
