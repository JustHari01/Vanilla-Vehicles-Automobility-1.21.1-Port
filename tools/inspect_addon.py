import json, pathlib, re

ROOT = pathlib.Path(__file__).resolve().parents[1]
BP = ROOT / 'original-addon' / 'Vanilla Vehicles BP'
RP = ROOT / 'original-addon' / 'Vanilla Vehicles RP'
def load_json(file):
    text = file.read_text(encoding='utf-8-sig')
    text = re.sub(r'"(?:\\.|[^"\\])*"|//[^\n]*|/\*[\s\S]*?\*/', lambda m: m[0] if m[0].startswith('"') else '', text)
    return json.loads(re.sub(r',\s*([}\]])', r'\1', text))

out = {}
for file in sorted((BP / 'entities/bw/vv/vehicles').glob('*.json')):
    data = load_json(file)['minecraft:entity']
    interactions = []
    for group, components in [('base', data.get('components', {})), *data.get('component_groups', {}).items()]:
        for key, value in components.items():
            if key in ['minecraft:interact', 'minecraft:rideable', 'minecraft:inventory', 'minecraft:health', 'minecraft:movement', 'minecraft:input_ground_controlled', 'minecraft:behavior.player_ride_tamed']:
                interactions.append({'group': group, 'component': key, 'value': value})
    out[file.stem] = {'properties': data['description'].get('properties'), 'interactions': interactions, 'events': list(data.get('events',{}))}
(ROOT / 'docs').mkdir(exist_ok=True)
(ROOT / 'docs/addon-inventory.json').write_text(json.dumps(out, indent=2, ensure_ascii=False), encoding='utf-8')
for vehicle, data in out.items():
    print(vehicle, 'properties=', list(data['properties'] or {}))
    print('  event names=', ', '.join(data['events']))
    for item in data['interactions']:
        if item['component'] != 'minecraft:interact':
            print(' ', item['group'],item['component'], json.dumps(item['value']))
        else:
            print('  interactions',item['group'],json.dumps(item['value'])[:500])
print('Inspected extracted source tree:', ROOT / 'original-addon')
