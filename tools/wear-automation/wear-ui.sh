#!/usr/bin/env bash
# Host-side helpers for the debug-only accessibility bridge (WearAutomationService).
# Requires: adb forward tcp:8096 tcp:8096, and the service enabled on the watch.
set -euo pipefail
export ADB="${ADB:-/d/Android_Studio/platform-tools/adb.exe}"
export SERIAL="${SERIAL:-10.10.120.2:5555}"
BASE="http://127.0.0.1:8096"

enc() { python -c 'import sys,urllib.parse; print(urllib.parse.quote(sys.argv[1]))' "$1"; }

case "${1:-}" in
  tree) curl -sS -m 10 "$BASE/tree" ;;
  click) curl -sS -m 15 -X POST "$BASE/click?text=$(enc "$2")" ;;
  clickdesc) curl -sS -m 15 -X POST "$BASE/click?desc=$(enc "$2")" ;;
  settext) curl -sS -m 15 -X POST "$BASE/settext?value=$(enc "${2:-}")${3:+&label=$(enc "$3")}" ;;
  scroll) curl -sS -m 15 -X POST "$BASE/scroll?dir=${2:-forward}" ;;
  back) curl -sS -m 15 -X POST "$BASE/back" ;;
  tap) curl -sS -m 15 -X POST "$BASE/tap?x=${2}&y=${3}" ;;
  start) "$ADB" -s "$SERIAL" shell am start -n me.rerere.rikkahub.debug/me.rerere.rikkahub.wear.WearRouteActivity >/dev/null && echo started ;;
  wake) "$ADB" -s "$SERIAL" shell input keyevent 224 >/dev/null && echo awake ;;
  screenshot)
    out="${2:-build/screenshots/auto.png}"
    "$ADB" -s "$SERIAL" exec-out screencap -p > "$out" && echo "$out" ;;
  *) echo "usage: $0 tree|click|clickdesc|settext|scroll|back|tap|start|wake|screenshot" >&2; exit 2 ;;
esac
