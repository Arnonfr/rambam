import urllib.request, json
req = urllib.request.urlopen("https://www.sefaria.org/api/texts/Rashi_on_Genesis.1.1-3?context=0")
data = json.loads(req.read())
he = data.get("he", [])
print(type(he), len(he))
if len(he) > 0: print(type(he[0]))
