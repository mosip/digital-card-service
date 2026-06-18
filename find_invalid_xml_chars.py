#!/usr/bin/env python3
"""
find_invalid_xml_chars.py

Pinpoints which field(s) in a MOSIP credential / demographic-identity JSON
contain characters that are ILLEGAL in XML 1.0. These are exactly the bytes
that make openhtmltopdf fail in digital-card-service with:

    SAXParseException: Character reference "&#xf" is an invalid XML character
    -> KER-PDG-001 -> DCS-011 (card stuck at ISSUED)

XML 1.0 allows only:  #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
Everything else (the C0 control chars 0x00-0x08, 0x0B, 0x0C, 0x0E-0x1F, etc.) is invalid.

USAGE
  # Scan the decrypted credential JSON that the service processes:
  python find_invalid_xml_chars.py decrypted_credential.json

  # Or pipe JSON in:
  cat decrypted_credential.json | python find_invalid_xml_chars.py -

The decrypted credential JSON is what you'd get from the datashare URL
(datashareurl column) after decryption, i.e. the object PDFCardServiceImpl
receives as `decryptedCredentialJson`.
"""

import json
import sys


def is_invalid_xml_char(cp: int) -> bool:
    """Return True if a Unicode code point is NOT permitted in XML 1.0."""
    if cp in (0x09, 0x0A, 0x0D):
        return False
    if 0x20 <= cp <= 0xD7FF:
        return False
    if 0xE000 <= cp <= 0xFFFD:
        return False
    if 0x10000 <= cp <= 0x10FFFF:
        return False
    return True


def scan_string(path: str, value: str, findings: list):
    bad = []
    for idx, ch in enumerate(value):
        cp = ord(ch)
        if is_invalid_xml_char(cp):
            bad.append((idx, cp))
    if bad:
        # Build a readable preview with offending chars shown as <0xNN>
        preview_chars = []
        for i, ch in enumerate(value[:120]):
            cp = ord(ch)
            preview_chars.append(f"<0x{cp:02X}>" if is_invalid_xml_char(cp) else ch)
        findings.append({
            "field": path,
            "count": len(bad),
            "codepoints": sorted({f"0x{cp:X}" for _, cp in bad}),
            "first_positions": [i for i, _ in bad[:10]],
            "preview": "".join(preview_chars),
        })


def walk(node, path, findings):
    if isinstance(node, dict):
        for k, v in node.items():
            walk(v, f"{path}.{k}" if path else str(k), findings)
    elif isinstance(node, list):
        for i, v in enumerate(node):
            walk(v, f"{path}[{i}]", findings)
    elif isinstance(node, str):
        scan_string(path, node, findings)
    # numbers / bools / null can't carry control chars


def main():
    if len(sys.argv) != 2:
        print(__doc__)
        sys.exit(1)

    src = sys.argv[1]
    raw = sys.stdin.read() if src == "-" else open(src, "r", encoding="utf-8", errors="surrogatepass").read()

    try:
        data = json.loads(raw)
    except json.JSONDecodeError as e:
        # If JSON itself won't parse, scan the raw text so we still locate the bytes.
        print(f"[warn] JSON did not parse ({e}); scanning raw text instead.\n")
        findings = []
        scan_string("<raw-text>", raw, findings)
        report(findings)
        return

    findings = []
    walk(data, "", findings)
    report(findings)


def report(findings):
    if not findings:
        print("No invalid XML 1.0 characters found. This data would NOT trigger KER-PDG-001.")
        return
    print(f"Found {len(findings)} field(s) with invalid XML characters:\n")
    for f in sorted(findings, key=lambda x: -x["count"]):
        print(f"  FIELD     : {f['field']}")
        print(f"  bad chars : {f['count']}  codepoints={f['codepoints']}")
        print(f"  positions : {f['first_positions']}")
        print(f"  preview   : {f['preview']}")
        print()
    print("=> These field values must be sanitized before the template merge / PDF render.")


if __name__ == "__main__":
    main()
