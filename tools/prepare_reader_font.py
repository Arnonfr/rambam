"""Build the OFL reader face from the bundled, unmodified Romm Vilna font.

Requires fontTools. Outlines and metrics remain unchanged; only the default
Hebrew language features and derivative naming are updated.
"""
from copy import deepcopy
from pathlib import Path
from fontTools.ttLib import TTFont

fonts = Path(__file__).resolve().parents[1] / "app/src/main/res/font"
font = TTFont(fonts / "romm_vilna_regular.ttf")
for tag in ("GPOS", "GSUB"):
    for record in font[tag].table.ScriptList.ScriptRecord:
        if record.ScriptTag == "hebr" and record.Script.DefaultLangSys is None:
            language = next((entry.LangSys for entry in record.Script.LangSysRecord
                             if entry.LangSysTag == "IWR "), None)
            if language is not None:
                record.Script.DefaultLangSys = deepcopy(language)
# The source reserves its family name; the adapted font uses a distinct name.
for record in font["name"].names:
    if record.nameID in (1, 3, 4, 6, 16):
        name = "DerechVilna-Regular" if record.nameID == 6 else "Derech Vilna"
        record.string = name.encode(record.getEncoding())
font.save(fonts / "derech_vilna_regular.ttf")
