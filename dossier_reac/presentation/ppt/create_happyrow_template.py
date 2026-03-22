#!/usr/bin/env python3
"""Convert happyrow_template_to_process.pptx slides into proper slide layouts.

Takes the 8 decorative slides and creates a PowerPoint template with
native slide layouts (like Betclic_GOAT-Template.pptx), so layouts
appear in the "New Slide" menu in PowerPoint.

Slide mapping:
  0 -> SPLIT 1       (50/50 two-column)
  1 -> SPLIT 2       (50/50 two-column, variant)
  2 -> MAIN 1        (full-width content)
  3 -> MAIN 2        (full-width content, variant)
  4 -> MAIN 3        (full-width content, variant)
  5 -> SPLIT 1-3     (1/3 + 2/3 columns)
  6 -> SECTION        (section divider)
  7 -> COVER          (title/cover)
"""

import shutil
import zipfile
import os
import re
from copy import deepcopy
from io import BytesIO
from pathlib import Path
from lxml import etree

SCRIPT_DIR = Path(__file__).parent
SOURCE = SCRIPT_DIR / "happyrow_template_to_process.pptx"
OUTPUT = SCRIPT_DIR / "HappyRow-Template.pptx"

NS = {
    "a": "http://schemas.openxmlformats.org/drawingml/2006/main",
    "r": "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
    "p": "http://schemas.openxmlformats.org/presentationml/2006/main",
    "ct": "http://schemas.openxmlformats.org/package/2006/content-types",
    "pr": "http://schemas.openxmlformats.org/package/2006/relationships",
}

IMAGE_REL_TYPE = (
    "http://schemas.openxmlformats.org/officeDocument/2006/relationships/image"
)
LAYOUT_REL_TYPE = (
    "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout"
)
MASTER_REL_TYPE = (
    "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster"
)
LAYOUT_CT = (
    "application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"
)

# Slide dimensions (EMU)
SLD_W = 18288000
SLD_H = 10287000

MARGIN = 457200  # ~0.5 inch
TITLE_H = 914400  # ~1 inch
TITLE_TOP = 228600  # ~0.25 inch

