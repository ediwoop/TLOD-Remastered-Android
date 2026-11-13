"""Build the experimental Android components. This does not create a playable APK."""
import os,pathlib,subprocess
root=pathlib.Path(__file__).resolve().parent.parent
jdk=pathlib.Path(os.environ.get('JAVA_HOME',str(root/'jdk25')))/'bin'
sdk=pathlib.Path(os.environ.get('ANDROID_SDK_ROOT',str(root/'android-sdk')))
android=list(sdk.glob('platforms/**/android.jar'))
if not android: raise SystemExit('Android platform android.jar is required')
android=sorted(android)[-1]
jars=list((pathlib.Path.home()/'.gradle/caches/modules-2/files-2.1').rglob('*.jar'))
if not jars: raise SystemExit('Resolve upstream Gradle dependencies first')
core=root/'source/build/classes/java/main'
if not core.exists(): raise SystemExit('Compile the core first with Gradle compileJava')
mod=root/'modloader_android';backend=root/'android_backend'
for p in [mod/'classes',mod/'dex',backend/'classes',backend/'dex']:p.mkdir(parents=True,exist_ok=True)
def run(args):subprocess.run([str(a) for a in args],cwd=root,check=True)
cp=os.pathsep.join(map(str,jars))
run([jdk/'javac','--release','17','-cp',cp,'-d',mod/'classes',*sorted((mod/'org').rglob('*.java'))])
cp=os.pathsep.join([str(mod/'classes'),str(core),str(android),cp])
run([jdk/'javac','--release','17','-cp',cp,'-d',backend/'classes',*sorted((backend/'src').rglob('*.java'))])
r8=root/'r8.jar'
if not r8.is_file():raise SystemExit('Provide R8 9.5.28 as r8.jar')
for part,classes in [(mod,[p for p in (mod/'classes').rglob('*.class') if p.name!='DesktopDiscovery.class']),(backend,list((backend/'classes').rglob('*.class')))]:
 run([jdk/'java','-cp',r8,'com.android.tools.r8.D8','--min-api','26','--lib',android,'--classpath',core,'--classpath',mod/'classes','--output',part/'dex',*sorted(classes)])
print('Compiled patched module loader and Android backend; runtime/device validation still required.')
