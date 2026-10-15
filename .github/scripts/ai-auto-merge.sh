#!/usr/bin/env bash
# Auto-merge PR yang lulus seluruh review — dipanggil hanya oleh
# .github/workflows/ai-auto-merge.yml setelah "AI PR Review" selesai `success`.
#
# Dibuat berupa skrip, bukan blok YAML, supaya bisa dibaca dan diuji tanpa
# memicu workflow. Setiap `lewat` di bawah adalah alasan TIDAK merge; defaultnya
# tidak merge, dan merge hanya terjadi kalau semua pagar dilewati.
#
# Butuh env: GH_TOKEN, REPO, SHA (head sha dari run review), METODE (merge|squash|rebase)

set -uo pipefail

: "${GH_TOKEN:?}"
: "${REPO:?}"
: "${SHA:?}"
METODE="${METODE:-merge}"

catat() { echo "$1"; [ -n "${GITHUB_STEP_SUMMARY:-}" ] && echo "$1" >> "$GITHUB_STEP_SUMMARY"; }
lewat() { catat "TIDAK merge: $1"; exit 0; }

PR=$(gh api "repos/$REPO/commits/$SHA/pulls" \
       --jq '.[] | select(.state == "open") | .number' 2>/dev/null | head -1)
[ -n "$PR" ] || lewat "tidak ada PR terbuka untuk commit $SHA."

data=$(gh pr view "$PR" --repo "$REPO" \
         --json number,title,isDraft,baseRefName,headRefName,headRefOid,labels,reviewDecision,mergeable,state)

ambil() { echo "$data" | jq -r "$1"; }

STATE=$(ambil '.state')
DRAFT=$(ambil '.isDraft')
BASE=$(ambil '.baseRefName')
HEAD=$(ambil '.headRefName')
HEAD_OID=$(ambil '.headRefOid')
LABELS=$(ambil '[.labels[].name] | join(",")')
REVIEW=$(ambil '.reviewDecision // ""')
MERGEABLE=$(ambil '.mergeable')
TITLE=$(ambil '.title')

catat "### PR #$PR — $TITLE"
catat ""
catat "\`$HEAD\` -> \`$BASE\`, label: \`${LABELS:-(tidak ada)}\`"
catat ""

punya_label() { echo ",$LABELS," | grep -q ",$1,"; }

# --- pagar 1: PR masih layak disentuh -------------------------------------
[ "$STATE" = "OPEN" ] || lewat "PR sudah $STATE."
[ "$DRAFT" = "false" ] || lewat "PR masih draft."
[ "$HEAD_OID" = "$SHA" ] || lewat "ada commit baru sejak review ($HEAD_OID != $SHA). Review ulang dulu."

# --- pagar 2: arah branch --------------------------------------------------
# main TIDAK PERNAH di-merge otomatis. Rilis ke nasabah adalah keputusan manusia,
# dan aturannya: yang masuk main hanya feature/ yang sudah lulus uji di development.
case "$BASE" in
  main)
    gh label create "ai-review/siap-rilis" --repo "$REPO" --color 1D76DB \
      --description "Lulus seluruh review; merge ke main menunggu keputusan manusia" 2>/dev/null || true
    gh pr edit "$PR" --repo "$REPO" --add-label "ai-review/siap-rilis" > /dev/null 2>&1 || true
    MARKER="<!-- ai-auto-merge:siap-rilis -->"
    sudah=$(gh api "repos/$REPO/issues/$PR/comments" --paginate \
              --jq ".[] | select(.body | contains(\"$MARKER\")) | .id" 2>/dev/null | head -1)
    if [ -z "$sudah" ]; then
      gh api -X POST "repos/$REPO/issues/$PR/comments" -f body="$MARKER
**Siap rilis — tapi tidak saya merge.**

