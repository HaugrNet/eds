# Independent implementation of the convention of Rune and Tolk for end-to-end
# encryption, version 1 (E2E.md, "The convention of Rune and Tolk"), written from
# the specification only. It makes the test vectors which both clients must
# reproduce: python3 -I vectors/e2e-v1.py   (in eds-quarkus/src/main/webui)
#
# Needs the Python package "cryptography"; nothing of EDS, Rune or Tolk.
import base64, hashlib, json, os, struct
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.kdf.hkdf import HKDF

PREFIX = "eds-e2e/v1/"
MAGIC = b"EDSE"


def b64u(data):
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def hkdf(ikm, salt, info):
    """HKDF-SHA-512 to 32 bytes; no salt means HKDF's default, zeros."""
    return HKDF(algorithm=hashes.SHA512(), length=32, salt=salt, info=info.encode()).derive(ikm)


def seal(key, nonce, aad, plain):
    """AES-256-GCM: nonce (12) | ciphertext | tag (16)."""
    return nonce + AESGCM(key).encrypt(nonce, plain, aad.encode())


# --- The Member's keys ------------------------------------------------------

def private_key(scalar):
    return ec.derive_private_key(int.from_bytes(scalar, "big"), ec.SECP384R1())


def public_point(private):
    """The public key as the uncompressed point: 0x04 | X (48) | Y (48)."""
    return private.public_key().public_bytes(serialization.Encoding.X962, serialization.PublicFormat.UncompressedPoint)


def public_key_text(point):
    """Member.publicKey: the convention, its version, the suite, the point."""
    return "eds-e2e:1:1:" + b64u(point)


def fingerprint(point):
    """What two people compare: SHA-256 of the point, the first 16 bytes, in groups of four hex digits."""
    digest = hashlib.sha256(point).hexdigest()[:32]
    return " ".join(digest[i:i + 4] for i in range(0, 32, 4))


def recovery_key(account_key):
    """The account key as people write it down: Base32 without padding, in groups of four."""
    text = base64.b32encode(account_key).decode().rstrip("=")
    return "-".join(text[i:i + 4] for i in range(0, len(text), 4))


def vault(account_key, member_id, scalar, salt, nonce):
    """Member.vault: the private key (the scalar, 48 bytes) under the account key."""
    info = PREFIX + "vault|" + member_id
    key = hkdf(account_key, salt, info)
    return json.dumps({"e2e": 1, "suite": 1, "salt": b64u(salt), "key": b64u(seal(key, nonce, info, scalar))}, separators=(",", ":"))


# --- The room key -----------------------------------------------------------

def secret_by_key(room_key, version, circle_id, member_id, member_point, ephemeral_scalar, nonce):
    """Trustee.secret: the room key wrapped to the Member's public key."""
    ephemeral = private_key(ephemeral_scalar)
    epk = public_point(ephemeral)
    member = ec.EllipticCurvePublicKey.from_encoded_point(ec.SECP384R1(), member_point)
    shared = ephemeral.exchange(ec.ECDH(), member)
    info = PREFIX + "secret|" + circle_id + "|" + member_id + "|" + str(version)
    key = hkdf(shared, epk + member_point, info)
    return json.dumps({"e2e": 1, "suite": 1, "by": "key", "version": version, "epk": b64u(epk),
                       "key": b64u(seal(key, nonce, info, room_key))}, separators=(",", ":"))


def secret_by_invite(room_key, version, circle_id, name, invite_secret, nonce):
    """Trustee.secret of an invitation: the room key under the invite secret of the link."""
    info = PREFIX + "invite|" + circle_id + "|" + name + "|" + str(version)
    key = hkdf(invite_secret, None, info)
    return json.dumps({"e2e": 1, "suite": 1, "by": "invite", "version": version,
                       "key": b64u(seal(key, nonce, info, room_key))}, separators=(",", ":"))


def shared_value(circle_id, version, room_key, earlier):
    """Circle.circleKey: marks the chamber, and holds the earlier room keys under the current one."""
    base = PREFIX + "earlier|" + circle_id + "|" + str(version)
    key = hkdf(room_key, None, base)
    sealed = {}
    for old_version, (old_key, nonce) in sorted(earlier.items()):
        sealed[str(old_version)] = b64u(seal(key, nonce, base + "|" + str(old_version), old_key))
    return json.dumps({"e2e": 1, "suite": 1, "version": version, "earlier": sealed}, separators=(",", ":"))


def invitation_link(name, token, invite_secret):
    """The fragment of an invitation link: name, token and, for a chamber, the invite secret."""
    payload = json.dumps({"n": name, "t": token, "k": b64u(invite_secret)}, separators=(",", ":"))
    return "#invite=" + b64u(payload.encode())


# --- Content ----------------------------------------------------------------

