#!/usr/bin/env bash

# Enforce UI agar tidak menggunakan:
# - hardcoded color
# - hardcoded dimension
# - hardcoded typography size
# - hardcoded user-facing UI string
#
# Catatan:
# String yang bersifat internal/data seperti:
# - animation label
# - transaction title/description
# - log message
# - URL
# - API key
# tidak dianggap sebagai hardcoded UI.

set -uo pipefail

SRC="app/src/main/java"

# File yang diperbolehkan berisi token UI.
TOKEN_FILES='(Color|Dimens|Shape|Type|Strings|StringResource|Typography|Theme)\.kt'

FAILED=0

echo "======================================"
echo " Hardcoded UI Check"
echo "======================================"
echo ""

# --------------------------------------------------
# 1. Hardcoded Color
# --------------------------------------------------

echo "[1/4] Checking hardcoded colors..."

color_hits=$(
  grep -rnE \
    --include='*.kt' \
    'Color\(0x[0-9A-Fa-f]{8}\)' \
    "$SRC" \
    | grep -vE "$TOKEN_FILES" \
    || true
)

if [ -n "$color_hits" ]; then
  echo "FAIL: hardcoded color ditemukan:"
  echo "$color_hits"
  FAILED=1
else
  echo "OK: tidak ada hardcoded color"
fi

echo ""

# --------------------------------------------------
# 2. Hardcoded Dimension / Typography
# --------------------------------------------------

echo "[2/4] Checking hardcoded dimensions..."

dimension_hits=$(
  grep -rnE \
    --include='*.kt' \
    '(^|[^a-zA-Z0-9_])[0-9]+(\.[0-9]+)?\.(dp|sp)\b' \
    "$SRC" \
    | grep -vE "$TOKEN_FILES" \
    || true
)

if [ -n "$dimension_hits" ]; then
  echo "FAIL: hardcoded dimension ditemukan:"
  echo "$dimension_hits"
  FAILED=1
else
  echo "OK: tidak ada hardcoded dimension"
fi

echo ""

# --------------------------------------------------
# 3. Hardcoded UI String
# --------------------------------------------------

echo "[3/4] Checking hardcoded UI strings..."

string_hits=$(
  grep -rnE \
    --include='*.kt' \
    'Text\(\s*"[^"]+"|contentDescription\s*=\s*"[^"]+"|placeholder\s*=\s*"[^"]+"|setText\(\s*"[^"]+"|Toast\.makeText\([^,]+,\s*"[^"]+"' \
    "$SRC" \
    | grep -vE "$TOKEN_FILES" \
    || true
)

if [ -n "$string_hits" ]; then
  echo "FAIL: hardcoded UI string ditemukan:"
  echo "$string_hits"
  FAILED=1
else
  echo "OK: tidak ada hardcoded UI string"
fi

echo ""

# --------------------------------------------------
# 4. Hardcoded UI String Property
# --------------------------------------------------

echo "[4/4] Checking user-facing UI properties..."

property_hits=$(
  grep -rnE \
    --include='*.kt' \
    '(supportingText|error|placeholderText|accessibilityLabel|hint)\s*=\s*"[^"]+"' \
    "$SRC" \
    | grep -vE "$TOKEN_FILES" \
    || true
)

if [ -n "$property_hits" ]; then
  echo "FAIL: hardcoded UI property string ditemukan:"
  echo "$property_hits"
  FAILED=1
else
  echo "OK: tidak ada hardcoded UI property string"
fi

echo ""

# --------------------------------------------------
# Result
# --------------------------------------------------

echo "======================================"

if [ "$FAILED" -ne 0 ]; then
  echo "FAIL: hardcoded UI ditemukan."
  echo "Gunakan stringResource()/strings.xml atau token UI yang sesuai."
  echo "======================================"
  exit 1
fi

echo "PASS: tidak ditemukan hardcoded UI."
echo "======================================"

exit 0