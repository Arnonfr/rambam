const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const file = '/Users/hyh/Documents/Codex/לימוד-יומי/תפילות-אודיו/בדיקת-אודיו.html';
const script = fs.readFileSync(file, 'utf8').match(/<script>([\s\S]*)<\/script>/)[1];
const elements = {};
function element(id) {
  return elements[id] ||= { checked: false, currentTime: 0, paused: true, children: [],
    replaceChildren() { this.children = []; }, append(x) { this.children.push(x); },
    pause() { this.paused = true; }, play() { this.paused = false; return Promise.resolve(); } };
}
const context = vm.createContext({ document: {getElementById: element, createElement: () => ({})} });
vm.runInContext(script, context);
assert.ok(elements.player.src.startsWith('מקטעים/'));
assert.equal(elements.previous.disabled, true);
elements.player.onloadedmetadata();
for (const day of ['sun', 'mon', 'tue', 'wed', 'thu', 'fri']) {
  elements.filter.value = day;
  elements.filter.onchange();
  assert.equal(elements.list.children.length, 1, day);
  assert.ok(elements.player.src.includes('shir-' + day));
  elements.player.onloadedmetadata();
  elements.player.onended();
  assert.equal(elements.player.paused, true);
}
elements.filter.value = 'all'; elements.filter.onchange();
elements.next.onclick(); elements.player.onloadedmetadata();
assert.ok(elements.player.src.includes('shir-mon'));
elements.previous.onclick(); elements.player.onloadedmetadata();
assert.ok(elements.player.src.includes('shir-sun'));
elements.player.currentTime = 4; elements.original.checked = true; elements.original.onchange();
elements.player.onloadedmetadata();
assert.ok(elements.player.src.startsWith('מקור/'));
assert.equal(elements.player.currentTime, 4);
elements.original.checked = false; elements.original.onchange(); elements.player.onloadedmetadata();
assert.equal(elements.player.currentTime, 4);
const cutIndex = vm.runInContext('all.findIndex(x => x.start_seconds > 0 && x.end_seconds - x.start_seconds > 8)', context);
elements.list.children[cutIndex].onclick(); elements.player.onloadedmetadata();
const cutStart = vm.runInContext('list[index].start_seconds', context);
elements.player.currentTime = 4; elements.original.checked = true; elements.original.onchange(); elements.player.onloadedmetadata();
assert.equal(elements.player.currentTime, cutStart + 4);
elements.original.checked = false; elements.original.onchange(); elements.player.onloadedmetadata();
assert.ok(Math.abs(elements.player.currentTime - 4) < 0.001);
elements.filter.value = 'sources'; elements.filter.onchange();
assert.equal(elements.list.children.length, 11);
assert.ok(elements.player.src.startsWith('מעובדים-לשמיעה/'));
console.log('PASS: six weekdays, 11 processed sources, playlist advance, previous, end stop, A/B position preservation');
