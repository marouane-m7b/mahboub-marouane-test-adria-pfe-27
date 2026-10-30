#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
python3 generate_report.py
if command -v tectonic >/dev/null 2>&1; then
    tectonic --keep-logs rapport_adria.tex
elif [[ -x ../.tools/tectonic/tectonic ]]; then
    ../.tools/tectonic/tectonic --keep-logs rapport_adria.tex
elif command -v pdflatex >/dev/null 2>&1; then
    pdflatex -interaction=nonstopmode -halt-on-error rapport_adria.tex
    pdflatex -interaction=nonstopmode -halt-on-error rapport_adria.tex
else
    echo 'Installer Tectonic ou une distribution LaTeX pour compiler le rapport.' >&2
    exit 1
fi
