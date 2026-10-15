#!/usr/bin/env bash
# Gerbang arah pull request — menolak PR yang menuju branch yang salah.
#
#   ./scripts/check-pr-branch-policy.sh <branch-asal> <branch-tujuan> [sha-asal]
#
# Sumber aturannya .claude/skills/git-branch-workflow/SKILL.md §Pull request:
#
#   working/ , fix/   -> feature/<nama>  atau development
#   feature/<nama>    -> development dulu, baru main
#   hotfix/<nama>     -> main (satu-satunya jalan pintas), lalu merge balik
#
# PR ke `main` dari branch yang commit-nya belum ada di `development` ditolak:
# yang diuji di development adalah commit, bukan niat.

set -uo pipefail

HEAD_REF="${1:?branch asal}"
BASE_REF="${2:?branch tujuan}"
HEAD_SHA="${3:-$(git rev-parse HEAD 2>/dev/null || echo "")}"

fail() {
  echo "FAIL: $1"
  shift
  for line in "$@"; do echo "      $line"; done
  exit 1
}

echo "PR: $HEAD_REF -> $BASE_REF"

[ "$HEAD_REF" = "$BASE_REF" ] && fail "branch asal dan tujuan sama."

case "$HEAD_REF" in
  main)
    fail "PR tidak boleh berasal dari 'main'." \
         "main hanya bertambah lewat merge, tidak pernah jadi sumber pekerjaan." \
         "Pindahkan pekerjaan ke working/<nama> lalu buka PR dari sana."
    ;;
  development)
    fail "PR tidak boleh berasal dari 'development'." \
         "development itu staging bercampur — isinya beberapa feature sekaligus," \
         "termasuk yang belum diputuskan ikut rilis. Yang masuk main adalah feature/."
    ;;
esac

case "$BASE_REF" in
  main)
    case "$HEAD_REF" in
      hotfix/*)
        echo "OK: hotfix/ boleh langsung ke main."
        echo "    Setelah merge: merge balik ke development dan ke setiap feature/ yang masih hidup,"
        echo "    kalau tidak perbaikannya kambuh di rilis berikutnya."
        ;;
      feature/*)
        if ! git fetch --quiet --no-tags origin development 2>/dev/null; then
          fail "branch 'development' belum ada di origin." \
               "Buat sekali dari main, merge feature/ ke sana, uji di sana," \
               "baru buka PR ke main."
        fi
        if [ -z "$HEAD_SHA" ]; then
          fail "sha branch asal tidak diketahui, tidak bisa membuktikan sudah lewat development."
        fi
        if git merge-base --is-ancestor "$HEAD_SHA" FETCH_HEAD; then
          echo "OK: $HEAD_SHA sudah ada di development — pengujian staging-nya mencakup commit ini."
        else
          fail "commit $HEAD_SHA belum ada di 'development'." \
               "PR ke main tidak boleh dibuka sebelum commit yang sama diuji di development." \
               "Urutannya: PR $HEAD_REF -> development, uji di sana, baru PR $HEAD_REF -> main." \
               "Kalau feature/ ini mendapat commit baru setelah diuji, ia harus kembali ke development dulu."
        fi
        ;;
      *)
        fail "PR ke main hanya boleh dari feature/ atau hotfix/." \
             "Branch '$HEAD_REF' harus menuju development (jalur pendek) atau feature/<nama> (jalur panjang)." \
             "Jangan pernah membuka PR ke main langsung — perlu diuji di development dulu."
        ;;
    esac
    ;;
  development)
    case "$HEAD_REF" in
      feature/*|working/*|fix/*|hotfix/*)
        echo "OK: $HEAD_REF menuju staging. Diuji di sana sebelum ada yang menuju main."
        ;;
      *)
        fail "nama branch '$HEAD_REF' tidak dikenal." \
             "Prefix yang sah: feature/, working/, fix/, hotfix/ — lihat skill git-branch-workflow §Penamaan."
        ;;
    esac
    ;;
  feature/*)
    case "$HEAD_REF" in
      working/*|fix/*)
        echo "OK: fase $HEAD_REF kembali ke payungnya."
        ;;
      *)
        fail "'$BASE_REF' hanya menerima working/ atau fix/." \
             "feature/ tidak bercabang dari feature/ lain."
        ;;
    esac
    ;;
  *)
    fail "branch tujuan '$BASE_REF' tidak dikenal." \
         "Tujuan yang sah: feature/<nama>, development, atau main (khusus feature/ dan hotfix/)."
    ;;
esac
