import urllib.request, json
r1 = json.loads(urllib.request.urlopen("https://www.sefaria.org/api/texts/Genesis.1.30-2.3?context=0").read())
r2 = json.loads(urllib.request.urlopen("https://www.sefaria.org/api/texts/Rashi_on_Genesis.1.30-2.3?context=0").read())

def flatten_to_verses(he, depth, target_depth):
    result = []
    if depth == target_depth:
        return [he]
    if isinstance(he, list):
        for item in he:
            result.extend(flatten_to_verses(item, depth+1, target_depth))
    else:
        result.append(he)
    return result

v = flatten_to_verses(r1["he"], 1, 2)
r = flatten_to_verses(r2["he"], 1, 2)
print("Verses:", len(v), "Rashi verses:", len(r))
