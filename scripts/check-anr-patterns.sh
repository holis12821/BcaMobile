#!/usr/bin/env bash
# Menolak pola yang jadi ANR, kebocoran, dan bug halus di kemudian hari.
#
#   ./scripts/check-anr-patterns.sh                    cek
#   ./scripts/check-anr-patterns.sh --update-baseline  catat kondisi sekarang
#
# Dua tingkat:
#
#   TERLARANG  — ditolak selalu, tanpa baseline. Tidak ada pemakaian sah di
#                aplikasi ini. Kalau ada perkecualian nyata, tulis alasannya
#                di baris yang sama sebagai `// anr-allow: <alasan>` — reviewer
#                manusia dan AI akan membacanya.
#   BERBASELINE — pola yang kadang sah (runBlocking di OkHttp Authenticator,
#                Log di berkas debug). Yang ditolak hanya PENAMBAHAN baru,
#                sama seperti check-hardcoded-ui.sh. Utang lama tidak tumbuh.
#
# Baseline menyimpan JUMLAH per pola per file, bukan nomor baris, supaya tidak
# busuk saat kode digeser.

set -uo pipefail

SRC="app/src/main/java"
BASELINE="scripts/anr-baseline.txt"

# key=regex. Key dipakai di baseline, jadi jangan diganti nama sembarangan.
FORBIDDEN=(
  "globalscope=GlobalScope"
  "thread-sleep=Thread\.sleep"
  "print-stacktrace=printStackTrace"
  "main-thread-db=allowMainThreadQueries"
  "internal-api-key=X-Internal-API-Key"
  "runblocking-ui=runBlocking.*(Dispatchers\.Main|withContext\(Dispatchers\.Main)"
)

BASELINED=(
  "runblocking=runBlocking"
  "not-null-assert=[a-zA-Z0-9_)\]]!!"
  "logcat=Log\.[dveiwa]\("
  "simpledateformat=SimpleDateFormat"
  "format-tanpa-locale=String\.format\(\""
  "shared-prefs=getSharedPreferences\("
  "while-true=while \(true\)"
  "url-literal=\"https?://"
)

scan_group() {
  local -a group=("$@")
  local entry key rx
  for entry in "${group[@]}"; do
    key="${entry%%=*}"
    rx="${entry#*=}"
    grep -rnE "$rx" --include='*.kt' "$SRC" 2>/dev/null \
      | grep -v 'anr-allow' \
      | awk -F: -v k="$key" '{ print k" "$1 }'
  done
}

scan_baselined() {
  scan_group "${BASELINED[@]}" | sort | uniq -c | awk '{ print $2" "$3" "$1 }' | sort
}

if [ "${1:-}" = "--update-baseline" ]; then
  mkdir -p "$(dirname "$BASELINE")"
  { echo "# baseline pola ANR/bug — jangan diedit tangan"; scan_baselined; } > "$BASELINE"
  echo "Baseline diperbarui: $(awk '!/^#/{n++} END{print n+0}' "$BASELINE") baris, $(awk '!/^#/{s+=$3} END{print s+0}' "$BASELINE") kemunculan."
  echo "Commit berkas ini. Jangan perbarui lagi kecuali angkanya TURUN."
  exit 0
fi

status=0

forbidden_hits=$(scan_group "${FORBIDDEN[@]}" | sort -u)
if [ -n "$forbidden_hits" ]; then
  echo "FAIL: pola terlarang"
  for entry in "${FORBIDDEN[@]}"; do
    key="${entry%%=*}"
    rx="${entry#*=}"
    hits=$(grep -rnE "$rx" --include='*.kt' "$SRC" 2>/dev/null | grep -v 'anr-allow')
    [ -n "$hits" ] && echo "$hits" | sed "s/^/  [$key] /"
  done
  echo
  echo "GlobalScope & Thread.sleep: pakai viewModelScope + delay, jangan menahan thread."
  echo "printStackTrace: lewatkan ke DataResult/ErrorText, jangan ke stderr."
  echo "X-Internal-API-Key: /internal/v1 tidak boleh dipanggil aplikasi nasabah."
  echo
  status=1
fi

if [ ! -f "$BASELINE" ]; then
  mkdir -p "$(dirname "$BASELINE")"
  echo "# baseline pola ANR/bug — jangan diedit tangan" > "$BASELINE"
fi

current=$(scan_baselined)

violations=$(awk '
  NR==FNR { if ($0 !~ /^#/) base[$1" "$2] = $3; next }
  {
    k = $1" "$2
    b = (k in base) ? base[k] : 0
    if ($3 > b) printf "  [%s] %s: %d kemunculan (baseline %d)\n", $1, $2, $3, b
  }
' "$BASELINE" <(echo "$current"))

if [ -n "$violations" ]; then
  echo "FAIL: penambahan pola berisiko"
  echo "$violations"
  echo
  echo "runBlocking: hanya sah di OkHttp Authenticator — di tempat lain ia menahan thread pemanggil."
  echo "!!          : ganti dengan pemeriksaan eksplisit; crash produksi tidak punya pesan error."
  echo "Log.*       : jangan ada data nasabah di logcat. Berkas debug pakai pagar BuildConfig.DEBUG."
  echo "url-literal : alamat server datang dari BuildConfig per flavor, bukan literal di kode."
  echo "Kalau pemakaiannya memang sah, tambahkan '// anr-allow: <alasan>' di baris itu dan jelaskan di PR."
  echo
  status=1
fi

if [ "$status" -eq 0 ]; then
  improved=$(awk '
    NR==FNR { cur[$1" "$2] = $3; next }
    /^#/ { next }
    {
      k = $1" "$2
      c = (k in cur) ? cur[k] : 0
      if (c < $3) printf "  [%s] %s: %d -> %d\n", $1, $2, $3, c
    }
  ' <(echo "$current") "$BASELINE")
  if [ -n "$improved" ]; then
    echo "OK — dan ada perbaikan:"
    echo "$improved"
    echo
    echo "Jalankan --update-baseline lalu commit, supaya perbaikannya terkunci."
  else
    echo "OK: tidak ada pola terlarang dan tidak ada penambahan baru."
  fi
fi

exit "$status"
