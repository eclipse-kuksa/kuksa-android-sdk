#!/usr/bin/env bash
#
# Copyright (c) 2026 Contributors to the Eclipse Foundation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

AUTH_DIR="${ROOT_DIR}/kuksa-java-sdk/src/test/resources/authentication"
TLS_DIR="${ROOT_DIR}/kuksa-java-sdk/src/test/resources/tls"

mkdir -p "${AUTH_DIR}"
mkdir -p "${TLS_DIR}"

echo "==> Generating JWT authentication keys and tokens in ${AUTH_DIR}..."

python3 - <<EOF
import base64
import json
import os
import subprocess
import tempfile

def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).decode('ascii').rstrip('=')

tmpdir = tempfile.mkdtemp()
priv_key_path = os.path.join(tmpdir, 'jwt.key')
pub_key_path = '${AUTH_DIR}/jwt.key.pub'

# Generate 4096-bit RSA private key and export public key in PEM format
subprocess.run(['openssl', 'genrsa', '-out', priv_key_path, '4096'], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
subprocess.run(['openssl', 'rsa', '-in', priv_key_path, '-pubout', '-out', pub_key_path], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

# Valid until 2045-01-01T00:00:00Z
exp_timestamp = 2366841600

tokens = {
    'actuate-provide-all': {
        'sub': 'local dev',
        'iss': 'createToken.py',
        'aud': ['kuksa.val'],
        'iat': 1516239022,
        'exp': exp_timestamp,
        'scope': 'actuate provide'
    },
    'provide-all': {
        'sub': 'local dev',
        'iss': 'createToken.py',
        'aud': ['kuksa.val'],
        'iat': 1516239022,
        'exp': exp_timestamp,
        'scope': 'provide'
    },
    'read-all': {
        'sub': 'local dev',
        'iss': 'createToken.py',
        'aud': ['kuksa.val'],
        'iat': 1516239022,
        'exp': exp_timestamp,
        'scope': 'read'
    }
}

header = {'typ': 'JWT', 'alg': 'RS256'}
header_b64 = b64url(json.dumps(header, separators=(',', ':')).encode('utf-8'))

for name, payload in tokens.items():
    payload_b64 = b64url(json.dumps(payload, separators=(',', ':')).encode('utf-8'))
    signing_input = f'{header_b64}.{payload_b64}'.encode('utf-8')

    p = subprocess.Popen(['openssl', 'dgst', '-sha256', '-sign', priv_key_path],
                         stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    sig, err = p.communicate(signing_input)
    if p.returncode != 0:
        raise RuntimeError(f"Signing failed: {err.decode('utf-8')}")

    token = f'{header_b64}.{payload_b64}.{b64url(sig)}'
    with open(f'${AUTH_DIR}/{name}.token', 'w') as f:
        f.write(token)

print("JWT tokens and public key successfully generated.")
EOF

echo "==> Generating TLS certificates in ${TLS_DIR}..."

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

CA_KEY="${TMP_DIR}/CA.key"
CA_CERT="${TLS_DIR}/CA.pem"
SERVER_KEY="${TLS_DIR}/Server.key"
SERVER_CSR="${TMP_DIR}/Server.csr"
SERVER_CERT="${TLS_DIR}/Server.pem"
EXT_FILE="${TMP_DIR}/server.ext"

cat > "${EXT_FILE}" <<EOF
subjectAltName = @alt_names
basicConstraints = CA:FALSE
keyUsage = nonRepudiation, digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth

[alt_names]
DNS.1 = Server
DNS.2 = localhost
IP.1 = 127.0.0.1
EOF

# 1. Generate CA private key and self-signed CA cert (valid 20 years = 7300 days)
openssl genrsa -out "${CA_KEY}" 2048 2>/dev/null
openssl req -x509 -new -nodes -key "${CA_KEY}" -sha256 -days 7300 \
    -subj "/C=CA/ST=Ontario/L=Ottawa/O=Eclipse.org Foundation, Inc./CN=localhost-ca/emailAddress=kuksa-dev@eclipse.org" \
    -out "${CA_CERT}" 2>/dev/null

# 2. Generate Server private key and CSR
openssl genrsa -out "${SERVER_KEY}" 2048 2>/dev/null
openssl req -new -key "${SERVER_KEY}" \
    -subj "/C=CA/ST=Ontario/L=Ottawa/O=Eclipse.org Foundation, Inc./CN=Server/emailAddress=kuksa-dev@eclipse.org" \
    -out "${SERVER_CSR}" 2>/dev/null

# 3. Sign Server certificate with CA (valid 20 years = 7300 days)
openssl x509 -req -in "${SERVER_CSR}" \
    -CA "${CA_CERT}" -CAkey "${CA_KEY}" -CAcreateserial \
    -out "${SERVER_CERT}" -days 7300 -sha256 \
    -extfile "${EXT_FILE}" 2>/dev/null

echo "TLS CA and Server certificates successfully generated."
echo "==> Done!"