def content(room_key, version, circle_id, context, chunk, salt, prefix, plain):
    """A file, a thumbnail or an entry of a feature, encrypted by the client.

    header = "EDSE" | format 0x01 | suite 0x01 | room key version (4, BE)
             | 0x01 | chunk size (4, BE) | salt (32) | nonce prefix (7)
    Then the chunks as in PLAN.md, Crypto section 4: AES-256-GCM, the nonce is
    prefix | chunk index (4, BE) | 1 for the last chunk, else 0.
    """
    header = MAGIC + bytes([1, 1]) + struct.pack(">I", version) + bytes([1]) + struct.pack(">I", chunk) + salt + prefix
    key = hkdf(room_key, salt, PREFIX + "content|" + circle_id + "|" + str(version))
    aad = header + ("|" + circle_id + "|" + context).encode()
    count = max(1, -(-len(plain) // chunk))
    out = header
    for i in range(count):
        nonce = prefix + struct.pack(">I", i) + bytes([1 if i == count - 1 else 0])
        out += AESGCM(key).encrypt(nonce, plain[i * chunk:(i + 1) * chunk], aad)
    return out


# --- The vectors ------------------------------------------------------------

CIRCLE = "d8838d7d-71e7-433d-8790-af7c080e9de9"
MEMBER = "073dcc8f-ffa6-4cda-8d61-09ba9441e78e"
scalar = bytes(range(1, 49))
ephemeral = bytes(range(101, 149))
account_key = bytes(range(0x20, 0x40))
room_key_1 = bytes(range(0x40, 0x60))
room_key_2 = bytes(range(0x60, 0x80))
room_key_3 = bytes(range(0x80, 0xa0))
invite_secret = bytes(range(0xa0, 0xc0))
salt = bytes(range(0xc0, 0xe0))
nonce = bytes.fromhex("000102030405060708090a0b")
nonce2 = bytes.fromhex("101112131415161718191a1b")
nonce3 = bytes.fromhex("202122232425262728292a2b")
prefix = bytes.fromhex("a0a1a2a3a4a5a6")
point = public_point(private_key(scalar))

vectors = {
    "about": "Test vectors of the convention of Rune and Tolk for end-to-end encryption, version 1, suite 1 (E2E.md). "
             "Made by vectors/e2e-v1.py, an independent implementation. Binary values are Base64 (URL-safe, no padding) unless named hex.",
    "circleId": CIRCLE,
    "memberId": MEMBER,
    "keys": {
        "privateKey": b64u(scalar),
        "publicPoint": b64u(point),
        "publicKey": public_key_text(point),
        "fingerprint": fingerprint(point),
        "accountKey": b64u(account_key),
        "recoveryKey": recovery_key(account_key),
        "deviceLink": "#device=" + b64u(account_key),
    },
    "vault": {
        "salt": b64u(salt),
        "nonce": b64u(nonce),
        "vault": vault(account_key, MEMBER, scalar, salt, nonce),
    },
    "secretByKey": {
        "roomKey": b64u(room_key_3),
        "version": 3,
        "ephemeralPrivateKey": b64u(ephemeral),
        "nonce": b64u(nonce2),
        "secret": secret_by_key(room_key_3, 3, CIRCLE, MEMBER, point, ephemeral, nonce2),
    },
    "secretByInvite": {
        "roomKey": b64u(room_key_3),
        "version": 3,
        "name": "Guest of the Chamber",
        "inviteSecret": b64u(invite_secret),
        "nonce": b64u(nonce3),
        "secret": secret_by_invite(room_key_3, 3, CIRCLE, "Guest of the Chamber", invite_secret, nonce3),
        "token": "dG9rZW4tb2YtdGhlLXN0b25l",
        "link": invitation_link("Guest of the Chamber", "dG9rZW4tb2YtdGhlLXN0b25l", invite_secret),
    },
    "sharedValue": {
        "version": 3,
        "roomKey": b64u(room_key_3),
        "earlier": {"1": {"roomKey": b64u(room_key_1), "nonce": b64u(nonce)}, "2": {"roomKey": b64u(room_key_2), "nonce": b64u(nonce2)}},
        "value": shared_value(CIRCLE, 3, room_key_3, {1: (room_key_1, nonce), 2: (room_key_2, nonce2)}),
        "first": shared_value(CIRCLE, 1, room_key_1, {}),
    },
    "content": [],
}

cases = [
    ("empty", "", 16, b""),
    ("partial", "", 16, b"hello chamber"),
    ("exact multiple", "", 16, bytes(range(32))),
    ("several chunks", "", 16, bytes(range(53))),
    ("a thumbnail", "thumbnail", 16, b"\x89PNG not really"),
    ("a file name", "name", 65536, "Quarterly report (Q4).pdf".encode()),
    ("an entry of a feature", "chat|message", 65536, json.dumps({"v": 1, "text": "Hello, chamber"}, separators=(",", ":")).encode()),
    ("an earlier key", "", 16, b"written under the first key"),
]
for name, context, chunk, plain in cases:
    key, version = (room_key_1, 1) if name == "an earlier key" else (room_key_3, 3)
    vectors["content"].append({
        "name": name,
        "context": context,
        "version": version,
        "roomKey": b64u(key),
        "chunkSize": chunk,
        "salt": b64u(salt),
        "noncePrefix": prefix.hex(),
        "plain": b64u(plain),
        "encrypted": b64u(content(key, version, CIRCLE, context, chunk, salt, prefix, plain)),
    })

# A file of the usual chunk size, too long to print: its hash.
large = bytes((i * 7 + 3) % 256 for i in range(65536 * 2 + 100))
vectors["large"] = {
    "name": "two full chunks and a rest, of 64 KiB",
    "length": len(large),
    "pattern": "byte i is (i * 7 + 3) mod 256",
    "version": 3,
    "roomKey": b64u(room_key_3),
    "chunkSize": 65536,
    "salt": b64u(salt),
    "noncePrefix": prefix.hex(),
    "sha256": hashlib.sha256(content(room_key_3, 3, CIRCLE, "", 65536, salt, prefix, large)).hexdigest(),
}

target = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "e2e", "vectors.json")
with open(target, "w", encoding="utf-8") as out:
    json.dump(vectors, out, indent=2, ensure_ascii=False)
    out.write("\n")
print("wrote", os.path.normpath(target))
