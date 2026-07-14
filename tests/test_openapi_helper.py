import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import requests

import openapi_helper


class CensorTest(unittest.TestCase):
    def test_censors_text(self):
        value = "https://demo.example/api uses secret-token"

        result = openapi_helper._censor(
            value,
            "https://demo.example",
            "secret-token",
        )

        self.assertEqual("<BASE_URL>/api uses <ACCESS_TOKEN>", result)

    def test_censors_bytes(self):
        value = b"https://demo.example/api uses secret-token"

        result = openapi_helper._censor(
            value,
            "https://demo.example",
            "secret-token",
        )

        self.assertEqual(b"<BASE_URL>/api uses <ACCESS_TOKEN>", result)


class SpecTest(unittest.TestCase):
    def test_loads_spec_outside_repository_directory(self):
        original_directory = Path.cwd()
        try:
            with tempfile.TemporaryDirectory() as temporary_directory:
                os.chdir(temporary_directory)
                spec = openapi_helper.load_spec()
        finally:
            os.chdir(original_directory)

        operation = spec["paths"]["/v1/accounts"]["get"]
        self.assertEqual("listAccount", operation["operationId"])


class RequestTest(unittest.TestCase):
    def test_censors_request_failures(self):
        base_url = "https://demo.example"
        token = "secret-token"
        error = requests.ConnectionError(
            f"Could not reach {base_url}/api/v1/accounts using {token}"
        )
        spec = {
            "paths": {
                "/v1/accounts": {
                    "get": {
                        "responses": {
                            "200": {
                                "content": {"application/vnd.api+json": {}},
                            }
                        }
                    }
                }
            }
        }

        with (
            patch.dict(
                os.environ,
                {"BASE_URL": base_url, "ACCESS_TOKEN": token},
                clear=False,
            ),
            patch(
                "openapi_helper.requests.request",
                side_effect=error,
            ) as request,
        ):
            with self.assertRaises(RuntimeError) as raised:
                openapi_helper.fetch_endpoint(spec, "/v1/accounts")

        message = str(raised.exception)
        self.assertNotIn(base_url, message)
        self.assertNotIn(token, message)
        self.assertIn("<BASE_URL>", message)
        self.assertIn("<ACCESS_TOKEN>", message)
        request.assert_called_once_with(
            "GET",
            f"{base_url}/api/v1/accounts",
            headers={
                "Authorization": f"Bearer {token}",
                "Accept": "application/vnd.api+json",
            },
            data=None,
            timeout=openapi_helper.REQUEST_TIMEOUT_SECONDS,
        )


if __name__ == "__main__":
    unittest.main()
