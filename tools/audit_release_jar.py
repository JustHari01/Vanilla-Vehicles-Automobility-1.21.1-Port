"""Check a clean, non-QA local jar against its original addon assets."""
import argparse
import json
import pathlib
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]

def audit(jar, addon):
    failures=[]
    with zipfile.ZipFile(jar) as z, zipfile.ZipFile(addon) as source:
        names=set(z.namelist())
        for dev in ['io/github/justhari/vanillavehicles/VehicleRuntimeQa.class','io/github/justhari/vanillavehicles/VehicleReloadQa.class','io/github/justhari/vanillavehicles/VehicleClientQa.class']:
            if dev in names: failures.append('Development QA class leaked into release: '+dev)
        metadata=z.read('META-INF/neoforge.mods.toml').decode('utf8')
        if 'version="0.5.0.h-vv1-localtest"' not in metadata: failures.append('Combined jar local test version metadata is missing')
        if 'modId="automobility"' not in metadata or 'modId="vanilla_vehicles"' not in metadata: failures.append('One of the two mod descriptors is missing')
        for config_name in (n for n in names if n.endswith('.mixins.json')):
            config=json.loads(z.read(config_name))
            refmap=config.get('refmap')
            if refmap and refmap not in names: failures.append(f'Mixin config {config_name} refers to missing refmap {refmap}')
        catalog=json.loads(z.read('assets/bw_vv/catalog.json'))
        if len(catalog['vehicles'])!=8: failures.append('Expected eight registered vehicle definitions in the resource catalog')
        if len(catalog['recipes'])<1: failures.append('Converted recipes are missing')
        original='Vanilla Vehicles RP/textures/'
        png_names=[n for n in source.namelist() if n.startswith(original) and n.lower().endswith('.png')]
        changed=[]
        for name in png_names:
            target='assets/bw_vv/'+name.removeprefix('Vanilla Vehicles RP/')
            if target not in names or source.read(name)!=z.read(target): changed.append(name)
        if len(png_names)!=218: failures.append(f'Unexpected source texture count: {len(png_names)}')
        if changed: failures.append(f'{len(changed)} source textures are missing or changed')
        if not any(n.startswith('assets/bw_vv/sounds/') and n.endswith('.ogg') for n in names): failures.append('Converted Java sounds are missing')
        if 'ASSET-RIGHTS.md' not in names: failures.append('Source asset attribution is missing')
        result={'jar':str(jar),'jar_bytes':jar.stat().st_size,'archive_texture_count':len(png_names),'all_source_textures_identical':not changed,'vehicle_definitions':len(catalog['vehicles']),'recipe_count':len(catalog['recipes']),'qa_classes_absent':not any(p in names for p in ['io/github/justhari/vanillavehicles/VehicleRuntimeQa.class','io/github/justhari/vanillavehicles/VehicleReloadQa.class','io/github/justhari/vanillavehicles/VehicleClientQa.class']),'failures':failures}
        return result

if __name__=='__main__':
    ap=argparse.ArgumentParser();ap.add_argument('jar',type=pathlib.Path);ap.add_argument('--addon',type=pathlib.Path,required=True,help='Original .mcaddon archive for byte-comparison.')
    args=ap.parse_args()
    result=audit(args.jar,args.addon);print(json.dumps(result,ensure_ascii=False,indent=2));raise SystemExit(bool(result['failures']))
