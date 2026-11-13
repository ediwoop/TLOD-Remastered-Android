"""Build the Android content archive independently of the APK toolchain."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
DIRECTORIES = ("gfx", "patches", "resources", "android-shaders")

def stage_assets(root, assets):
    # Explicitly retired Rose profile, never retain it in an incremental asset copy.
    for retired_model in ['user-rose','rose-fbx','characters']:shutil.rmtree(assets/'gfx/remaster'/retired_model,ignore_errors=True)
    for source,target in [(root/'source/gfx',assets/'gfx'),(root/'source/patches',assets/'patches'),(root/'source/src/main/resources',assets/'resources'),(root/'android_backend/shaders',assets/'android-shaders')]:shutil.copytree(source,target,dirs_exist_ok=True)
    # Ship only the finished single plate for ready entries. Masters stay in the source project.
    for metadata in (assets/'gfx/remaster/submaps').glob('*.properties'):
     properties=dict(line.split('=',1) for line in metadata.read_text().splitlines() if '=' in line and not line.lstrip().startswith('#'))
     if properties.get('ready','').strip()=='true':
      cut=metadata.stem
      if not (metadata.parent/(cut+'-ready.png')).is_file():raise SystemExit('Missing finished plate '+cut)
      for name in [cut+'.png',cut+'-surround.png',cut+'-master20.png',cut+'-master21.png',cut+'-master22.png']:(metadata.parent/name).unlink(missing_ok=True)

    # Original Rose remains native; the approved user texture profile is selected only in Remaster.
    for retired in (assets/'gfx/remaster/characters').glob('rose*.png'):
     retired.unlink()
    shutil.copy2(root/'android_backend/media/intro.mp4',assets/'gfx/intro.mp4')
    asset_hash=hashlib.sha256()
    for directory in ['gfx','patches','resources','android-shaders']:
     for path in sorted((assets/directory).rglob('*')):
      if path.is_file():asset_hash.update(path.relative_to(assets).as_posix().encode()+b'\0');asset_hash.update(path.read_bytes())
    (assets/'engine-assets.sha256').write_text(asset_hash.hexdigest())

def build_content(pal, output):
    output = output.resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run([sys.executable, ROOT/'project-tools/build_compat_shaders.py'], check=True)
    with tempfile.TemporaryDirectory() as temporary:
        assets = Path(temporary)
        stage_assets(ROOT, assets)
        with zipfile.ZipFile(pal) as disc, zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED, compresslevel=3) as pack:
            if "files/pal-source.sha256" not in disc.namelist():
                raise ValueError("Expected prepared PAL game data under files/")
            for directory in DIRECTORIES:
                pack.write(assets/directory, directory)
                for path in sorted((assets/directory).rglob("*")):
                    if path.is_file():
                        pack.write(path, path.relative_to(assets).as_posix())
            for info in disc.infolist():
                relative = Path(info.filename)
                if relative.is_absolute() or ".." in relative.parts or not relative.parts or relative.parts[0] != "files":
                    raise ValueError("Unexpected prepared PAL archive member")
                if info.is_dir():
                    pack.writestr(info.filename, b"")
                else:
                    with disc.open(info) as source, pack.open(info.filename, "w", force_zip64=True) as target:
                        shutil.copyfileobj(source, target, 1024 * 1024)
        with zipfile.ZipFile(output) as pack:
            expanded = sum(info.file_size for info in pack.infolist())
            if pack.testzip() is not None:
                raise ValueError("Content archive failed its CRC check")
        with output.open("rb") as stream:
            digest = hashlib.file_digest(stream, "sha256").hexdigest()
        metadata = {"assetHash": (assets/"engine-assets.sha256").read_text().strip(), "sha256": digest,
                    "size": output.stat().st_size, "expanded": expanded}
        output.with_suffix(".json").write_text(json.dumps(metadata, indent=2) + "\n")
        print(metadata)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("pal", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    build_content(args.pal, args.output)
