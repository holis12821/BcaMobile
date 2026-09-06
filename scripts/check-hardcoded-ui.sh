#!/usr/bin/env bash
# Menolak literal visual di luar file token — dengan baseline.
#
# Pelanggaran lama yang sudah tercatat di baseline dibiarkan.
# Yang ditolak hanya penambahan baru. Utang lama tidak boleh tumbuh.
#
#   ./scripts/check-hardcoded-ui.sh                    cek
#   ./scripts/check-hardcoded-ui.sh --update-baseline  catat kondisi sekarang
#
# Baseline menyimpan JUMLAH pelanggaran per file, bukan nomor baris —
# supaya tidak busuk saat kode digeser atau diformat ulang.

set -uo pipefail

SRC="app/src/main/java"
TOKEN_FILES='(Color|Dimens|Shape|Type)\.kt'
BASELINE="scripts/ui-baseline.txt"
PATTERN='Color\(0x|[^a-zA-Z0-9_][0-9]+\.dp|[^a-zA-Z0-9_][0-9]+\.sp'

scan() {
  grep -rcE "$PATTERN" --include='*.kt' "$SRC" 2>/dev/null \
    | grep -vE "$TOKEN_FILES" \
    | awk -F: '$2 > 0 { print $1" "$2 }' \
    | sort
}

if [ "${1:-}" = "--update-baseline" ]; then
  mkdir -p "$(dirname "$BASELINE")"
  # Baris header wajib ada: awk NR==FNR salah baca kalau file pertama kosong.
  { echo "# baseline pelanggaran visual — jangan diedit tangan"; scan; } > "$BASELINE"
  echo "Baseline diperbarui: $(grep -vc '^#' "$BASELINE" | tr -d ' ') file, $(awk '!/^#/{s+=$2} END{print s+0}' "$BASELINE") pelanggaran."
  echo "Commit file ini. Jangan perbarui lagi kecuali angkanya TURUN."
  exit 0
fi

[ -f "$BASELINE" ] || echo "# baseline pelanggaran visual — jangan diedit tangan" > "$BASELINE"

current=$(scan)

violations=$(awk '
  NR==FNR { if ($0 !~ /^#/) base[$1] = $2; next }
  {
    b = ($1 in base) ? base[$1] : 0
    if ($2 > b) printf "  %s: %d pelanggaran (baseline %d)\n", $1, $2, b
  }
' "$BASELINE" <(echo "$current"))

if [ -n "$violations" ]; then
  echo "FAIL: pelanggaran baru di luar file token"
  echo "$violations"
  echo
  echo "Pakai token dari design-tokens.md. Kalau tokennya tidak ada, STOP dan lapor."
  echo "Jangan jalankan --update-baseline untuk melewati ini."
  exit 1
fi

improved=$(awk '
  NR==FNR { cur[$1] = $2; next }
  /^#/ { next }
  {
    c = ($1 in cur) ? cur[$1] : 0
    if (c < $2) printf "  %s: %d -> %d\n", $1, $2, c
  }
' <(echo "$current") "$BASELINE")

if [ -n "$improved" ]; then
  echo "OK — dan ada perbaikan:"
  echo "$improved"
  echo
  echo "Jalankan --update-baseline lalu commit, supaya perbaikannya terkunci."
else
  echo "OK: tidak ada pelanggaran baru."
fi
