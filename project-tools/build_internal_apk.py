"""Package the actual core and Android host for internal runtime testing; not a game release."""
import pathlib,os,json,subprocess,zipfile,shutil
import hashlib,tempfile
root=pathlib.Path(__file__).resolve().parent.parent
jdk=pathlib.Path(os.environ.get('JAVA_HOME',str(root/'jdk25')))/'bin'
sdk=pathlib.Path(os.environ.get('ANDROID_SDK_ROOT',str(root/'android-sdk')))
android=sorted(sdk.glob('platforms/**/android.jar'))[-1]
bt=sdk/'build-tools/35.0.0'; app=root/'android_app';build=app/'build'
for name in ['classes','deps','dex']: (build/name).mkdir(parents=True,exist_ok=True)
def run(args,log=None,cwd=root):
 if log:
  with open(log,'w') as stream:subprocess.run([str(a) for a in args],cwd=cwd,stdout=stream,stderr=subprocess.STDOUT,check=True)
 else:subprocess.run([str(a) for a in args],cwd=cwd,check=True)
subprocess.run([os.sys.executable,str(root/'project-tools/check_android_native27.py')],cwd=root,check=True)
subprocess.run([os.sys.executable,str(root/'project-tools/build_compat_shaders.py')],cwd=root,check=True)
# The downloadable content is built separately so the APK carries code and configuration only.
from urllib.parse import urlsplit
url = os.environ.get('DRAGOON_CONTENT_URL', '')
content = os.environ.get('DRAGOON_CONTENT_METADATA', '')
if urlsplit(url).scheme != 'https' or not urlsplit(url).hostname or not content:
 raise SystemExit('Set DRAGOON_CONTENT_URL to the HTTPS release asset and DRAGOON_CONTENT_METADATA to the package JSON.')
metadata = json.loads(pathlib.Path(content).read_text())
assets = pathlib.Path(tempfile.mkdtemp(prefix='dragoon-apk-assets-'))
(assets/'engine-assets.sha256').write_text(metadata['assetHash'])
(assets/'content-download.properties').write_text(
 f"url={url}\nsize={metadata['size']}\nexpanded={metadata['expanded']}\nsha256={metadata['sha256']}\n"
)
cp=os.pathsep.join(map(str,[root/'modloader_android/classes',root/'source/build/classes/java/main',root/'android_backend/classes',android,*sorted((pathlib.Path.home()/'.gradle/caches/modules-2/files-2.1/org.joml').rglob('*.jar'))]))
run([jdk/'javac','--release','17','-cp',cp,'-d',build/'classes',*sorted((app/'src').rglob('*.java'))])
def jar_from(out,*folders,exclude=()):
 with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
  seen=set()
  for folder in folders:
   for f in sorted(folder.rglob('*')):
    if f.is_file() and f.name not in exclude:
     name=f.relative_to(folder).as_posix()
     if name not in seen:z.write(f,name);seen.add(name)
engine=build/'engine.jar';mods=build/'mod-loader-builtins.jar';host=build/'app-host.jar'
jar_from(engine,root/'source/build/classes/java/main',root/'source/src/main/resources')
jar_from(mods,root/'modloader_android/classes',exclude=('DesktopDiscovery.class',))
jar_from(host,build/'classes',root/'android_backend/classes')
cache=pathlib.Path.home()/'.gradle/caches/modules-2/files-2.1';deps=[];dependency_resources={}
for recorded in json.loads((root/'android_backend/bootstrap-dependencies.json').read_text()):
 name=pathlib.Path(recorded).name
 if name in ('dragoon-engine-test.jar','mod-loader-builtins.jar'):continue
 matches=sorted(cache.rglob(name))
 if not matches:raise SystemExit('Missing dependency '+name)
 target=build/'deps'/name
 with zipfile.ZipFile(matches[0]) as src,zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as dst:
  for n in src.namelist():
   if n.endswith('.class') and not n.startswith('META-INF/') and not n.endswith('module-info.class'):dst.writestr(n,src.read(n))
   elif not n.endswith('/') and not n.endswith('.class') and (not n.startswith('META-INF/') or n=='META-INF/services/org.apache.logging.log4j.util.PropertySource'):
    data=src.read(n)
    if n in dependency_resources and dependency_resources[n]!=data:raise ValueError('Conflicting Java resource '+n)
    dependency_resources[n]=data
 deps.append(target)
with tempfile.TemporaryDirectory(prefix='fresh-dex-',dir=build) as fresh_dex:
 run([jdk/'java','-Xmx3g','-cp',root/'r8.jar','com.android.tools.r8.D8','--min-api','35','--lib',android,'--output',fresh_dex,engine,mods,host,*deps],build/'d8.log')
 dex_files=sorted(pathlib.Path(fresh_dex).glob('*.dex'))
 if not dex_files:raise RuntimeError('D8 generated no DEX')
 for old in (build/'dex').glob('*.dex'):old.unlink()
 for generated in dex_files:shutil.copyfile(generated,build/'dex'/generated.name)
# aapt may retain stale ZIP members when reusing an output archive.
(build/'unsigned.apk').unlink(missing_ok=True)
run([bt/'aapt','package','-f','-M',app/'AndroidManifest.xml','-I',android,'-A',assets,'-0','zip','-F',build/'unsigned.apk'])
with zipfile.ZipFile(build/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
 for d in sorted((build/'dex').glob('*.dex')):z.write(d,d.name)
 for path in sorted((root/'source/src/main/resources').rglob('*')):
  if path.is_file():
   name=path.relative_to(root/'source/src/main/resources').as_posix()
   data=path.read_bytes()
   if name in dependency_resources and dependency_resources[name]!=data:raise ValueError('Conflicting engine resource '+name)
   dependency_resources[name]=data
 for name,data in sorted(dependency_resources.items()):z.writestr(name,data)
run([bt/'zipalign','-f','4',build/'unsigned.apk',build/'aligned.apk'])
key=root/'signing/debug.keystore'
if not key.exists():raise SystemExit('Provide the preserved internal signing key at signing/debug.keystore')
env=os.environ.copy();env['JAVA_HOME']=str(jdk.parent)
subprocess.run([str(bt/'apksigner'),'sign','--ks',str(key),'--ks-pass','pass:android','--key-pass','pass:android','--out',str(build/'candidate.apk'),str(build/'aligned.apk')],cwd=root,env=env,check=True)
os.replace(build/'candidate.apk',build/'Dragoon-Engine-Internal.apk')
with open(build/'signature-check.log','w') as log:subprocess.run([str(bt/'apksigner'),'verify','--verbose',str(build/'Dragoon-Engine-Internal.apk')],env=env,stdout=log,check=True)
run([os.sys.executable,root/'project-tools/check_packaged_download.py'],build/'packaged-code-check.log')
shutil.rmtree(assets)
print('Created internal engine APK; package code checked; physical device validation still required.')
