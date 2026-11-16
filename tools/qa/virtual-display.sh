#!/bin/sh
set -eu
export DISPLAY=:99
export ALSOFT_DRIVERS=null
Xvfb "$DISPLAY" -screen 0 1280x720x24 +extension GLX -nolisten tcp -ac &
display_pid=$!
trap 'kill "$display_pid" 2>/dev/null || true' EXIT
while [ ! -S /tmp/.X11-unix/X99 ]; do sleep 0.1; done
exec python3 /task/tools/qa/launch.py "$@"
