#!/usr/bin/env python3
"""Extract human-readable native source and verify its SHA-256 before compilation."""
import base64
import gzip
import hashlib
from pathlib import Path

root = Path(__file__).resolve().parents[1]
encoded = (root / "src/eri_android.cpp.gz.b64").read_text(encoding="ascii").strip()
source = gzip.decompress(base64.b64decode(encoded, validate=True))
expected = "16f1150767271fa79f38dc8060393f2e2fe85f4d47bd37107da4faca7fe5dc12"
digest = hashlib.sha256(source).hexdigest()
if digest != expected:
    raise RuntimeError(f"ERI source digest mismatch: {digest}")
target = root / "src/eri_android.cpp"
target.write_bytes(source)
print(f"Decoded {target}: {len(source)} bytes SHA256={digest}")