Seluruh gerbang hijau dan review AI bersih. PR ini menuju \`main\`, jadi
merge-nya tetap keputusan manusia: yang masuk \`main\` adalah rilis ke nasabah.

Sebelum menekan merge, pastikan commit ini sudah diuji di \`development\`
(gerbang arah branch sudah memeriksa bahwa commit-nya ada di sana).

<sub>Auto-merge hanya berlaku untuk PR menuju \`development\` dan \`feature/*\`.</sub>" > /dev/null 2>&1 || true
    fi
    lewat "tujuannya main — diberi label siap-rilis, merge-nya manual."
    ;;
  development) ;;
  feature/*) ;;
  *) lewat "branch tujuan \`$BASE\` tidak termasuk cakupan auto-merge." ;;
esac

# --- pagar 3: label dan review manusia -------------------------------------
punya_label "ai-review/pass" || lewat "label ai-review/pass tidak ada."
for tahan in "ai-review/blocked" "do-not-merge" "wip" "hold" "jangan-merge"; do
  punya_label "$tahan" && lewat "ada label \`$tahan\`."
done
[ "$REVIEW" = "CHANGES_REQUESTED" ] && lewat "ada reviewer yang meminta perubahan."

# --- pagar 4: seluruh check lain hijau -------------------------------------
checks=$(gh api "repos/$REPO/commits/$SHA/check-runs?per_page=100" \
           --jq '.check_runs[] | select(.name != "Auto-merge oleh AI") | "\(.status)|\(.conclusion // "")|\(.name)"' 2>/dev/null)
belum=$(echo "$checks" | awk -F'|' '$1 != "completed" { print $3 }')
[ -n "$belum" ] && lewat "masih ada check berjalan: $(echo "$belum" | tr '\n' ' ')"
gagal=$(echo "$checks" | awk -F'|' '$2 == "failure" || $2 == "cancelled" || $2 == "timed_out" || $2 == "action_required" { print $3 }')
[ -n "$gagal" ] && lewat "ada check gagal: $(echo "$gagal" | tr '\n' ' ')"

# --- pagar 5: bisa di-merge -----------------------------------------------
[ "$MERGEABLE" = "CONFLICTING" ] && lewat "ada konflik dengan \`$BASE\`."

# --- merge ----------------------------------------------------------------
catat "Semua pagar lewat. Merge dengan metode \`$METODE\`."

PESAN="Merge otomatis setelah seluruh review lulus

Gerbang: arah branch, unit test, lint, literal visual, pola ANR, review logika AI.
Tidak ada temuan blocker/major. Dasar putusan ada di komentar review pada PR ini."

# --subject/--body hanya sah untuk merge & squash; rebase menolaknya.
case "$METODE" in
  rebase) ARGS_PESAN=() ;;
  *)      ARGS_PESAN=(--subject "Merge PR #$PR: $TITLE" --body "$PESAN") ;;
esac

if gh pr merge "$PR" --repo "$REPO" "--$METODE" "${ARGS_PESAN[@]+${ARGS_PESAN[@]}}"; then
  catat ""
  catat "**Merged.** \`$HEAD\` -> \`$BASE\`."
  gh api -X POST "repos/$REPO/issues/$PR/comments" -f body="<!-- ai-auto-merge:merged -->
**Di-merge otomatis.** Seluruh gerbang hijau dan review AI tidak menemukan
temuan blocker/major.

Branch \`$HEAD\` tidak dihapus — kalau ia \`working/\`, isinya masih dibutuhkan
sebagai riwayat fase. Hapus sendiri kalau sudah tidak dipakai." > /dev/null 2>&1 || true
else
  catat ""
  catat "Merge langsung ditolak GitHub (kemungkinan branch protection). Mencoba mengaktifkan auto-merge GitHub."
  if gh pr merge "$PR" --repo "$REPO" "--$METODE" --auto; then
    catat "Auto-merge GitHub diaktifkan; PR akan merge sendiri begitu syarat protection terpenuhi."
  else
    catat "Tidak bisa merge dan tidak bisa mengaktifkan auto-merge. Perlu tangan manusia."
    exit 1
  fi
fi
