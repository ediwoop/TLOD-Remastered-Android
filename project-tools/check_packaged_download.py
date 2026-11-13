"""Verify the final APK contains native HD only, with no imported character classes/assets."""
from pathlib import Path
import zipfile,struct,subprocess,hashlib,shutil,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1];p=root/'android_app/build/Dragoon-Engine-Internal.apk';classes=set()
with zipfile.ZipFile(p)as z:
 assert z.testzip()is None
 for n in z.namelist():
  assert not any('assets/gfx/remaster/'+old+'/' in n for old in ['user-rose','rose-fbx','characters'])
  if n.endswith('.dex'):
   d=z.read(n);ns,so=struct.unpack_from('<II',d,56);nt,to=struct.unpack_from('<II',d,64);nc,co=struct.unpack_from('<II',d,96);strings=[]
   for i in range(ns):
    off=struct.unpack_from('<I',d,so+4*i)[0]
    while d[off]&128:off+=1
    off+=1;strings.append(d[off:d.index(b'\0',off)].decode('utf-8',errors='replace'))
   types=[strings[struct.unpack_from('<I',d,to+4*i)[0]]for i in range(nt)]
   classes.update(types[struct.unpack_from('<I',d,co+32*i)[0]]for i in range(nc))
 assert not any(n.startswith(("assets/gfx/", "assets/files/", "assets/patches/", "assets/android-shaders/", "assets/resources/", "assets/pal-data.")) for n in z.namelist())
 assert {"assets/content-download.properties", "assets/engine-assets.sha256"}.issubset(z.namelist())
 assert "Llegend/game/tmd/NativeRoseHd$Switched;" in classes
 assert "Lcom/realbox/dragoon/DownloadActivity;" in classes
 assert not any("MeshyRose29" in c or "ImportedRose" in c or "UserRoseAppearance" in c for c in classes)
manifest=ET.parse(root/'android_app/AndroidManifest.xml').getroot();ns='{http://schemas.android.com/apk/res/android}';code=manifest.attrib[ns+'versionCode'];version=manifest.attrib[ns+'versionName']
badging=subprocess.check_output([root/'android-sdk/build-tools/35.0.0/aapt','dump','badging',p],text=True);assert f"versionCode='{code}'"in badging and f"versionName='{version}'"in badging
assert 'v3): true'in(root/'android_app/build/signature-check.log').read_text()
h=hashlib.sha256(p.read_bytes()).hexdigest();p.with_suffix('.apk.sha256').write_text(h+'  '+p.name+'\n');out=root.parent/('deliverables/Legend-of-Dragoon-Remastered-'+version.split('-')[0]+'.apk');out.parent.mkdir(exist_ok=True);shutil.copyfile(p,out)
print('PASS: 0.28 native HD Rose and downloader classes; no embedded game content')
print('PASS: manifest version '+code+', v3 signature and archive integrity; SHA256 '+h)
print(out)
