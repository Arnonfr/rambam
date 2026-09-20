import urllib.request, json
req = urllib.request.urlopen("https://www.sefaria.org/api/texts/Rashi_on_Genesis.1.30-2.3?context=0")
data = json.loads(req.read())
he = data.get("he", [])

def get_verses(he_array, depth, target_depth):
    result = []
    if depth == target_depth:
        if isinstance(he_array, list):
            result.append(he_array)
        else:
            result.append([he_array])
        return result
    if isinstance(he_array, list):
        for item in he_array:
            result.extend(get_verses(item, depth+1, target_depth))
    else:
        # Unexpected scalar
        result.append([])
    return result

# Find textDepth and sectionNames
print(data.get("textDepth"), data.get("sectionNames"))
