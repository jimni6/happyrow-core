from __future__ import annotations

import colorsys
import io
import os
import re
import shutil
import tempfile
import zipfile
from pathlib import Path
from typing import Iterable
from xml.etree import ElementTree as ET

from PIL import Image, ImageDraw, ImageFont


SCRIPT_DIR = Path(__file__).resolve().parent
BASE_TEMPLATE = SCRIPT_DIR / "Betclic_GOAT-Template.pptx"
HAPPYROW_SOURCE = SCRIPT_DIR / "happyrow_template_to_process.pptx"
OUTPUT_TEMPLATE = SCRIPT_DIR / "HappyRow_Reusable_Template.pptx"

PML_NS = "http://schemas.openxmlformats.org/presentationml/2006/main"
DRAWING_NS = "http://schemas.openxmlformats.org/drawingml/2006/main"
REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships"

ET.register_namespace("a", DRAWING_NS)
ET.register_namespace("p", PML_NS)
ET.register_namespace("r", "http://schemas.openxmlformats.org/officeDocument/2006/relationships")

NS = {"a": DRAWING_NS, "p": PML_NS, "pr": REL_NS}


HAPPYROW_COLORS = {
    "dk1": "1F2D37",
    "lt1": "FFFFFF",
    "dk2": "3D5A6C",
    "lt2": "F8F9FA",
    "accent1": "5FBDB4",
    "accent2": "E6A19A",
    "accent3": "3D5A6C",
    "accent4": "7DD3CB",
    "accent5": "F5E8E4",
    "accent6": "2B5C73",
    "hlink": "2B5C73",
    "folHlink": "5FBDB4",
}

NAVY = (61, 90, 108)
DEEP_NAVY = (31, 45, 55)
TEAL = (95, 189, 180)
TEAL_ALT = (92, 186, 178)
CORAL = (230, 161, 154)
OFF_WHITE = (248, 249, 250)

LAYOUT_NAME_OVERRIDES = {
    "COVER": "HR - Cover",
    "WELCOME": "HR - Section Welcome",
    "SECTION": "HR - Section Number",
    "CHAPTER": "HR - Chapter",
    "1 BLOCK white": "HR - Content White",
    "1 BLOCK lines 1": "HR - Content Lines 1",
    "1 BLOCK lines 2": "HR - Content Lines 2",
    "1 BLOCK lines 3": "HR - Content Lines 3",
    "1 BLOCK grey": "HR - Content Grey",
    "2 BLOCKS left": "HR - Two Blocks Left",
    "2 BLOCKS right": "HR - Two Blocks Right",
    "CONTENT summary": "HR - Summary",
    "CONTENT left tabs": "HR - Content Left Tabs",
    "CONTENT timeline": "HR - Timeline",
    "CONTENT world": "HR - World / Map",
    "HIGHLIGHT free 1": "HR - Highlight Free 1",
    "HIGHLIGHT free 2": "HR - Highlight Free 2",
    "HIGHLIGHT free 3": "HR - Highlight Free 3",
    "HIGHLIGHT app 1": "HR - Highlight App 1",
    "HIGHLIGHT app 2": "HR - Highlight App 2",
    "HIGHLIGHT important": "HR - Highlight Important",
    "COLORED red 1": "HR - Colored Accent 1",
    "COLORED red 2": "HR - Colored Accent 2",
    "COLORED yellow 1": "HR - Colored Soft 1",
    "COLORED yellow 2": "HR - Colored Soft 2",
    "COLORED dark": "HR - Colored Dark",
    "Q&A": "HR - Q&A",
    "THANKS - END": "HR - Thanks / End",
}

STRING_REPLACEMENTS = {
    "Betclic Medium": "Arial Rounded MT Bold",
    "Betclic Light": "Arial",
    "Betclic ExtraBold": "Arial Bold",
    "Betclic Condensed Condensed": "Arial Narrow",
    "Betclic": "HappyRow",
    "E10014E": "HappyRowPalette",
    "CONFIDENTIAL DOCUMENT": "HAPPYROW",
    "12/09/2025": "JJ/MM/AAAA",
    "BEM-VINDO": "HELLO",
    "BIENVENUE": "WELCOME",
    "POWITANIE": "INTRO",
    "MBOTE": "HAPPYROW",
    "OBRIGADO": "THANK YOU",
    "DZIĘKUJĘ": "MERCI",
    "CHAPTER SLIDE": "SECTION TITLE",
}

