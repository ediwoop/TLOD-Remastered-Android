"""Build vertex/fragment-only TMD shaders, retaining the exported game math.

Per-face positions supplied by TmdTriangleData replace the geometry stage's
near-face culling and averaged PS1 depth. Use plain varyings and deterministic
initial values on mobile drivers. Generated assets are reviewed with a raster
comparison against the original vertex/geometry/fragment programs.
"""
from pathlib import Path
import re

root = Path(__file__).resolve().parent.parent
folder = root / 'android_backend/shaders'
members = re.compile(r'((?:flat\s+)?(?:highp\s+)?)(vec[234]|int|float)\s+(\w+)\s*;')


def block_name(source, name):
    return re.search(r'layout\(std140\) uniform ' + name + r'\s*\{[^}]+}\s*(\w+);', source).group(1)


def make(vertex_name, fragment_name, output):
    vertex = (folder / vertex_name).read_text()
    fragment = (folder / fragment_name).read_text()
    block = re.search(r'layout\(location = 0\) out VS_OUT\s*\{(.*?)\}\s*vs_out;', vertex, re.S)
    declarations, initializers = [], []
    for qualifiers, kind, name in members.findall(block.group(1)):
        if name == 'viewspaceZ':
            continue
        declarations.append(('flat ' if 'flat' in qualifiers else '') + f'out highp {kind} v_{name};')
        value = 'vec4(1.0)' if name == 'vertColour' else '1.0' if name == 'widthMultiplier' else '255' if name == 'indexMask' else '0' if kind == 'int' else '0.0' if kind == 'float' else f'{kind}(0.0)'
        initializers.append(f'    v_{name} = {value};')
    vertex = vertex.replace(block.group(), '\n'.join(declarations) + '\nfloat compatViewspaceZ;')
    vertex = vertex.replace('vs_out.viewspaceZ', 'compatViewspaceZ')
    vertex = re.sub(r'vs_out\.(\w+)', r'v_\1', vertex)
    vertex = vertex.replace('void main()\n{', 'void shadeVertex()\n{\n' + '\n'.join(initializers), 1)
    transforms = block_name(vertex, 'transforms')
    models = block_name(vertex, 'transforms2')
    info = block_name(vertex, 'projectionInfo')
    vertex += f'''
// Same four face positions on every emitted triangle vertex. For a triangle,
// inFace0.w is zero and inFace3 is ignored. A split quad keeps its fourth point.
layout(location = 7) in vec4 inFace0;
layout(location = 8) in vec4 inFace1;
layout(location = 9) in vec4 inFace2;
layout(location = 10) in vec4 inFace3;
uniform int usePs1Depth;

vec4 clipFacePosition(vec4 viewPosition, vec4 offset)
{{
    if ({info}.projectionMode == 1.0)
    {{
        float z = clamp(viewPosition.z, 0.0, 65536.0);
        if (z != 0.0) viewPosition.xy *= {info}.zfar / z;
    }}
    if ({info}.projectionMode != 2.0) viewPosition.z += offset.z;
    viewPosition.xy += offset.xy;
    return {transforms}.projection * viewPosition;
}}

void main()
{{
    shadeVertex();
    mat4 viewModel = {transforms}.camera * {models}.modelTransforms[int(modelIndex)].model;
    vec4 offset = {models}.modelTransforms[int(modelIndex)].screenOffset;
    vec4 a = viewModel * vec4(inFace0.xyz, 1.0);
    vec4 b = viewModel * vec4(inFace1.xyz, 1.0);
    vec4 c = viewModel * vec4(inFace2.xyz, 1.0);
    vec4 d = viewModel * vec4(inFace3.xyz, 1.0);
    bool quad = inFace0.w != 0.0;
    if ({info}.zfar >= a.z * 2.0 || {info}.zfar >= b.z * 2.0 ||
        {info}.zfar >= c.z * 2.0 || (quad && {info}.zfar >= d.z * 2.0))
    {{
        // The original geometry stage drops the whole face in this case.
        gl_Position = vec4(0.0, 0.0, 2.0, 1.0);
        return;
    }}
    if (usePs1Depth != 0)
    {{
        float depth = clipFacePosition(a, offset).z + clipFacePosition(b, offset).z + clipFacePosition(c, offset).z;
        if (quad) depth += clipFacePosition(d, offset).z;
        gl_Position.z = depth / (quad ? 4.0 : 3.0);
    }}
}}
'''
    block = re.search(r'layout\(location = 0\) in GS_OUT\s*\{(.*?)\}\s*_79;', fragment, re.S)
    declarations = []
    for qualifiers, kind, name in members.findall(block.group(1)):
        declarations.append(('flat ' if 'flat' in qualifiers else '') + f'in highp {kind} v_{name};')
    fragment = fragment.replace(block.group(), '\n'.join(declarations))
    fragment = re.sub(r'_79\.(\w+)', r'v_\1', fragment)
    # ES 3.0 binds samplers by name through the existing ShaderOptions factory.
    fragment = re.sub(r'layout\([^)]*\)\s*(uniform\s+highp\s+u?sampler2D)', r'\1', fragment)
    for suffix, source in [('vsh', vertex), ('fsh', fragment)]:
        source = source.replace('#version 320 es', '#version 300 es', 1)
        source = source.replace('precision mediump float;', 'precision highp float;')
        if suffix == 'vsh':
            source = source.replace('#version 300 es\n', '#version 300 es\nprecision highp float;\nprecision highp int;\n', 1)
        if 'vs_out.' in source or '_79.' in source:
            raise ValueError('Unconverted TMD interface')
        (folder / f'{output}.{suffix}').write_text(source)


make('tmd.vsh', 'tmd.fsh', 'compat_tmd')
make('battle_tmd.vsh', 'battle_tmd.fsh', 'compat_battle_tmd')
print('Generated GLES triangle TMD programs with face culling, depth and defined varyings.')

# Standard FBX material path is shared verbatim with desktop, with ES precision declarations.
for ext in ("vsh","fsh"):
    text=(root/f"source/gfx/shaders/standard.{ext}").read_text().replace("#version 330 core","#version 320 es\nprecision highp float;\nprecision highp int;\nprecision highp usampler2D;")
    # ES does not permit implicit numeric conversions accepted by desktop GLSL.
    text=re.sub(r"(projectionMode|anim.x|translucency|useTextureAlpha|discardTranslucency|alpha|texColour\.[argb]|tpageOverride.x|clutOverride.x|t.screenOffset.z|\bz)(\s*[!=<>]=?\s*)(-?\d+)(?![.\d])",r"\1\2\3.0",text)
    text=text.replace("vertClut.x + p","vertClut.x + float(p)").replace("1.0 / widthDivisor","1.0 / float(widthDivisor)").replace("pow(16, vertBpp + 1) - 1","pow(16.0, float(vertBpp + 1)) - 1.0").replace("clamp(gl_Position.z, 0, 65536)","clamp(gl_Position.z, 0.0, 65536.0)").replace("intTpage & 0x10) != 0 ? 256 : 0","intTpage & 0x10) != 0 ? 256.0 : 0.0").replace("pixel >> 15 & 0x1fu","pixel >> 15u & 0x1fu").replace("pixel >> 10 & 0x1fu","pixel >> 10u & 0x1fu").replace("pixel >>  5 & 0x1fu","pixel >>  5u & 0x1fu").replace("vertClut.y), 0).r","int(vertClut.y)), 0).r")
    (folder/f"standard.{ext}").write_text(text)
