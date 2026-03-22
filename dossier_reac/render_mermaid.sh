#!/bin/bash
set -e

DOSSIER="dossier_reac"
IMG_DIR="$DOSSIER/mermaid-images"
PROCESSED_DIR="$DOSSIER/_processed"

rm -rf "$IMG_DIR" "$PROCESSED_DIR"
mkdir -p "$IMG_DIR" "$PROCESSED_DIR"

counter=0

for mdfile in "$DOSSIER"/0*.md "$DOSSIER"/1*.md; do
    filename=$(basename "$mdfile")
    outfile="$PROCESSED_DIR/$filename"

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
                counter=$((counter + 1))
                mmd_file="$IMG_DIR/diagram_${counter}.mmd"
                png_file="$IMG_DIR/diagram_${counter}.png"

                echo "$mermaid_block" > "$mmd_file"
                echo "Rendering diagram $counter from $filename..."
                mmdc -i "$mmd_file" -o "$png_file" -b transparent -w 1200 2>/dev/null

                echo "![Diagramme $counter]($IMG_DIR/diagram_${counter}.png)" >> "$outfile"
                echo "" >> "$outfile"
            else
                if [ -n "$mermaid_block" ]; then
                    mermaid_block="$mermaid_block
$line"
                else
                    mermaid_block="$line"
                fi
            fi
        else
            echo "$line" >> "$outfile"
        fi
    done < "$mdfile"
done

echo ""
echo "Done! $counter diagrams rendered."
echo "Processed files in $PROCESSED_DIR/"
ls -la "$IMG_DIR"/*.png 2>/dev/null || echo "No PNG files found"