HEX_REPLACEMENTS = {
    "E10014": "5FBDB4",
    "FFC000": "E6A19A",
    "1A1F2A": "3D5A6C",
    "424242": "547C8C",
    "34AE7D": "7DD3CB",
    "FAA541": "E6A19A",
    "0E2841": "3D5A6C",
    "E8E9EB": "F8F9FA",
}


def main() -> None:
    if not BASE_TEMPLATE.exists():
        raise FileNotFoundError(f"Base template not found: {BASE_TEMPLATE}")
    if not HAPPYROW_SOURCE.exists():
        raise FileNotFoundError(f"HappyRow source deck not found: {HAPPYROW_SOURCE}")

    with tempfile.TemporaryDirectory(prefix="happyrow_template_build_") as tmp_dir:
        tmp_path = Path(tmp_dir)
        unpacked_dir = tmp_path / "base"
        with zipfile.ZipFile(BASE_TEMPLATE) as source_zip:
            source_zip.extractall(unpacked_dir)

        with zipfile.ZipFile(HAPPYROW_SOURCE) as happyrow_zip:
            happyrow_icon = Image.open(io.BytesIO(happyrow_zip.read("ppt/media/image2.png"))).convert("RGBA")

        remove_embedded_fonts(unpacked_dir)
        update_theme(unpacked_dir)
        update_presentation_xml(unpacked_dir)
        update_layout_names(unpacked_dir)
        replace_strings_in_xml(unpacked_dir)
        replace_brand_images(unpacked_dir, happyrow_icon)
        recolor_media(unpacked_dir)

        if OUTPUT_TEMPLATE.exists():
            OUTPUT_TEMPLATE.unlink()
        zip_directory(unpacked_dir, OUTPUT_TEMPLATE)

    print(f"Created {OUTPUT_TEMPLATE}")


def remove_embedded_fonts(root_dir: Path) -> None:
    presentation_xml = root_dir / "ppt" / "presentation.xml"
    tree = ET.parse(presentation_xml)
    root = tree.getroot()
    root.attrib["embedTrueTypeFonts"] = "0"

    embedded_font_list = root.find(f"{{{PML_NS}}}embeddedFontLst")
    if embedded_font_list is not None:
        root.remove(embedded_font_list)

    tree.write(presentation_xml, encoding="utf-8", xml_declaration=True)

    rels_path = root_dir / "ppt" / "_rels" / "presentation.xml.rels"
    rels_tree = ET.parse(rels_path)
    rels_root = rels_tree.getroot()
    to_remove = [
        rel
        for rel in list(rels_root)
        if rel.attrib.get("Type", "").endswith("/font")
    ]
    for rel in to_remove:
        rels_root.remove(rel)
    rels_tree.write(rels_path, encoding="utf-8", xml_declaration=True)

    fonts_dir = root_dir / "ppt" / "fonts"
    if fonts_dir.exists():
        shutil.rmtree(fonts_dir)


def update_theme(root_dir: Path) -> None:
    theme_path = root_dir / "ppt" / "theme" / "theme1.xml"
    tree = ET.parse(theme_path)
    root = tree.getroot()

    theme_name = root.attrib.get("name", "")
    if theme_name:
        root.attrib["name"] = "HappyRow Theme"

    color_scheme = root.find(".//a:themeElements/a:clrScheme", NS)
    if color_scheme is not None:
        color_scheme.attrib["name"] = "HappyRow"
        for child in list(color_scheme):
            tag = child.tag.split("}")[-1]
            if tag in HAPPYROW_COLORS and len(child):
                list(child)[0].attrib["val"] = HAPPYROW_COLORS[tag]

    font_scheme = root.find(".//a:themeElements/a:fontScheme", NS)
    if font_scheme is not None:
        font_scheme.attrib["name"] = "HappyRow"
        major_latin = font_scheme.find("./a:majorFont/a:latin", NS)
        minor_latin = font_scheme.find("./a:minorFont/a:latin", NS)
        if major_latin is not None:
            major_latin.attrib["typeface"] = "Arial Rounded MT Bold"
        if minor_latin is not None:
            minor_latin.attrib["typeface"] = "Arial"

    tree.write(theme_path, encoding="utf-8", xml_declaration=True)


