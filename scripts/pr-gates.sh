#!/usr/bin/env bash
# Gerbang yang harus hijau sebelum PR boleh di-merge — satu perintah.
#
#   ./scripts/pr-gates.sh            keempatnya
#   ./scripts/pr-gates.sh --fast     lewati unit test & lint (hanya pemeriksa statis)
#
# Dipakai dua tempat dengan isi yang sama: CI (.github/workflows/ai-pr-review.yml)
# dan laptop sebelum mengusulkan merge. Beda isi antara keduanya berarti CI
# menolak hal yang di laptop lolos — itu yang paling membuang waktu.
#
# Kegagalan dilaporkan apa adanya, tidak dibulatkan jadi "sudah lewat".

set -uo pipefail

FAST=0
[ "${1:-}" = "--fast" ] && FAST=1

declare -a NAMES=()
declare -a RESULTS=()
status=0

run_gate() {
  local name="$1"
  shift
  echo ""
  echo "=============================================================="
  echo ">>> $name"
  echo "=============================================================="
  if "$@"; then
    NAMES+=("$name"); RESULTS+=("LULUS")
  else
    NAMES+=("$name"); RESULTS+=("GAGAL")
    status=1
  fi
}

if [ "$FAST" -eq 0 ]; then
  run_gate "Unit test (testLocalDebugUnitTest)" ./gradlew --no-daemon testLocalDebugUnitTest
  run_gate "Android lint (lintLocalDebug)"      ./gradlew --no-daemon lintLocalDebug
else
  echo "--fast: unit test & lint dilewati. Jangan pakai ini sebagai dasar merge."
fi

run_gate "Literal visual di luar token" ./scripts/check-hardcoded-ui.sh
run_gate "Pola ANR / bug laten"         ./scripts/check-anr-patterns.sh

echo ""
echo "=============================================================="
echo ">>> Ringkasan gerbang"
echo "=============================================================="
for i in "${!NAMES[@]}"; do
  printf "  %-5s  %s\n" "${RESULTS[$i]}" "${NAMES[$i]}"
done
echo ""

if [ "$status" -ne 0 ]; then
  echo "Ada gerbang yang GAGAL. Jangan usulkan merge; laporkan keluarannya."
else
  echo "Semua gerbang lulus."
fi

exit "$status"
