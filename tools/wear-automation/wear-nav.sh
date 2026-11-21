#!/usr/bin/env bash
# Navigation helpers built on the debug accessibility bridge.
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT" || exit 1
UI="bash tools/wear-automation/wear-ui.sh"

tree() { $UI tree; }

top_texts() {
  $UI tree | python -c "import sys,json; d=json.load(sys.stdin); print(' | '.join([n['text'] for n in d['nodes'] if n['text']][:8]))"
}

has_text() { $UI tree | grep -q "\"text\":\"$1\""; }

goto_top() {
  for _ in $(seq 1 14); do $UI scroll backward >/dev/null 2>&1; done
}

open_page() {
  local name="$1"
  goto_top
  for _ in $(seq 1 22); do
    if has_text "$name"; then
      $UI click "$name" >/dev/null
      sleep 1.8
      printf '%s => ' "$name"
      top_texts
      return 0
    fi
    $UI scroll forward >/dev/null 2>&1
    sleep 0.15
  done
  echo "$name => NOT FOUND"
  return 1
}

back() { $UI back >/dev/null 2>&1; sleep 1.2; }
wake() { $UI wake >/dev/null 2>&1; }
