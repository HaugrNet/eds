# Independent implementation of the EDS data format v1 (PLAN.md, Crypto §4), from the spec only.
# Regenerates data-format-v1.properties next to this script: python3 -I data-format-v1.py
import os, struct, hashlib
from cryptography.hazmat.primitives.kdf.hkdf import HKDF
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

def object_key(secret, salt, circle, obj, ver):
    info = f"eds/object|{circle}|{obj}|{ver}".encode()
    return HKDF(algorithm=hashes.SHA512(), length=32, salt=salt, info=info).derive(secret)

def encrypt(secret, circle, obj, ver, chunk, salt, prefix, plain):
    header = bytes([1]) + struct.pack(">I", chunk) + salt + prefix
    key = object_key(secret, salt, circle, obj, ver)
    aad = header + f"|{circle}|{obj}|{ver}".encode()
    n = max(1, -(-len(plain) // chunk))
    out = header
    for i in range(n):
        nonce = prefix + struct.pack(">I", i) + bytes([1 if i == n - 1 else 0])
        out += AESGCM(key).encrypt(nonce, plain[i*chunk:(i+1)*chunk], aad)
    return out

secret = bytes(range(32))
salt = bytes(range(0x40, 0x60))
prefix = bytes.fromhex("a0a1a2a3a4a5a6")
circle, obj, ver = "d8838d7d-71e7-433d-8790-af7c080e9de9", "5ebda360-5e3d-4e88-82fb-947d25075a4a", 7
cases = {
  "empty": (32, b""),
  "partial": (32, b"Hello, EDS!"),
  "exact": (32, bytes((i * 7) % 256 for i in range(64))),
  "multi": (32, bytes((i * 13) % 256 for i in range(100))),
}
lines = ["# EDS data format v1 test vectors (PLAN.md, Crypto §4).",
         "# Computed with an independent implementation (Python 'cryptography') from the spec.",
         "# Production uses chunk size 65536; small chunks keep the vectors readable.",
         f"secret = {secret.hex()}", f"salt = {salt.hex()}", f"prefix = {prefix.hex()}",
         f"circleId = {circle}", f"objectId = {obj}", f"circleSecretVersion = {ver}",
         f"objectKey = {object_key(secret, salt, circle, obj, ver).hex()}"]
for name, (chunk, plain) in cases.items():
    lines += [f"{name}.chunkSize = {chunk}", f"{name}.plaintext = {plain.hex()}",
              f"{name}.stored = {encrypt(secret, circle, obj, ver, chunk, salt, prefix, plain).hex()}"]
open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "data-format-v1.properties"), "w").write("\n".join(lines) + "\n")
