#!/usr/bin/env python3
"""
Epic Online Services Kotlin binding codegen helper.

This script parses the EOS C headers and generates Kotlin source skeletons
for enums and structs. It is a helper, not a full transpiler: review and
refine the output before committing.

Usage:
    python scripts/codegen.py                    # generate everything
    python scripts/codegen.py --enum eos_p2p_types.h
    python scripts/codegen.py --struct eos_rtc_types.h
    python scripts/codegen.py --module p2p
"""

import argparse
import os
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

SDK_INCLUDE = Path(__file__).resolve().parent.parent / "EOS_SDK" / "SDK" / "Include"
OUT_DIR = Path(__file__).resolve().parent.parent / "src" / "main" / "kotlin" / "gg" / "sona" / "eos"

# Map C type tokens to their Kotlin/JVM FFM counterparts.
# The field name on the Kotlin side is just the same name; this is the type
# translation table used when emitting struct writers.
PRIMITIVE_MAP = {
    "int8_t": "ValueLayout.JAVA_BYTE",
    "uint8_t": "ValueLayout.JAVA_BYTE",
    "int16_t": "ValueLayout.JAVA_SHORT",
    "uint16_t": "ValueLayout.JAVA_CHAR.unsized()",
    "int32_t": "ValueLayout.JAVA_INT",
    "uint32_t": "ValueLayout.JAVA_INT",
    "int64_t": "ValueLayout.JAVA_LONG",
    "uint64_t": "ValueLayout.JAVA_LONG",
    "EOS_Bool": "ValueLayout.JAVA_INT",
    "float": "ValueLayout.JAVA_FLOAT",
    "double": "ValueLayout.JAVA_DOUBLE",
    "size_t": "ValueLayout.JAVA_LONG",
    "char": "ValueLayout.JAVA_BYTE",
}

# ID-like opaque types we should model as Long-typed value classes in Kotlin.
OPAQUE_HANDLE_TYPES = {
    "EOS_EpicAccountId",
    "EOS_ProductUserId",
    "EOS_ContinuanceToken",
    "EOS_HPlatform",
    "EOS_HMetrics",
    "EOS_HAuth",
    "EOS_HConnect",
    "EOS_HEcom",
    "EOS_HUI",
    "EOS_HFriends",
    "EOS_HPresence",
    "EOS_HSessions",
    "EOS_HLobby",
    "EOS_HUserInfo",
    "EOS_HP2P",
    "EOS_HRTC",
    "EOS_HRTCAdmin",
    "EOS_HRTCAudio",
    "EOS_HRTCData",
    "EOS_HPlayerDataStorage",
    "EOS_HTitleStorage",
    "EOS_HAchievements",
    "EOS_HStats",
    "EOS_HLeaderboards",
    "EOS_HMods",
    "EOS_HAntiCheatClient",
    "EOS_HAntiCheatServer",
    "EOS_HProgressionSnapshot",
    "EOS_HReports",
    "EOS_HSanctions",
    "EOS_HKWS",
    "EOS_HCustomInvites",
    "EOS_HIntegratedPlatform",
    "EOS_AntiCheatCommon_ClientHandle",
    "EOS_HSessionModification",
    "EOS_HSessionSearch",
    "EOS_HActiveSession",
    "EOS_HLobbyModification",
    "EOS_HLobbySearch",
    "EOS_HLobbyDetails",
    "EOS_HPresenceModification",
    "EOS_HPlayerDataStorageFileTransfer",
    "EOS_HTitleStorageFileTransfer",
    "EOS_HIntegratedPlatformOptionsContainer",
}

# ID-like opaque types with special semantics: the EOS_NotificationId
NOTIFICATION_TYPES = {"EOS_NotificationId"}


@dataclass
class EnumDef:
    name: str
    members: list = field(default_factory=list)


@dataclass
class StructField:
    c_type: str
    name: str
    array_size: int = 0  # 0 means not an array; > 0 means fixed-size array
    is_pointer: bool = False


@dataclass
class StructDef:
    name: str
    api_latest: int | None = None
    fields: list[StructField] = field(default_factory=list)


