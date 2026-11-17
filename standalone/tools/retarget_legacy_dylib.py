#!/usr/bin/env python3
"""Retarget the install name of a user-supplied iOS ARM64 Mach-O DYLIB.

Uses the original binary as the basis to preserve its compiled hooks.
This does NOT integrate new ImGui source code, remove login, or prove hooks work.
No game function bytes are modified.
"""
from __future__ import annotations

import argparse
import hashlib
from pathlib import Path
import struct

MH_MAGIC_64 = 0xFEEDFACF
CPU_TYPE_ARM64 = 0x0100000C
MH_DYLIB = 6
LC_ID_DYLIB = 0xD
OLD_NAME = b"@executable_path/Frameworks/zsmvn.dylib"
NEW_NAME = b"@executable_path/Frameworks/ERI.dylib"
REQUIRED_SYMBOLS = (
    b"ZSM_ApplyVehicleAndSkinMod",
    b"ZSM_GetRealVehiclePhysicsCaps",
    b"ZSM_UpdateGroup5ShadowReplayBot",
    b"ZSM_UpdateAutoMatchAndRoom",
)


def patch(src: Path, dst: Path) -> None:
    if src.resolve() == dst.resolve():
        raise ValueError("Never overwrite the original dylib")
    original = src.read_bytes()
    buf = bytearray(original)
    if len(buf) < 32:
        raise ValueError("Not a Mach-O file")
    magic, cpu, _subtype, ftype, ncmds, cmdbytes, _flags, _reserved = struct.unpack_from(
        "<8I", buf
    )
    if (magic, cpu, ftype) != (MH_MAGIC_64, CPU_TYPE_ARM64, MH_DYLIB):
        raise ValueError("Expected unencrypted ARM64 Mach-O dylib")
    if cmdbytes > len(buf) - 32 or ncmds > 4096:
        raise ValueError("Invalid Mach-O load command range")

    at, updated = 32, 0
    for _ in range(ncmds):
        if at + 8 > 32 + cmdbytes:
            raise ValueError("Truncated Mach-O load command")
        cmd, size = struct.unpack_from("<II", buf, at)
        if size < 8 or at + size > 32 + cmdbytes:
            raise ValueError("Invalid Mach-O load command size")
        if cmd == LC_ID_DYLIB:
            offset = struct.unpack_from("<I", buf, at + 8)[0]
            start = at + offset
            if offset >= size:
                raise ValueError("Malformed LC_ID_DYLIB name offset")
            end = buf.index(0, start, at + size)
            if bytes(buf[start:end]) != OLD_NAME:
                raise ValueError("Unexpected original install name")
            if len(NEW_NAME) > len(OLD_NAME):
                raise ValueError("Replacement install name does not fit")
            buf[start : end + 1] = NEW_NAME.ljust(len(OLD_NAME) + 1, b"\0")
            updated += 1
        at += size
    if updated != 1:
        raise ValueError(f"Expected exactly one LC_ID_DYLIB, got {updated}")
    for symbol in REQUIRED_SYMBOLS:
        if symbol not in buf:
            raise ValueError(f"Original missing expected hook symbol: {symbol!r}")
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_bytes(buf)
    print("Source SHA256:", hashlib.sha256(original).hexdigest())
    print("Output SHA256:", hashlib.sha256(buf).hexdigest())
    print("Changed bytes:", sum(a != b for a, b in zip(original, buf)))
    print("Output:", dst)


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("input", type=Path)
    ap.add_argument("output", type=Path)
    a = ap.parse_args()
    patch(a.input, a.output)
