# Chumash Rashi repair — 2026-10-05

- Refreshed all 28 bundled aliyot from Sefaria's Rashi text API: Haazinu,
  Vezot Haberakhah, Bereishit and Noach (687 comments total).
- Preserve each verse group, including verses with no commentary. Spanning
  references are flattened by chapter only after padding omitted trailing
  uncommented verses using the corresponding Torah response's chapter lengths.
- Decode HTML entities in bundled text and at runtime (including old text data),
  retaining bold opening words in Rashi. No generated commentary is used.
- Old downloaded parasha caches are rejected by content schema version; bundled
  assets and reading-position storage remain available and are not deleted.
- `build_chumash_rashi_assets.py` validates every aliya's verse count before
  writing the refreshed bundle; any source/validation failure aborts the write.
- Regression tests cover spanning/single chapters, missing trailing commentary,
  HTML entities, and Rashi at the start of Bereishit's second aliya (Genesis 2:4).

Source example: https://www.sefaria.org/api/texts/Rashi_on_Genesis.2.4-3.21?context=0