def camel(name: str) -> str:
    s1 = re.sub(r"_(.)", lambda m: m.group(1).upper(), name)
    s1 = s1.replace("Eos", "").lstrip("_")
    if not s1:
        return name
    return s1[0].lower() + s1[1:]


def eos_strip(name: str) -> str:
    """Drop EOS_ / Eos prefix and lower-case the first char for enum members."""
    if name.startswith("EOS_"):
        name = name[4:]
    elif name.startswith("Eos"):
        name = name[3:]
    if not name:
        return name
    return name[0].upper() + name[1:]


def parse_enums(path: Path) -> list[EnumDef]:
    enums: list[EnumDef] = []
    text = path.read_text(encoding="utf-8", errors="ignore")

    # Find all EOS_ENUM( Name, ... ) and EOS_ENUM_START ... EOS_ENUM_END blocks.
    for m in re.finditer(r"EOS_ENUM\(\s*(EOS_[A-Za-z0-9_]+)\s*,([\s\S]*?)\)\s*;", text):
        name = m.group(1)
        body = m.group(2)
        members = []
        for line in body.splitlines():
            line = line.strip().rstrip(",")
            if not line or line.startswith("//") or line.startswith("/*"):
                continue
            tok = line.split("=")[0].strip()
            if not tok or not re.match(r"^[A-Za-z][A-Za-z0-9_]*$", tok):
                continue
            members.append(tok)
        enums.append(EnumDef(name=name, members=members))

    for m in re.finditer(
        r"EOS_ENUM_START\(\s*(EOS_[A-Za-z0-9_]+)\s*\)([\s\S]*?)EOS_ENUM_END", text
    ):
        name = m.group(1)
        body = m.group(2)
        members = []
        for line in body.splitlines():
            line = line.strip().rstrip(",")
            if not line or line.startswith("//") or line.startswith("/*"):
                continue
            tok = line.split("=")[0].strip()
            if not tok or not re.match(r"^[A-Za-z][A-Za-z0-9_]*$", tok):
                continue
            if tok.startswith("__"):
                continue
            members.append(tok)
        enums.append(EnumDef(name=name, members=members))
    return enums


def parse_structs(path: Path) -> list[StructDef]:
    structs: list[StructDef] = []
    text = path.read_text(encoding="utf-8", errors="ignore")

    for m in re.finditer(
        r"EOS_STRUCT\(\s*(EOS_[A-Za-z0-9_]+)\s*,([\s\S]*?)\)\)\s*;", text
    ):
        name = m.group(1)
        body = m.group(2)
        api_latest = None
        struct_start = m.start()
        best_distance = None
        for m2 in re.finditer(r"#define\s+EOS_[A-Z0-9_]+_API_LATEST\s+(\d+)", text):
            if m2.start() >= struct_start:
                continue
            distance = struct_start - m2.start()
            if best_distance is None or distance < best_distance:
                best_distance = distance
                api_latest = int(m2.group(1))
        fields: list[StructField] = []
        for line in body.splitlines():
            line = line.strip().rstrip(";")
            if not line or line.startswith("//") or line.startswith("/*") or "EOS_STRUCT" in line:
                continue
            # Strip trailing inline comments.
            line = re.sub(r"/\*.*?\*/", "", line).strip()
            # Skip EOS_HAS_ENUM_CLASS style no field lines.
            if not line or line.startswith("EOS_") and "(" in line and ")" in line and ";" in line:
                continue
            # field looks like:  "const char* Name"  or "uint32_t Name"  or "char Name[33]"
            # Also might have a comment after; comments are already removed.
            m3 = re.match(
                r"^(const\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*(\*+)?\s*([A-Za-z_][A-Za-z0-9_]*)\s*(\[[^\]]*\])?\s*$",
                line,
            )
            if not m3:
                continue
            c_type = m3.group(2)
            is_ptr = m3.group(3) is not None
            fname = m3.group(4)
            arr = m3.group(5) or ""
            arr_size = 0
            if arr:
                num = re.search(r"\d+", arr)
                if num:
                    arr_size = int(num.group(0))
            fields.append(StructField(c_type=c_type, name=fname, array_size=arr_size, is_pointer=is_ptr))
        structs.append(StructDef(name=name, api_latest=api_latest, fields=fields))
    return structs


