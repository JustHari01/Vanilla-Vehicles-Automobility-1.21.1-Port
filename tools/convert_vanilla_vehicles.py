"""Reproducible source-asset transfer for the local Java port."""
import argparse, json, pathlib, re, shutil, hashlib, subprocess, os
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[1]
BP = ROOT / 'original-addon/Vanilla Vehicles BP'
RP = ROOT / 'original-addon/Vanilla Vehicles RP'
RES = ROOT / 'neoforge/src/main/resources'
AS = RES / 'assets/bw_vv'
DATA = RES / 'data/bw_vv/recipe'

parser = argparse.ArgumentParser(description='Convert the extracted Vanilla Vehicles add-on source tree.')
parser.add_argument('--archive', type=pathlib.Path, help='Optional original .mcaddon file to record its SHA-256 in the conversion report.')
args = parser.parse_args()

def load(file):
    text = file.read_text(encoding='utf-8-sig')
    text = re.sub(r'"(?:\\.|[^"\\])*"|//[^\n]*|/\*[\s\S]*?\*/', lambda m: m[0] if m[0].startswith('"') else '', text)
    return json.loads(re.sub(r',\s*([}\]])', r'\1', text))

def put(file, data):
    file.parent.mkdir(parents=True, exist_ok=True)
    file.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding='utf-8')

for kind in ['textures', 'sounds']:
    for file in (RP / kind).rglob('*'):
        if file.is_file() and file.suffix.lower() in ['.png', '.ogg']:
            dst = AS / kind / file.relative_to(RP / kind)
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(file, dst)

# Bedrock ships PCM WAV; Java's sound loader requires Ogg Vorbis. Keep the
# original WAV untouched and encode the same samples rather than replacing audio.
ffmpeg = os.environ.get('VV_FFMPEG') or shutil.which('ffmpeg')
wav_files = list((RP/'sounds').rglob('*.wav'))
if wav_files and not ffmpeg: raise RuntimeError('Install ffmpeg or set VV_FFMPEG to an ffmpeg executable to convert the original WAV sounds')
for file in wav_files:
    dst = (AS/'sounds'/file.relative_to(RP/'sounds')).with_suffix('.ogg')
    dst.parent.mkdir(parents=True, exist_ok=True)
    if not dst.exists() or file.stat().st_mtime > dst.stat().st_mtime:
        subprocess.run([ffmpeg,'-nostdin','-loglevel','error','-y','-i',str(file),'-c:a','libvorbis','-q:a','6',str(dst)],check=True)

catalog = {'items': [], 'vehicles': {}, 'recipeWarnings': []}
catalog['recipes']=[]
for file in (RP / 'models/entity/bw/vv/vehicles').glob('*.json'):
    geo = load(file)
    geo['format_version'] = '1.12.0'
    name = file.name.split('.')[0]
    put(AS / f'geo/{name}.geo.json', geo)
    ce = load(RP / f'entity/bw/vv/vehicles/{name}.entity.json')['minecraft:client_entity']['description']
    catalog['vehicles'][name] = {'textures': ce['textures'], 'bones': [b['name'] for b in geo['minecraft:geometry'][0]['bones']]}
    properties=load(BP/f'entities/bw/vv/vehicles/{name}.json')['minecraft:entity']['description']['properties']
    catalog['vehicles'][name]['hitResistance']=properties['bw_vv:hit_resistance']['default']
    catalog['vehicles'][name]['toolResistance']=properties['bw_vv:hit_resistance_tools']['default']
put(AS / 'animations/vehicle.animation.json', {'format_version': '1.8.0', 'animations': {'vehicle.idle': {'loop': True, 'animation_length': 1, 'bones': {}}}})
animations={'vehicle.idle': {'loop': True, 'animation_length': 1, 'bones': {}}}
for file in (RP/'animations/bw/vv/vehicles').glob('*.json'):
    for name, animation in load(file).get('animations',{}).items():
        if any(token in name for token in ['mech_walk_forward','mech_walk_backward','mech_walk_left','mech_walk_right','hot_air_balloon_idle','hot_air_balloon_deflated','prismer_swimming','prismer_standby','steamboat_idle']):
            animations[name]={k:v for k,v in animation.items() if k in ['loop','animation_length','bones']}
            if animations[name].get('loop')=='hold_on_last_frame': animations[name]['loop']=True
