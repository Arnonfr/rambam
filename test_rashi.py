import json, urllib.request

with open("app/src/main/assets/chumash_cache.json", "r") as f:
    data = json.load(f)

def flatten_to_verses(he, depth, target_depth):
    result = []
    if depth == target_depth:
        return [he] if he else [[]]
    if isinstance(he, list):
        for item in he:
            result.extend(flatten_to_verses(item, depth+1, target_depth))
    else:
        result.append(he)
    return result

for parasha_name, p_data in data.items():
    print(f"Processing {parasha_name}...")
    for aliya in p_data.get("aliyot", []):
        ref = aliya["ref"]
        formatted_ref = ref.replace(" ", ".")
        rashi_url = f"https://www.sefaria.org/api/texts/Rashi_on_{formatted_ref}?context=0"
        
        try:
            req = urllib.request.urlopen(rashi_url)
            rashi_data = json.loads(req.read())
            he = rashi_data.get("he", [])
            depth = rashi_data.get("textDepth", 1)
            raw_verses = flatten_to_verses(he, 1, depth - 1)
            print(f"  {ref}: fetched {len(raw_verses)} rashi verses")
            for v_idx, verse in enumerate(aliya.get("verses", [])):
                if v_idx < len(raw_verses):
                    rashi_list = raw_verses[v_idx]
                    if isinstance(rashi_list, list):
                        verse["rashi"] = [r.strip() for r in rashi_list if isinstance(r, str) and r.strip()]
                    else:
                        verse["rashi"] = []
                else:
                    verse["rashi"] = []
        except Exception as e:
            print(f"Failed {ref}: {e}")

with open("app/src/main/assets/chumash_cache.json", "w") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print("Done!")