LAYOUT_DEFS = [
    {
        "name": "SPLIT 1",
        "slide_idx": 0,
        "placeholders": [
            {"idx": 0, "type": "title", "x": MARGIN, "y": TITLE_TOP,
             "cx": SLD_W - 2 * MARGIN, "cy": TITLE_H},
            {"idx": 10, "type": "body", "x": MARGIN, "y": TITLE_TOP + TITLE_H + 228600,
             "cx": (SLD_W - 3 * MARGIN) // 2, "cy": SLD_H - TITLE_TOP - TITLE_H - 3 * MARGIN},
            {"idx": 11, "type": "body", "x": MARGIN + (SLD_W - 3 * MARGIN) // 2 + MARGIN,
             "y": TITLE_TOP + TITLE_H + 228600,
             "cx": (SLD_W - 3 * MARGIN) // 2, "cy": SLD_H - TITLE_TOP - TITLE_H - 3 * MARGIN},
        ],
    },
    {
        "name": "SPLIT 2",
        "slide_idx": 1,
        "placeholders": [
            {"idx": 0, "type": "title", "x": MARGIN, "y": TITLE_TOP,
             "cx": SLD_W - 2 * MARGIN, "cy": TITLE_H},
            {"idx": 10, "type": "body", "x": MARGIN, "y": TITLE_TOP + TITLE_H + 228600,
             "cx": (SLD_W - 3 * MARGIN) // 2, "cy": SLD_H - TITLE_TOP - TITLE_H - 3 * MARGIN},
            {"idx": 11, "type": "body", "x": MARGIN + (SLD_W - 3 * MARGIN) // 2 + MARGIN,
             "y": TITLE_TOP + TITLE_H + 228600,
             "cx": (SLD_W - 3 * MARGIN) // 2, "cy": SLD_H - TITLE_TOP - TITLE_H - 3 * MARGIN},
        ],
    },
    {
        "name": "MAIN 1",
        "slide_idx": 2,
        "placeholders": [
            {"idx": 0, "type": "title", "x": MARGIN, "y": TITLE_TOP,
             "cx": SLD_W - 2 * MARGIN, "cy": TITLE_H},
            {"idx": 10, "type": "body", "x": MARGIN, "y": TITLE_TOP + TITLE_H + 228600,
             "cx": SLD_W - 2 * MARGIN, "cy": SLD_H - TITLE_TOP - TITLE_H - 3 * MARGIN},
        ],
    },
    {
        "name": "MAIN 2",
        "slide_idx": 3,
        "placeholders": [
            {"idx": 0, "type": "title", "x": MARGIN, "y": TITLE_TOP,
             "cx": SLD_W - 2 * MARGIN, "cy": TITLE_H},
            {"idx": 10, "type": "body", "x": MARGIN, "y": TITLE_TOP + TITLE_H + 228600,
             "cx": SLD_W - 2 * MARGIN, "cy": SLD_H - TITLE_TOP - TITLE_H - 3 * MARGIN},
        ],
    },
    {
        "name": "MAIN 3",
        "slide_idx": 4,
        "placeholders": [
            {"idx": 0, "type": "title", "x": MARGIN, "y": TITLE_TOP,
             "cx": SLD_W - 2 * MARGIN, "cy": TITLE_H},
            {"idx": 10, "type": "body", "x": MARGIN, "y": TITLE_TOP + TITLE_H + 228600,
             "cx": SLD_W - 2 * MARGIN, "cy": SLD_H - TITLE_TOP - TITLE_H - 3 * MARGIN},
        ],
    },
    {
        "name": "SPLIT 1-3",
        "slide_idx": 5,
        "placeholders": [
            {"idx": 0, "type": "title", "x": MARGIN, "y": TITLE_TOP,
             "cx": SLD_W - 2 * MARGIN, "cy": TITLE_H},
            {"idx": 10, "type": "body", "x": MARGIN, "y": TITLE_TOP + TITLE_H + 228600,
             "cx": (SLD_W - 3 * MARGIN) // 3, "cy": SLD_H - TITLE_TOP - TITLE_H - 3 * MARGIN},
            {"idx": 11, "type": "body",
             "x": MARGIN + (SLD_W - 3 * MARGIN) // 3 + MARGIN,
             "y": TITLE_TOP + TITLE_H + 228600,
             "cx": (SLD_W - 3 * MARGIN) * 2 // 3,
             "cy": SLD_H - TITLE_TOP - TITLE_H - 3 * MARGIN},
        ],
    },
    {
        "name": "SECTION",
        "slide_idx": 6,
        "placeholders": [
            {"idx": 0, "type": "ctrTitle",
             "x": SLD_W // 6, "y": SLD_H // 3,
             "cx": SLD_W * 2 // 3, "cy": SLD_H // 3},
        ],
    },
    {
        "name": "COVER",
        "slide_idx": 7,
        "placeholders": [
            {"idx": 0, "type": "ctrTitle",
             "x": SLD_W // 6, "y": SLD_H // 4,
             "cx": SLD_W * 2 // 3, "cy": SLD_H // 4},
            {"idx": 10, "type": "subTitle",
             "x": SLD_W // 4, "y": SLD_H // 4 + SLD_H // 4 + 228600,
             "cx": SLD_W // 2, "cy": SLD_H // 4},
        ],
    },
]

NEW_LAYOUT_START = 12  # existing layouts are 1-11


def _qn(ns_prefix, tag):
    return f"{{{NS[ns_prefix]}}}{tag}"


def make_placeholder_sp(ph_def, shape_id):
    """Build a placeholder <p:sp> element."""
    idx = ph_def["idx"]
    ph_type = ph_def["type"]

    sp = etree.SubElement(etree.Element("dummy"), _qn("p", "sp"))

    # nvSpPr
    nv = etree.SubElement(sp, _qn("p", "nvSpPr"))
    cnv_pr = etree.SubElement(nv, _qn("p", "cNvPr"))
    cnv_pr.set("id", str(shape_id))

    if ph_type == "title":
        cnv_pr.set("name", "Title")
    elif ph_type == "ctrTitle":
        cnv_pr.set("name", "Title")
    elif ph_type == "subTitle":
        cnv_pr.set("name", "Subtitle")
    else:
        cnv_pr.set("name", f"Content Placeholder {idx}")

    cnv_sp_pr = etree.SubElement(nv, _qn("p", "cNvSpPr"))
    sp_locks = etree.SubElement(cnv_sp_pr, _qn("a", "spLocks"))
    sp_locks.set("noGrp", "1")

    nv_pr = etree.SubElement(nv, _qn("p", "nvPr"))
    ph_el = etree.SubElement(nv_pr, _qn("p", "ph"))
    if ph_type == "title":
        ph_el.set("type", "title")
    elif ph_type == "ctrTitle":
        ph_el.set("type", "ctrTitle")
    elif ph_type == "subTitle":
        ph_el.set("type", "subTitle")
    else:
        ph_el.set("sz", "quarter")
    ph_el.set("idx", str(idx))

    # spPr
    sp_pr = etree.SubElement(sp, _qn("p", "spPr"))
    xfrm = etree.SubElement(sp_pr, _qn("a", "xfrm"))
    off = etree.SubElement(xfrm, _qn("a", "off"))
    off.set("x", str(ph_def["x"]))
    off.set("y", str(ph_def["y"]))
    ext = etree.SubElement(xfrm, _qn("a", "ext"))
    ext.set("cx", str(ph_def["cx"]))
    ext.set("cy", str(ph_def["cy"]))
    geom = etree.SubElement(sp_pr, _qn("a", "prstGeom"))
    geom.set("prst", "rect")
    etree.SubElement(geom, _qn("a", "avLst"))

    # txBody
    tx = etree.SubElement(sp, _qn("p", "txBody"))
    body_pr = etree.SubElement(tx, _qn("a", "bodyPr"))
    if ph_type in ("ctrTitle", "subTitle"):
        body_pr.set("anchor", "ctr")
    etree.SubElement(tx, _qn("a", "lstStyle"))
    p_el = etree.SubElement(tx, _qn("a", "p"))
    if ph_type in ("ctrTitle", "subTitle"):
        p_pr = etree.SubElement(p_el, _qn("a", "pPr"))
        p_pr.set("algn", "ctr")
    r_el = etree.SubElement(p_el, _qn("a", "r"))
    r_pr = etree.SubElement(r_el, _qn("a", "rPr"))
    r_pr.set("lang", "fr-FR")
    if ph_type in ("title", "ctrTitle"):
        r_pr.set("sz", "2800")
        r_pr.set("b", "1")
    elif ph_type == "subTitle":
        r_pr.set("sz", "1800")
    else:
        r_pr.set("sz", "1400")
    t_el = etree.SubElement(r_el, _qn("a", "t"))

    if ph_type in ("title", "ctrTitle"):
        t_el.text = "Titre"
    elif ph_type == "subTitle":
        t_el.text = "Sous-titre"
    else:
        t_el.text = "Contenu"

    return sp


def slide_xml_to_layout_xml(slide_xml_bytes, layout_name, ph_defs):
    """Convert a <p:sld> XML into a <p:sldLayout> XML with placeholders."""
    sld = etree.fromstring(slide_xml_bytes)

    # Create layout root
    layout = etree.Element(
        _qn("p", "sldLayout"),
        nsmap={"a": NS["a"], "r": NS["r"], "p": NS["p"]},
    )
    layout.set("preserve", "1")
    layout.set("userDrawn", "1")

    # cSld
    csld = etree.SubElement(layout, _qn("p", "cSld"))
    csld.set("name", layout_name)

    # Copy background if present
    src_csld = sld.find(_qn("p", "cSld"))
    src_bg = src_csld.find(_qn("p", "bg")) if src_csld is not None else None
    if src_bg is not None:
        csld.append(deepcopy(src_bg))

    # Copy spTree (decorative shapes)
    src_sp_tree = src_csld.find(_qn("p", "spTree")) if src_csld is not None else None
    if src_sp_tree is not None:
        sp_tree = deepcopy(src_sp_tree)
    else:
        sp_tree = etree.SubElement(csld, _qn("p", "spTree"))
        nv = etree.SubElement(sp_tree, _qn("p", "nvGrpSpPr"))
        etree.SubElement(nv, _qn("p", "cNvPr")).set("id", "1")
        etree.SubElement(nv, _qn("p", "cNvGrpSpPr"))
        etree.SubElement(nv, _qn("p", "nvPr"))
        grp = etree.SubElement(sp_tree, _qn("p", "grpSpPr"))
        xfrm = etree.SubElement(grp, _qn("a", "xfrm"))
        for tag in ("off", "ext", "chOff", "chExt"):
            el = etree.SubElement(xfrm, _qn("a", tag))
            el.set("x" if "off" in tag.lower() or "Off" in tag else "cx", "0")
            el.set("y" if "off" in tag.lower() or "Off" in tag else "cy", "0")

    # Mark existing shapes as userDrawn
    for sp_el in sp_tree.findall(_qn("p", "sp")):
        nv_pr = sp_el.find(f".//{_qn('p', 'nvPr')}")
        if nv_pr is not None:
            nv_pr.set("userDrawn", "1")

    # Find max shape id
    max_id = 1
    for cid in sp_tree.iter():
        id_val = cid.get("id")
        if id_val and id_val.isdigit():
            max_id = max(max_id, int(id_val))

    # Add placeholders
    for ph_def in ph_defs:
        max_id += 1
        ph_sp = make_placeholder_sp(ph_def, max_id)
        sp_tree.append(ph_sp)

    csld.append(sp_tree)

    # clrMapOvr (inherit from master)
    cmovr = etree.SubElement(layout, _qn("p", "clrMapOvr"))
    etree.SubElement(cmovr, _qn("a", "masterClrMapping"))

    return etree.tostring(layout, xml_declaration=True, encoding="UTF-8", standalone=True)


def make_layout_rels(master_rid, image_rels):
    """Build a .rels XML for a slideLayout."""
    root = etree.Element(
        "Relationships",
        nsmap={None: "http://schemas.openxmlformats.org/package/2006/relationships"},
    )
    # Relationship to slide master
    rel = etree.SubElement(root, "Relationship")
    rel.set("Id", master_rid)
    rel.set("Type", MASTER_REL_TYPE)
    rel.set("Target", "../slideMasters/slideMaster1.xml")

    # Relationships to images
    for rid, target in image_rels:
        r = etree.SubElement(root, "Relationship")
        r.set("Id", rid)
        r.set("Type", IMAGE_REL_TYPE)
        r.set("Target", target)

    return etree.tostring(root, xml_declaration=True, encoding="UTF-8", standalone=True)


def build_template():
    print(f"Reading source: {SOURCE.name}")
    src_buf = BytesIO(SOURCE.read_bytes())

    with zipfile.ZipFile(src_buf, "r") as src_zip:
        all_files = {name: src_zip.read(name) for name in src_zip.namelist()}

    # Collect slide data
    slides_data = []
    for i in range(8):
        slide_num = i + 1
        sld_xml = all_files[f"ppt/slides/slide{slide_num}.xml"]
        rels_xml = all_files.get(f"ppt/slides/_rels/slide{slide_num}.xml.rels", None)
        image_rels = []
        if rels_xml:
            rels_root = etree.fromstring(rels_xml)
            for rel in rels_root:
                rtype = rel.get("Type", "")
                if "image" in rtype:
                    image_rels.append((rel.get("Id"), rel.get("Target")))
        slides_data.append({"xml": sld_xml, "image_rels": image_rels})

    # Prepare output files dict (start with all source files)
    out_files = dict(all_files)

    # Remove existing slides (we only want layouts)
    for key in list(out_files.keys()):
        if key.startswith("ppt/slides/") and not key.startswith("ppt/slides/_rels/"):
            del out_files[key]
        if key.startswith("ppt/slides/_rels/"):
            del out_files[key]

    # Create new slide layouts from the 8 slides
    new_layout_files = {}
    new_layout_rels = {}

    for layout_def in LAYOUT_DEFS:
        si = layout_def["slide_idx"]
        sd = slides_data[si]
        layout_num = NEW_LAYOUT_START + LAYOUT_DEFS.index(layout_def)
        layout_path = f"ppt/slideLayouts/slideLayout{layout_num}.xml"
        rels_path = f"ppt/slideLayouts/_rels/slideLayout{layout_num}.xml.rels"

        layout_xml = slide_xml_to_layout_xml(
            sd["xml"], layout_def["name"], layout_def["placeholders"]
        )

        # Remap image rels: keep same rIds, adjust target paths
        # Slide rels have targets like "../media/image1.svg"
        # Layout rels also use "../media/image1.svg" (same relative path)
        img_rels = [(rid, target) for rid, target in sd["image_rels"]]

        master_rid = f"rId{len(img_rels) + 1}"
        layout_rels_xml = make_layout_rels(master_rid, img_rels)

        new_layout_files[layout_path] = layout_xml
        new_layout_rels[rels_path] = layout_rels_xml

        print(f"  Layout {layout_num}: {layout_def['name']} "
              f"(from slide {si}, {len(img_rels)} images, "
              f"{len(layout_def['placeholders'])} placeholders)")

    # Add new layouts to output
    out_files.update(new_layout_files)
    out_files.update(new_layout_rels)

    # Update slide master rels to include new layouts
    master_rels_path = "ppt/slideMasters/_rels/slideMaster1.xml.rels"
    master_rels_root = etree.fromstring(out_files[master_rels_path])

    # Find max rId in master rels
    max_master_rid = 0
    for rel in master_rels_root:
        rid_str = rel.get("Id", "rId0")
        m = re.match(r"rId(\d+)", rid_str)
        if m:
            max_master_rid = max(max_master_rid, int(m.group(1)))

    for i, layout_def in enumerate(LAYOUT_DEFS):
        layout_num = NEW_LAYOUT_START + i
        max_master_rid += 1
        rel = etree.SubElement(master_rels_root, "Relationship")
        rel.set("Id", f"rId{max_master_rid}")
        rel.set("Type", LAYOUT_REL_TYPE)
        rel.set("Target", f"../slideLayouts/slideLayout{layout_num}.xml")

    out_files[master_rels_path] = etree.tostring(
        master_rels_root, xml_declaration=True, encoding="UTF-8", standalone=True
    )

    # Update slide master XML to reference new layouts in sldLayoutIdLst
    master_path = "ppt/slideMasters/slideMaster1.xml"
    master_root = etree.fromstring(out_files[master_path])
    sld_layout_id_lst = master_root.find(_qn("p", "sldLayoutIdLst"))

    if sld_layout_id_lst is None:
        sld_layout_id_lst = etree.SubElement(master_root, _qn("p", "sldLayoutIdLst"))

    # Find max layout id
    max_layout_id = 2147483647
    for existing in sld_layout_id_lst:
        lid = existing.get("id")
        if lid:
            max_layout_id = max(max_layout_id, int(lid))

    # Re-read master rels to get the rIds we just created
    master_rels_root2 = etree.fromstring(out_files[master_rels_path])
    layout_rids = {}
    for rel in master_rels_root2:
        target = rel.get("Target", "")
        for i, layout_def in enumerate(LAYOUT_DEFS):
            layout_num = NEW_LAYOUT_START + i
            if target == f"../slideLayouts/slideLayout{layout_num}.xml":
                layout_rids[layout_num] = rel.get("Id")

    for i, layout_def in enumerate(LAYOUT_DEFS):
        layout_num = NEW_LAYOUT_START + i
        max_layout_id += 1
        sld_layout_id = etree.SubElement(sld_layout_id_lst, _qn("p", "sldLayoutId"))
        sld_layout_id.set("id", str(max_layout_id))
        rid = layout_rids.get(layout_num)
        if rid:
            sld_layout_id.set(_qn("r", "id"), rid)

    out_files[master_path] = etree.tostring(
        master_root, xml_declaration=True, encoding="UTF-8", standalone=True
    )

    # Update [Content_Types].xml
    ct_root = etree.fromstring(out_files["[Content_Types].xml"])

    # Remove slide content type overrides
    for override in list(ct_root):
        pn = override.get("PartName", "")
        if pn.startswith("/ppt/slides/slide"):
            ct_root.remove(override)

    # Add new layout content types
    for i in range(len(LAYOUT_DEFS)):
        layout_num = NEW_LAYOUT_START + i
        part_name = f"/ppt/slideLayouts/slideLayout{layout_num}.xml"
        # Check if already exists
        exists = any(
            o.get("PartName") == part_name for o in ct_root
        )
        if not exists:
            ov = etree.SubElement(ct_root, "Override")
            ov.set("PartName", part_name)
            ov.set("ContentType", LAYOUT_CT)

    out_files["[Content_Types].xml"] = etree.tostring(
        ct_root, xml_declaration=True, encoding="UTF-8", standalone=True
    )

    # Update presentation.xml: remove slide references
    pres_path = "ppt/presentation.xml"
    pres_root = etree.fromstring(out_files[pres_path])
    sld_id_lst = pres_root.find(_qn("p", "sldIdLst"))
    if sld_id_lst is not None:
        for sld_id in list(sld_id_lst):
            sld_id_lst.remove(sld_id)

    out_files[pres_path] = etree.tostring(
        pres_root, xml_declaration=True, encoding="UTF-8", standalone=True
    )

    # Update presentation.xml.rels: remove slide rels
    pres_rels_path = "ppt/_rels/presentation.xml.rels"
    pres_rels_root = etree.fromstring(out_files[pres_rels_path])
    for rel in list(pres_rels_root):
        target = rel.get("Target", "")
        if target.startswith("slides/slide"):
            pres_rels_root.remove(rel)

    out_files[pres_rels_path] = etree.tostring(
        pres_rels_root, xml_declaration=True, encoding="UTF-8", standalone=True
    )

    # Write output
    print(f"\nWriting template: {OUTPUT.name}")
    with zipfile.ZipFile(str(OUTPUT), "w", zipfile.ZIP_DEFLATED) as zout:
        for path, data in sorted(out_files.items()):
            zout.writestr(path, data)

    print("Done!")


if __name__ == "__main__":
    build_template()