def update_presentation_xml(root_dir: Path) -> None:
    presentation_xml = root_dir / "ppt" / "presentation.xml"
    tree = ET.parse(presentation_xml)
    root = tree.getroot()
    default_text_style = root.find(f"{{{PML_NS}}}defaultTextStyle")
    if default_text_style is not None:
        for latin_node in default_text_style.findall(".//a:latin", NS):
            typeface = latin_node.attrib.get("typeface")
            if typeface == "+mn-lt":
                latin_node.attrib["typeface"] = "Arial"
            elif typeface == "+mj-lt":
                latin_node.attrib["typeface"] = "Arial Rounded MT Bold"
    tree.write(presentation_xml, encoding="utf-8", xml_declaration=True)


def update_layout_names(root_dir: Path) -> None:
    layout_dir = root_dir / "ppt" / "slideLayouts"
    for layout_path in layout_dir.glob("slideLayout*.xml"):
        tree = ET.parse(layout_path)
        root = tree.getroot()
        csld = root.find(f"{{{PML_NS}}}cSld")
        if csld is None:
            continue
        name = csld.attrib.get("name")
        if not name:
            continue
        csld.attrib["name"] = LAYOUT_NAME_OVERRIDES.get(name, f"HR - {name}")
        tree.write(layout_path, encoding="utf-8", xml_declaration=True)


def replace_strings_in_xml(root_dir: Path) -> None:
    for xml_path in root_dir.rglob("*.xml"):
        content = xml_path.read_text(encoding="utf-8", errors="ignore")
        updated = content
        for old, new in STRING_REPLACEMENTS.items():
            updated = updated.replace(old, new)
        for old, new in HEX_REPLACEMENTS.items():
            updated = updated.replace(old, new)
        if updated != content:
            xml_path.write_text(updated, encoding="utf-8")


def replace_brand_images(root_dir: Path, happyrow_icon: Image.Image) -> None:
    media_dir = root_dir / "ppt" / "media"

    cover_logo_path = media_dir / "image2.png"
    if cover_logo_path.exists():
        base_image = Image.open(cover_logo_path).convert("RGBA")
        build_cover_wordmark(base_image.size, happyrow_icon).save(cover_logo_path)

    side_brand_path = media_dir / "image4.png"
    if side_brand_path.exists():
        base_image = Image.open(side_brand_path).convert("RGBA")
        build_vertical_strip(base_image.size, happyrow_icon).save(side_brand_path)


def recolor_media(root_dir: Path) -> None:
    media_dir = root_dir / "ppt" / "media"
    for image_path in sorted(media_dir.glob("*.png")):
        if image_path.name in {"image2.png", "image4.png"}:
            continue
        recolored = recolor_png(Image.open(image_path).convert("RGBA"))
        recolored.save(image_path)


def build_cover_wordmark(size: tuple[int, int], happyrow_icon: Image.Image) -> Image.Image:
    width, height = size
    canvas = Image.new("RGBA", size, (0, 0, 0, 0))
    icon = trim_transparent(happyrow_icon)
    target_height = int(height * 0.72)
    icon_ratio = icon.width / icon.height
    icon_size = (int(target_height * icon_ratio), target_height)
    icon = icon.resize(icon_size, Image.Resampling.LANCZOS)

    font_path = choose_font(
        [
            "/System/Library/Fonts/Supplemental/Arial Rounded Bold.ttf",
            "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
            "/System/Library/Fonts/Supplemental/Comic Sans MS Bold.ttf",
        ]
    )
    draw = ImageDraw.Draw(canvas)
    font_size = int(height * 0.56)
    font = load_font(font_path, font_size)
    text = "HappyRow"

    while True:
        bbox = draw.textbbox((0, 0), text, font=font)
        total_width = icon.width + 24 + (bbox[2] - bbox[0])
        if total_width <= width - 32 or font_size <= 36:
            break
        font_size -= 4
        font = load_font(font_path, font_size)

    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]
    total_width = icon.width + 24 + text_width
    start_x = (width - total_width) // 2
    icon_y = (height - icon.height) // 2
    text_y = (height - text_height) // 2 - 6

    canvas.alpha_composite(icon, (start_x, icon_y))
    text_x = start_x + icon.width + 24
    draw.text((text_x, text_y), text, font=font, fill=(255, 255, 255, 255))

    underline_y = text_y + text_height + 6
    draw.rounded_rectangle(
        [(text_x, underline_y), (text_x + text_width, underline_y + 8)],
        radius=4,
        fill=CORAL + (255,),
    )
    return canvas