def render_enum(e: EnumDef) -> str:
    if not e.members:
        return ""
    lines = [
        "package gg.sona.eos",
        "",
        "import gg.sona.eos.internal.Native",
        "",
        "/**",
        f" * EOS enum **{e.name}**.",
        " *",
        " * Generated skeleton: verify before use.",
        " */",
        f"public enum class {eos_strip(e.name)}(override val value: Int) {{",
    ]
    for member in e.members:
        cname = member
        if cname.startswith("EOS_"):
            cname = cname[4:]
        kotlin_name = re.sub(r"_(.)", lambda m: m.group(1).upper(), cname)
        lines.append(f"    /** `{member}` */")
        lines.append(f"    {kotlin_name}(Native.const('{member}')){{}},")
    lines.append("    ;")
    lines.append("    public companion object {")
    lines.append(f"        public fun fromValue(v: Int): {eos_strip(e.name)} = entries.firstOrNull {{ it.value == v }} ?: {eos_strip(e.name)}(v)")
    lines.append("    }")
    lines.append("}")
    return "\n".join(lines) + "\n"


def render_struct(s: StructDef) -> str:
    name = s.name
    kotlin_name = eos_strip(name)
    # The struct writer is generated as an internal helper.
    out = [
        "package gg.sona.eos",
        "",
        "/**",
        f" * EOS struct **{name}**.",
        " *",
        " * Generated skeleton: verify before use.",
        " *",
    ]
    if s.api_latest is not None:
        out.append(f" * API latest: {s.api_latest}")
    out.append(" */")
    out.append(f"public class {kotlin_name} internal constructor(")
    ctor_args = ["    internal val struct: gg.sona.eos.internal.StructRef,"]
    field_lines = []
    for f in s.fields:
        kotlin_field = camel(f.name)
        if f.c_type in PRIMITIVE_MAP:
            kt = PRIMITIVE_MAP[f.c_type]
        elif f.c_type in OPAQUE_HANDLE_TYPES or f.c_type in NOTIFICATION_TYPES:
            kt = "Long"
        elif f.is_pointer or f.c_type == "void" or f.c_type.endswith("*"):
            kt = "MemorySegment?" if False else "Any?"  # placeholder
        elif f.array_size > 0:
            kt = "ByteArray"  # raw bytes
        else:
            kt = "Any?"  # opaque
        ctor_args.append(f"    public var {kotlin_field}: {kt} = null as Any?,")
        field_lines.append((f, kotlin_field, kt))
    out.append(",\n".join(ctor_args))
    out.append(") {")
    out.append("    public companion object {")
    out.append(f'        public const val API_LATEST: Int = {s.api_latest or 1}')
    out.append("    }")
    out.append("}")
    out.append("")
    return "\n".join(out)


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate Kotlin skeletons for EOS C bindings")
    parser.add_argument("--module", help="Module name (e.g. p2p, rtc)")
    parser.add_argument("--enum", help="Single header to extract enums from")
    parser.add_argument("--struct", help="Single header to extract structs from")
    parser.add_argument("--out", default=str(OUT_DIR), help="Output directory")
    args = parser.parse_args()

    out_dir = Path(args.out)
    if args.enum:
        path = SDK_INCLUDE / args.enum
        for e in parse_enums(Path(path)):
            print(render_enum(e))
        return 0
    if args.struct:
        path = SDK_INCLUDE / args.struct
        for s in parse_structs(Path(path)):
            print(render_struct(s))
        return 0

    headers = sorted(SDK_INCLUDE.glob("*.h"))
    print(f"// Found {len(headers)} headers under {SDK_INCLUDE}", file=sys.stderr)
    print(f"// {len(list(headers))} headers to scan", file=sys.stderr)
    for path in headers:
        for e in parse_enums(path):
            print(render_enum(e))
        for s in parse_structs(path):
            print(render_struct(s))
    return 0


if __name__ == "__main__":
    sys.exit(main())
