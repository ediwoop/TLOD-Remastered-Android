"""Compile the supplied Rose loader against Android's JSON API, before APK packaging.
Desktop org.json has extra methods that Android's boot class loader cannot supply.
"""
from pathlib import Path
import os,subprocess,tempfile
root=Path(__file__).resolve().parents[1]
jdk=Path(os.environ.get('JAVA_HOME',root.parent/'runtime/extracted/toolchain-setup/jdk25'))/'bin'
sdk=Path(os.environ.get('ANDROID_SDK_ROOT',root/'android-sdk'))
android=sorted(sdk.glob('platforms/**/android.jar'))[-1]
jars=sorted((Path.home()/'.gradle/caches/modules-2/files-2.1').rglob('*.jar'))
cp=os.pathsep.join(map(str,[android,root/'source/build/classes/java/main',root/'modloader_android/classes',*jars]))
with tempfile.TemporaryDirectory(prefix='dragoon-android-api-') as out:
 subprocess.run([str(jdk/'javac'),'--release','17','-cp',cp,'-d',out,str(root/'source/src/main/java/legend/game/tmd/NativeRoseHd.java')],check=True)
for name in ['Scus94491BpeSegment.class','Scus94491BpeSegment$5.class']:
 file=root/'source/build/classes/java/main/legend/game'/name
 if file.exists():assert b'javafx/application/Platform' not in file.read_bytes(),name+' calls JavaFX from shared shutdown'
print('PASS: native Rose HD loader compiles against Android 35 API; shared game close has no JavaFX call')