def build_vertical_strip(size: tuple[int, int], happyrow_icon: Image.Image) -> Image.Image:
    width, height = size
    canvas = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)

    draw.rectangle([(0, 0), (width, height)], fill=NAVY + (255,))
    draw.rectangle([(0, height - 18), (width, height)], fill=CORAL + (255,))

    font_path = choose_font(
        [
            "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
            "/System/Library/Fonts/Supplemental/Arial Rounded Bold.ttf",
            "/System/Library/Fonts/Supplemental/Comic Sans MS Bold.ttf",
        ]
    )
    text = "HAPPYROW"
    text_font = load_font(font_path, 26)
    text_canvas = Image.new("RGBA", (height, width), (0, 0, 0, 0))
    text_draw = ImageDraw.Draw(text_canvas)
    bbox = text_draw.textbbox((0, 0), text, font=text_font)
    text_x = (height - (bbox[2] - bbox[0])) // 2
    text_y = (width - (bbox[3] - bbox[1])) // 2
    text_draw.text((text_x, text_y), text, font=text_font, fill=(255, 255, 255, 255))
    rotated_text = text_canvas.rotate(90, expand=False)
    canvas.alpha_composite(rotated_text, (0, 0))

    icon = trim_transparent(happyrow_icon)
    icon_size = 44
    icon = icon.resize((icon_size, icon_size), Image.Resampling.LANCZOS)
    circle_center = (width // 2, height - 48)
    radius = 28
    draw.ellipse(
        [
            (circle_center[0] - radius, circle_center[1] - radius),
            (circle_center[0] + radius, circle_center[1] + radius),
        ],
        fill=OFF_WHITE + (255,),
    )
    canvas.alpha_composite(icon, (circle_center[0] - icon_size // 2, circle_center[1] - icon_size // 2))
    return canvas


def recolor_png(image: Image.Image) -> Image.Image:
    result = Image.new("RGBA", image.size)
    src = image.load()
    dst = result.load()
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = src[x, y]
            dst[x, y] = remap_pixel(r, g, b, a)
    return result


def remap_pixel(r: int, g: int, b: int, a: int) -> tuple[int, int, int, int]:
    if a == 0:
        return (r, g, b, a)

    hue, lightness, saturation = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
    hue_deg = hue * 360

    if saturation < 0.08:
        if lightness > 0.88:
            return OFF_WHITE + (a,)
        if lightness < 0.12:
            return hls_to_rgba(DEEP_NAVY, lightness, saturation, a)
        return hls_to_rgba(NAVY, lightness, max(saturation, 0.18), a)

    if hue_deg < 25 or hue_deg > 335:
        return hls_to_rgba(TEAL, lightness, max(saturation, 0.55), a)
    if 25 <= hue_deg <= 80:
        return hls_to_rgba(CORAL, lightness, max(saturation, 0.42), a)
    if 80 < hue_deg < 180:
        return hls_to_rgba(TEAL_ALT, lightness, max(saturation, 0.45), a)
    return hls_to_rgba(NAVY, lightness, max(saturation, 0.35), a)


def hls_to_rgba(base_rgb: tuple[int, int, int], lightness: float, saturation: float, alpha: int) -> tuple[int, int, int, int]:
    base_h, _, base_s = colorsys.rgb_to_hls(*(channel / 255 for channel in base_rgb))
    adjusted_lightness = max(0.0, min(1.0, lightness))
    adjusted_saturation = max(base_s * 0.75, min(1.0, saturation))
    red, green, blue = colorsys.hls_to_rgb(base_h, adjusted_lightness, adjusted_saturation)
    return (int(red * 255), int(green * 255), int(blue * 255), alpha)


def trim_transparent(image: Image.Image) -> Image.Image:
    bbox = image.getbbox()
    if bbox is None:
        return image
    return image.crop(bbox)


def choose_font(candidates: Iterable[str]) -> str | None:
    for candidate in candidates:
        if Path(candidate).exists():
            return candidate
    return None


def load_font(font_path: str | None, size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    if font_path:
        return ImageFont.truetype(font_path, size=size)
    return ImageFont.load_default()


def zip_directory(source_dir: Path, output_file: Path) -> None:
    with zipfile.ZipFile(output_file, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for file_path in sorted(source_dir.rglob("*")):
            if file_path.is_file():
                archive.write(file_path, file_path.relative_to(source_dir).as_posix())


if __name__ == "__main__":
    main()
