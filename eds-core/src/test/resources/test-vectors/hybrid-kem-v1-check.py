# Independent check of the hybrid KEM combiner, ECDH and seal (PLAN.md, Crypto §3, HybridKem v1).
# Checks hybrid-kem-v1.properties next to this script: python3 -I hybrid-kem-v1-check.py
import os, struct, hashlib
from cryptography.hazmat.primitives.kdf.hkdf import HKDF
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "hybrid-kem-v1.properties")
v = dict(l.split(" = ", 1) for l in open(path).read().splitlines() if l and not l.startswith("#"))
b = {k: bytes.fromhex(x) for k, x in v.items()}
w = b["wrapped"]; assert w[0] == 1
n1 = struct.unpack(">I", w[1:5])[0]; ct = w[5:5+n1]; o = 5 + n1
n2 = struct.unpack(">I", w[o:o+4])[0]; epk = w[o+4:o+4+n2]; sealed = w[o+4+n2:]
# ECDH, independently
ecpriv = serialization.load_der_private_key(b["ecPrivate"], None)
ss_ecdh = ecpriv.exchange(ec.ECDH(), serialization.load_der_public_key(epk))
assert ss_ecdh == b["ssEcdh"], "ECDH mismatch"
transcript = hashlib.sha512(ct + epk + b["mlkemPublic"] + b["ecPublic"]).digest()
kek = HKDF(hashes.SHA512(), 32, b"eds/hybrid-kem/v1", b"eds/hybrid-kem/v1/kek" + transcript).derive(b["ssKem"] + ss_ecdh)
assert kek == b["kek"], "combiner mismatch"
plain = AESGCM(kek).decrypt(sealed[:12], sealed[12:], b["aad"])
assert plain == b["secret"], "seal mismatch"
print("hybrid KEM vector verified independently: ECDH, combiner and seal match")
