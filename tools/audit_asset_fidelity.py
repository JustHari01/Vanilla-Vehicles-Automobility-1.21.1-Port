"""Read-only comparison against the original .mcaddon, not just its extracted copy."""
import argparse
import hashlib
import io
import json
import pathlib
import re
import zipfile
import os
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[1]
AS = ROOT / 'neoforge/src/main/resources/assets/bw_vv'

def parse_jsonc(data):
    text = data.decode('utf-8-sig')
    text = re.sub(r'"(?:\\.|[^"\\])*"|//[^\n]*|/\*[\s\S]*?\*/', lambda m: m[0] if m[0].startswith('"') else '', text)
    return json.loads(re.sub(r',\s*([}\]])', r'\1', text))

def audit(archive):
    report = {'source_sha256': hashlib.sha256(archive.read_bytes()).hexdigest(), 'textures': [], 'geometry': [], 'derivedFrames': [], 'sounds': []}
    with zipfile.ZipFile(archive) as source:
        prefix = 'Vanilla Vehicles RP/'
        for name in source.namelist():
            rel = name.removeprefix(prefix)
            if name.startswith(prefix + 'textures/') and name.lower().endswith('.png'):
                original = source.read(name)
                target = AS / rel
                report['textures'].append({'file': rel, 'sha256': hashlib.sha256(original).hexdigest(), 'identical': target.exists() and target.read_bytes() == original})
        extra_models = {
            'redstone_compressor': 'models/entity/bw/vv/utilities/redstone_compactor.json',
            'smoke_bomb': 'models/entity/bw/vv/smoke_bomb.geo.json',
            'wind_vane': 'models/blocks/bw/vv/wind_vane.geo.json',
            'mechanic': 'models/entity/bw/vv/mobs/mechanic.json',
            'flag': 'models/blocks/bw/vv/flag.geo.json',
            'flag_pole': 'models/blocks/bw/vv/flag.pole.geo.json',
            'mech_projectile': 'models/entity/bw/vv/helpers/mech_projectile.geo.json',
            'tesla_helmet': 'models/entity/bw/vv/attachable/tesla_helmet.geo.json',
            'prismer_aura': 'models/entity/bw/vv/helpers/prismer_aura.geo.json',
        }
        vehicle_models = [name for name in source.namelist() if name.startswith(prefix+'models/entity/bw/vv/vehicles/') and name.endswith('.json')]
        model_sources = {pathlib.PurePosixPath(name).name.split('.')[0]: name.removeprefix(prefix) for name in vehicle_models}
        model_sources.update(extra_models)
        for name, rel in model_sources.items():
            original = parse_jsonc(source.read(prefix+rel))
            target = json.loads((AS/f'geo/{name}.geo.json').read_text(encoding='utf-8'))
            report['geometry'].append({'file': rel, 'cubes_bones_uv_identical': original['minecraft:geometry'] == target['minecraft:geometry']})
        catalog = json.loads((AS/'catalog.json').read_text(encoding='utf-8'))
        for vehicle, details in catalog['vehicles'].items():
            for key, count in details.get('sheets', {}).items():
                original = Image.open(io.BytesIO(source.read(prefix+details['textures'][key]+'.png'))).convert('RGBA')
                height = original.height//count
                for index in range(count):
                    target = Image.open(AS/f'textures/derived/{vehicle}/{key}/{index}.png').convert('RGBA')
                    crop = original.crop((0, index*height, original.width, (index+1)*height))
                    report['derivedFrames'].append({'file': f'{vehicle}/{key}/{index}', 'pixels_identical': target.size == crop.size and target.tobytes() == crop.tobytes()})
        for name in source.namelist():
            if name.startswith(prefix+'sounds/') and name.lower().endswith('.wav'):
                rel = name.removeprefix(prefix)
                target = (AS/rel).with_suffix('.ogg')
                valid = target.exists() and target.read_bytes()[:4] == b'OggS' and b'vorbis' in target.read_bytes()[:100]
                report['sounds'].append({'original': rel, 'original_sha256': hashlib.sha256(source.read(name)).hexdigest(), 'java_ogg_exists': valid, 'conversion': 'Vorbis quality 6; lossy, not byte-identical'})
    checks = [row['identical'] for row in report['textures']] + [row['cubes_bones_uv_identical'] for row in report['geometry']] + [row['pixels_identical'] for row in report['derivedFrames']] + [row['java_ogg_exists'] for row in report['sounds']]
    report['summary'] = {'original_png_count': len(report['textures']), 'original_vehicle_models': len(vehicle_models), 'additional_models': len(extra_models), 'lossless_frames': len(report['derivedFrames']), 'original_wav_sounds': len(report['sounds']), 'all_checks_pass': all(checks)}
    report['limits'] = ['Asset equality does not prove identical rendering in Java and Bedrock.', 'Dye, emissive/PBR materials, particles and animation/controller behaviour require separate runtime comparison.', 'Not every original animation or Bedrock material is used by the Java port yet.']
    return report

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    default_archive = pathlib.Path(os.environ['VV_ADDON']) if os.environ.get('VV_ADDON') else None
    parser.add_argument('--archive', type=pathlib.Path, default=default_archive, help='Original .mcaddon to compare with the converted assets (or set VV_ADDON).')
    args = parser.parse_args()
    if args.archive is None:
        parser.error('pass --archive or set VV_ADDON to the original .mcaddon file')
    report = audit(args.archive)
    (ROOT/'docs/asset-fidelity.json').write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps(report['summary']))
    raise SystemExit(0 if report['summary']['all_checks_pass'] else 1)
