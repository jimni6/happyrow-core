#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SLIDES_DIR="$SCRIPT_DIR/slides"
IMG_DIR="$SCRIPT_DIR/mermaid-images"
OUTPUT_MD="$SCRIPT_DIR/presentation.md"
OUTPUT_PPTX="$SCRIPT_DIR/HappyRow.pptx"
REFERENCE="$SCRIPT_DIR/reference.pptx"
SCRIPT_FILE="$SCRIPT_DIR/00_SCRIPT_COMPLET.md"

echo "=== HappyRow PPTX Generator ==="
echo ""

# ── Step 1: Pre-render mermaid diagrams ──────────────────────────────────────

echo "[1/3] Rendering mermaid diagrams to PNG..."
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

# ── Step 2: Build unified pandoc markdown ────────────────────────────────────

echo "[2/3] Building unified presentation.md..."

# Extract speaker notes
NOTES_DIR="$SCRIPT_DIR/.notes_tmp"
rm -rf "$NOTES_DIR"
mkdir -p "$NOTES_DIR"

if [ -f "$SCRIPT_FILE" ]; then
    current_slide=""
    while IFS= read -r line || [[ -n "$line" ]]; do
        if [[ "$line" =~ ^###\ Slide\ ([0-9]+) ]]; then
            current_slide="${BASH_REMATCH[1]}"
            : > "$NOTES_DIR/slide_${current_slide}.txt"
            continue
        fi
        if [ -n "$current_slide" ]; then
            [[ "$line" == ">"* ]] && continue
            [[ "$line" == "---" ]] && continue
            [[ "$line" == "###"* ]] && continue
            [[ "$line" == "##"* ]] && continue
            [ -n "$line" ] && echo "$line" >> "$NOTES_DIR/slide_${current_slide}.txt"
        fi
    done < "$SCRIPT_FILE"
    notes_count=$(ls "$NOTES_DIR"/slide_*.txt 2>/dev/null | wc -l | tr -d ' ')
    echo "  Extracted speaker notes for $notes_count slides"
fi

# Write pandoc header (title slide)
cat > "$OUTPUT_MD" << 'HEADER'
---
title: "HappyRow"
subtitle: "Plateforme collaborative de gestion d'événements — Titre Professionnel CDA"
author: "[Nom Prénom]"
date: "[Date de session]"
---

HEADER

# Helper: append speaker notes for a slide
append_notes() {
    local slide_num="$1"
    if [ -n "$slide_num" ] && [ -f "$NOTES_DIR/slide_${slide_num}.txt" ]; then
        echo "" >> "$OUTPUT_MD"
        echo "::: notes" >> "$OUTPUT_MD"
        cat "$NOTES_DIR/slide_${slide_num}.txt" >> "$OUTPUT_MD"
        echo "" >> "$OUTPUT_MD"
        echo ":::" >> "$OUTPUT_MD"
    fi
}

mermaid_idx=0
MAX_CODE_LINES=18

for mdfile in "$SLIDES_DIR"/*.md; do
    in_mermaid=false
    in_code=false
    code_lang=""
    code_lines=0
    code_truncated=false
    current_slide_num=""

    while IFS= read -r line || [[ -n "$line" ]]; do
        # Skip HTML comments
        [[ "$line" == "<!--"* ]] && continue

        # Skip standalone --- separators
        [[ "$line" == "---" ]] && continue

        # Skip instruction blockquotes (> *[Insérer...)
        [[ "$line" == "> *["* ]] && continue
        [[ "$line" == ">*["* ]] && continue

        # ── Mermaid blocks → image ──
        if [[ "$line" == '```mermaid' ]]; then
            in_mermaid=true
            mermaid_idx=$((mermaid_idx + 1))
            continue
        fi
        if $in_mermaid; then
            [[ "$line" == '```' ]] && {
                in_mermaid=false
                echo "" >> "$OUTPUT_MD"
                echo "![Diagramme](mermaid-images/diagram_${mermaid_idx}.png){ width=80% }" >> "$OUTPUT_MD"
                echo "" >> "$OUTPUT_MD"
            }
            continue
        fi

        # ── Code blocks: pass through but truncate if too long ──
        if [[ "$line" =~ ^\`\`\`(.+)$ ]] && ! $in_code; then
            in_code=true
            code_lang="${BASH_REMATCH[1]}"
            code_lines=0
            code_truncated=false
            echo "$line" >> "$OUTPUT_MD"
            continue
        fi
        if [[ "$line" == '```' ]] && $in_code; then
            in_code=false
            if $code_truncated; then
                echo "// ..." >> "$OUTPUT_MD"
            fi
            echo '```' >> "$OUTPUT_MD"
            continue
        fi
        if $in_code; then
            code_lines=$((code_lines + 1))
            if [ $code_lines -le $MAX_CODE_LINES ]; then
                echo "$line" >> "$OUTPUT_MD"
            elif [ $code_lines -eq $((MAX_CODE_LINES + 1)) ]; then
                code_truncated=true
            fi
            continue
        fi

        # ── Section headers: # Title → skip (avoid extra section divider slides) ──
        if [[ "$line" =~ ^#\ (.+)$ ]] && [[ ! "$line" =~ ^##\  ]]; then
            # Flush notes for previous slide
            append_notes "$current_slide_num"
            current_slide_num=""
            continue
        fi

        # ── Slide headers: ## Slide N — Title (timing) → ## Title ──
        if [[ "$line" =~ ^##\ Slide\ ([0-9]+)\ —\ (.+)\ \( ]]; then
            append_notes "$current_slide_num"
            current_slide_num="${BASH_REMATCH[1]}"
            echo "" >> "$OUTPUT_MD"
            echo "## ${BASH_REMATCH[2]}" >> "$OUTPUT_MD"
            echo "" >> "$OUTPUT_MD"
            continue
        fi
        if [[ "$line" =~ ^##\ Slide\ ([0-9]+)\ —\ (.+)$ ]]; then
            append_notes "$current_slide_num"
            current_slide_num="${BASH_REMATCH[1]}"
            echo "" >> "$OUTPUT_MD"
            echo "## ${BASH_REMATCH[2]}" >> "$OUTPUT_MD"
            echo "" >> "$OUTPUT_MD"
            continue
        fi

        # ── Sub-headers ### → bold paragraph (avoids extra whitespace) ──
        if [[ "$line" =~ ^###\ (.+)$ ]]; then
            echo "" >> "$OUTPUT_MD"
            echo "**${BASH_REMATCH[1]}**" >> "$OUTPUT_MD"
            echo "" >> "$OUTPUT_MD"
            continue
        fi

        # ── Pass through everything else ──
        echo "$line" >> "$OUTPUT_MD"

    done < "$mdfile"

    append_notes "$current_slide_num"
done

rm -rf "$NOTES_DIR"

slide_count=$(grep -c "^## " "$OUTPUT_MD" || true)
echo "  → presentation.md: $slide_count content slides"
echo ""

# ── Step 3: Generate PPTX ───────────────────────────────────────────────────

echo "[3/3] Generating PPTX with pandoc..."

pandoc "$OUTPUT_MD" \
    -o "$OUTPUT_PPTX" \
    --reference-doc="$REFERENCE" \
    -s \
    --slide-level=2

echo ""

# ── Step 4: Post-process — remove overflow slides & apply styling ────────────

echo "[4/4] Post-processing: removing overflow slides & styling..."
python3 "$SCRIPT_DIR/cleanup_pptx.py" "$OUTPUT_PPTX" "$OUTPUT_PPTX"

echo ""
echo "=== Done! ==="
echo "Output: $OUTPUT_PPTX"
ls -lh "$OUTPUT_PPTX"
