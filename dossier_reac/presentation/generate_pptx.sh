#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SLIDES_DIR="$SCRIPT_DIR/slides"
IMG_DIR="$SCRIPT_DIR/mermaid-images"

echo "=== HappyRow PPTX Generator ==="
echo ""

# ── Step 1: Pre-render mermaid diagrams ──────────────────────────────────────

echo "[1/2] Rendering mermaid diagrams to PNG..."
rm -rf "$IMG_DIR"
mkdir -p "$IMG_DIR"

mermaid_counter=0

for mdfile in "$SLIDES_DIR"/*.md; do
    in_mermaid=false
    mermaid_block=""

    while IFS= read -r line || [[ -n "$line" ]]; do
        if [[ "$line" == '```mermaid' ]]; then
            in_mermaid=true
            mermaid_block=""
            continue
        fi

        if $in_mermaid; then
            if [[ "$line" == '```' ]]; then
                in_mermaid=false
                mermaid_counter=$((mermaid_counter + 1))
                mmd_file="$IMG_DIR/diagram_${mermaid_counter}.mmd"
                png_file="$IMG_DIR/diagram_${mermaid_counter}.png"

                echo "$mermaid_block" > "$mmd_file"
                echo "  Rendering diagram $mermaid_counter from $(basename "$mdfile")..."
                mmdc -i "$mmd_file" -o "$png_file" -b transparent -w 1200 -H 600 2>/dev/null || {
                    echo "  Warning: Failed to render diagram $mermaid_counter"
                }
            else
                if [ -n "$mermaid_block" ]; then
                    mermaid_block="$mermaid_block
$line"
                else
                    mermaid_block="$line"
                fi
            fi
        fi
    done < "$mdfile"
done

echo "  → $mermaid_counter diagrams rendered"
echo ""

# ── Step 2: Build PPTX with python-pptx ─────────────────────────────────────

echo "[2/2] Building PPTX from template..."
python3 "$SCRIPT_DIR/build_pptx.py" "$@"

echo ""
echo "=== Done! ==="
ls -lh "$SCRIPT_DIR/HappyRow.pptx"
