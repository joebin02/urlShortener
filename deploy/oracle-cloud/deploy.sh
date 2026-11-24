#!/usr/bin/env bash
# Run from the cloned project on the Oracle VM to update and restart the stack.
set -euo pipefail
repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$repo_root"
git pull --ff-only
sudo docker compose up -d --build
sudo docker compose ps