put(AS/'animations/vehicle.animation.json',{'format_version':'1.8.0','animations':animations})
for name, file in {'redstone_compressor':'models/entity/bw/vv/utilities/redstone_compactor.json','smoke_bomb':'models/entity/bw/vv/smoke_bomb.geo.json','wind_vane':'models/blocks/bw/vv/wind_vane.geo.json','mechanic':'models/entity/bw/vv/mobs/mechanic.json'}.items():
    geo=load(RP/file);geo['format_version']='1.12.0';put(AS/f'geo/{name}.geo.json',geo)
for name,file in {'flag':'models/blocks/bw/vv/flag.geo.json','flag_pole':'models/blocks/bw/vv/flag.pole.geo.json','mech_projectile':'models/entity/bw/vv/helpers/mech_projectile.geo.json','tesla_helmet':'models/entity/bw/vv/attachable/tesla_helmet.geo.json','prismer_aura':'models/entity/bw/vv/helpers/prismer_aura.geo.json'}.items():
    geo=load(RP/file);geo['format_version']='1.12.0';put(AS/f'geo/{name}.geo.json',geo)
# Split Bedrock UV sheets losslessly into direct Java texture frames. All source
# PNGs above remain byte-for-byte intact; derived frames have their own paths.
for vehicle, details in catalog['vehicles'].items():
    details['sheets']={}
    for key,texture in details['textures'].items():
        frames = 18 if key=='aura_animated' else 8 if key=='tentacles_animated' else 7 if key=='wings_animated' else 4 if vehicle in ('plane','mobtruck') else 5
        if not ('animated' in key or key=='fueled') or not (RP/(texture+'.png')).exists(): continue
        im=Image.open(RP/(texture+'.png'))
        if im.height%frames:raise ValueError('Invalid source UV sheet '+texture)
        for index in range(frames):
            dst=AS/f'textures/derived/{vehicle}/{key}/{index}.png';dst.parent.mkdir(parents=True,exist_ok=True)
            im.crop((0,index*im.height//frames,im.width,(index+1)*im.height//frames)).save(dst)
        details['sheets'][key]=frames
put(AS/'mechanic-trades.json',load(BP/'trading/bw/vv/mechanic_trades.json'))
definitions=load(RP/'sounds/sound_definitions.json').get('sound_definitions',{})
sounds={}
for name, definition in definitions.items():
    converted=[]
    for sound in definition.get('sounds',[]):
        if isinstance(sound,str): sound={'name':sound}
        path=sound['name'].removeprefix('sounds/').removesuffix('.ogg').removesuffix('.wav')
        converted.append({'name':'bw_vv:'+path,**{k:sound[k] for k in ['volume','pitch','stream','weight'] if k in sound}})
    sounds[name]={'sounds':converted}
put(AS/'sounds.json',sounds)

tex = load(RP / 'textures/item_texture.json')['texture_data']
for file in (BP / 'items').rglob('*.json'):
    item = load(file).get('minecraft:item')
    if not item: continue
    identifier = item['description']['identifier']
    if not identifier.startswith('bw_vv:'): continue
    name = identifier.split(':',1)[1]
    comp = item.get('components',{})
    icon = comp.get('minecraft:icon', {})
    if isinstance(icon, str): key = icon
    else: key = icon.get('textures',{}).get('default',icon.get('texture',''))
    index = icon.get('textures',{}).get('default',{}) if isinstance(icon,dict) else {}
    if isinstance(key, dict): key = key.get('texture','')
    texture = tex.get(key,{}).get('textures','textures/bw/vv/items/compressed_redstone')
    if isinstance(texture,list):
        texture_index = comp.get('minecraft:icon',{}).get('textures',{}).get('default',{}).get('index',0) if isinstance(index,dict) else 0
        texture = texture[min(texture_index,len(texture)-1)]
    if isinstance(texture,dict): texture = texture.get('path', 'textures/bw/vv/items/compressed_redstone')
    if texture.endswith('.png'): texture = texture[:-4]
    vehicle = None
    if 'minecraft:entity_placer' in comp:
        vehicle = comp['minecraft:entity_placer'].get('entity','').split(':')[-1]
    entry = {'id':name,'texture':texture,'vehicle':vehicle,'stack':comp.get('minecraft:max_stack_size',{}),'source':str(file.relative_to(BP)).replace('\\','/')}
    catalog['items'].append(entry)
    put(AS / f'models/item/{name}.json', {'parent':'minecraft:item/generated','textures':{'layer0':'bw_vv:'+texture.removeprefix('textures/')}})

ids = {i['id'] for i in catalog['items']}
# Bedrock keeps some wooden variants in entity events rather than separate item files.
woods = ['oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry','bamboo','crimson','warped','pale_oak']
for vehicle in ['logcar','plane']:
    for wood in woods:
        name = vehicle+'.'+wood
        if name in ids: continue
        texture = f'textures/bw/vv/items/vehicle_spawns/{vehicle}/{wood}'
        if not (RP/(texture+'.png')).exists(): texture = f'textures/bw/vv/items/vehicle_spawns/{vehicle}/oak'
        catalog['items'].append({'id':name, 'vehicle':vehicle,'texture':texture,'stack':{'value':1},'source':'entity variant'})
        ids.add(name)
        put(AS / f'models/item/{name}.json', {'parent':'minecraft:item/generated','textures':{'layer0':'bw_vv:'+texture.removeprefix('textures/')}})

tags = {'minecraft:planks':'minecraft:planks','minecraft:log':'minecraft:logs','minecraft:wool':'minecraft:wool','minecraft:fence':'minecraft:wooden_fences','minecraft:fence_gate':'minecraft:fence_gates','minecraft:wooden_slab':'minecraft:wooden_slabs','minecraft:wooden_stairs':'minecraft:wooden_stairs','minecraft:boat':'minecraft:boats'}
aliases = {'minecraft:stone_slab':'minecraft:smooth_stone_slab','minecraft:wood':'minecraft:oak_wood','minecraft:log2':'minecraft:acacia_log','minecraft:leather_helmet':'minecraft:leather_helmet','minecraft:banner':'minecraft:white_banner','minecraft:skull':'minecraft:skeleton_skull','minecraft:slime':'minecraft:slime_ball'}
def ingredient(value):
    item = value.get('item', '')
    if item in tags: return {'tag':tags[item]}
    return {'item':aliases.get(item,item)}
for file in (BP/'recipes').rglob('*.json'):
    data = load(file)
    kind = next((k for k in ['minecraft:recipe_shaped','minecraft:recipe_shapeless'] if k in data),None)
    if not kind:
        if 'minecraft:recipe_smithing_transform' in data:
            r=data['minecraft:recipe_smithing_transform']
            put(DATA/'tesla_helmet_upgrade.json',{'type':'minecraft:smithing_transform','template':{'item':r['template']},'base':{'item':r['base']},'addition':{'item':r['addition']},'result':{'id':r['result']}})
            catalog['recipes'].append('tesla_helmet_upgrade')
        else: catalog['recipeWarnings'].append(str(file.relative_to(BP))+': requires custom recipe conversion')
        continue
    recipe = data[kind]
    result = recipe.get('result')
    if isinstance(result,list): result = result[0]
    if not result: continue
    result_id = result['item']
    if result_id.startswith('bw_vv:') and result_id.split(':')[1] not in ids:
        catalog['recipeWarnings'].append(str(file.relative_to(BP))+': result requires block/utility registration '+result_id)
        continue
    out = {'type':'minecraft:crafting_shaped' if kind.endswith('shaped') else 'minecraft:crafting_shapeless','category':'misc','result':{'id':result_id,'count':result.get('count',1)}}
    if kind.endswith('shaped'):
        out['pattern']=recipe['pattern']; out['key']={k:ingredient(v) for k,v in recipe['key'].items()}
    else: out['ingredients']=[ingredient(v) for v in recipe['ingredients'] for _ in range(v.get('count',1))]
    name = recipe['description']['identifier'].split(':')[1]
    put(DATA / (name+'.json'),out)
    catalog['recipes'].append(name)

langs = {}
for line in (RP / 'texts/en_US.lang').read_text(encoding='utf-8-sig').splitlines():
    if not line or line.startswith('#') or '=' not in line: continue
    key, val = line.split('=',1)
    for token,replacement in {':_input_key.jump:':'Space',':_input_key.attack:':'Left-click',':_input_key.use:':'Right-click',':_input_key.sneak:':'Shift'}.items():val=val.replace(token,replacement)
    langs[key]=val
    if key.startswith('item.bw_vv:'): langs['item.bw_vv.'+key[len('item.bw_vv:'):]]=val
for vehicle in catalog['vehicles']:
    langs['entity.bw_vv.'+vehicle]=vehicle.replace('_',' ').title()
for color in ['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']:
    name='flag.'+color
    put(AS/f'blockstates/{name}.json',{'variants':{'':{'model':'bw_vv:block/'+name}}})
    put(AS/f'models/block/{name}.json',{'textures':{'particle':f'bw_vv:bw/vv/blocks/flags/{color}'}})
    put(RES/f'data/bw_vv/loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'bw_vv:'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
put(AS/'blockstates/flag.pole.json',{'variants':{'':{'model':'bw_vv:block/flag.pole'}}})
put(AS/'models/block/flag.pole.json',{'textures':{'particle':'bw_vv:bw/vv/blocks/flag.pole'}})
put(RES/'data/bw_vv/loot_table/blocks/flag.pole.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'bw_vv:flag.pole'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
langs['itemGroup.bw_vv']='Vanilla Vehicles - Local Test'
langs['entity.bw_vv.mechanic']='Mechanic'
langs['item.bw_vv.mechanic_spawn_egg']='Mechanic Spawn Egg'
put(AS/'models/item/mechanic_spawn_egg.json',{'parent':'minecraft:item/template_spawn_egg'})
put(AS/'blockstates/wind_vane.json',{'variants':{'':{'model':'bw_vv:block/wind_vane'}}})
put(AS/'models/block/wind_vane.json',{'textures':{'particle':'bw_vv:bw/vv/blocks/wind_vane'}})
put(RES/'data/bw_vv/loot_table/blocks/wind_vane.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'bw_vv:wind_vane'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
put(AS/'lang/en_us.json',langs)
pages={}
for key,value in langs.items():
    if key.startswith('bw_vv.guide_book.') and key.endswith('.title') and any(part in key for part in ('vehicle_page.','utility_page.','item_page.','mob_page.','system_page.')):
        prefix=key[:-5];pages[prefix]=[k for k in langs if k.startswith(prefix) and not k.endswith('.title')]
put(AS/'guide-pages.json',pages)
# Item textures live outside vanilla's item/ and block/ directories; explicitly
# include their original paths in the block/item atlas.
put(RES/'assets/minecraft/atlases/blocks.json',{'sources':[
    {'type':'minecraft:directory','source':'bw/vv/items','prefix':'bw/vv/items/'},
    {'type':'minecraft:directory','source':'bw/vv/blocks','prefix':'bw/vv/blocks/'}]})
put(AS/'catalog.json',catalog)
source_hash = hashlib.sha256(args.archive.read_bytes()).hexdigest() if args.archive else None
put(ROOT/'docs/asset-conversion.json',{'vehicles':len(catalog['vehicles']),'items':len(catalog['items']), 'recipeWarnings':catalog['recipeWarnings'], 'source_sha256':source_hash})
print('Transferred',len(catalog['vehicles']),'vehicle models and',len(catalog['items']),'items. Recipe warnings:',len(catalog['recipeWarnings']))
