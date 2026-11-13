#version 300 es
precision highp float;
precision highp int;

struct ModelTransforms
{
    mat4 model;
    vec4 screenOffset;
};

struct Light
{
    mat4 lightDirection;
    mat3 lightColour;
    vec4 backgroundColour;
};

layout(std140) uniform transforms2
{
    ModelTransforms modelTransforms[128];
} _88;

layout(std140) uniform lighting
{
    Light lights[128];
} _109;

layout(std140) uniform clutAnimation
{
    vec4 clutAnimations[1024];
} _320;

layout(std140) uniform transforms
{
    mat4 camera;
    mat4 projection;
} _392;

layout(std140) uniform projectionInfo
{
    float znear;
    float zfar;
    float zdiffInv;
    float projectionMode;
} _408;

uniform int ctmdFlags;
uniform float modelIndex;
uniform vec3 battleColour;
uniform vec2 tpageOverride;
uniform vec2 clutOverride;

layout(location = 0) in vec4 inPos;
out highp vec2 v_vertUv;
flat out highp vec2 v_vertTpage;
flat out highp vec2 v_vertClut;
flat out highp int v_vertBpp;
out highp vec4 v_vertColour;
flat out highp int v_vertFlags;
flat out highp int v_translucency;
flat out highp float v_widthMultiplier;
flat out highp int v_widthMask;
flat out highp int v_indexShift;
flat out highp int v_indexMask;
out highp float v_depth;
out highp float v_depthOffset;
float compatViewspaceZ;

layout(location = 6) in float inFlags;
layout(location = 5) in vec4 inColour;
layout(location = 1) in vec3 inNorm;
layout(location = 3) in float inTpage;
layout(location = 4) in float inClut;
layout(location = 2) in vec2 inUv;

void shadeVertex()
{
    v_vertUv = vec2(0.0);
    v_vertTpage = vec2(0.0);
    v_vertClut = vec2(0.0);
    v_vertBpp = 0;
    v_vertColour = vec4(1.0);
    v_vertFlags = 0;
    v_translucency = 0;
    v_widthMultiplier = 1.0;
    v_widthMask = 0;
    v_indexShift = 0;
    v_indexMask = 255;
    v_depth = 0.0;
    v_depthOffset = 0.0;
    vec4 pos = vec4(inPos.xyz, 1.0);
    v_vertFlags = int(inFlags);
    bool ctmd = (ctmdFlags & 32) != 0;
    bool uniformLit = (ctmdFlags & 16) != 0;
    bool _52 = (v_vertFlags & 8) != 0;
    bool _60;
    if (!_52)
    {
        _60 = (ctmdFlags & 2) != 0;
    }
    else
    {
        _60 = _52;
    }
    bool translucent = _60;
    bool coloured = (v_vertFlags & 4) != 0;
    bool textured = (v_vertFlags & 2) != 0;
    bool lit = (v_vertFlags & 1) != 0;
    int _92 = int(modelIndex);
    ModelTransforms t;
    t.model = _88.modelTransforms[_92].model;
    t.screenOffset = _88.modelTransforms[_92].screenOffset;
    int _111 = int(modelIndex);
    Light l;
    l.lightDirection = _109.lights[_111].lightDirection;
    l.lightColour = _109.lights[_111].lightColour;
    l.backgroundColour = _109.lights[_111].backgroundColour;
    bool _127 = (textured && translucent) && (!lit);
    bool _133;
    if (_127)
    {
        _133 = ctmd || uniformLit;
    }
    else
    {
        _133 = _127;
    }
    if (_133)
    {
        vec3 _142 = inColour.xyz * battleColour;
        v_vertColour.x = _142.x;
        v_vertColour.y = _142.y;
        v_vertColour.z = _142.z;
        if (v_vertColour.x > 2.0)
        {
            v_vertColour.x = mod(v_vertColour.x, 2.0);
        }
        if (v_vertColour.y > 2.0)
        {
            v_vertColour.y = mod(v_vertColour.y, 2.0);
        }
        if (v_vertColour.z > 2.0)
        {
            v_vertColour.z = mod(v_vertColour.z, 2.0);
        }
    }
    else
    {
        if (lit)
        {
            float range = 1.0;
            if (textured)
            {
                range = 2.0;
            }
            vec3 _222 = clamp(clamp((l.lightColour * clamp(l.lightDirection * vec4(inNorm, 1.0), vec4(0.0), vec4(8.0)).xyz) + l.backgroundColour.xyz, vec3(0.0), vec3(8.0)) * inColour.xyz, vec3(0.0), vec3(range));
            v_vertColour.x = _222.x;
            v_vertColour.y = _222.y;
            v_vertColour.z = _222.z;
        }
        else
        {
            if (coloured)
            {
                v_vertColour = inColour;
            }
            else
            {
                v_vertColour = vec4(1.0);
            }
        }
    }
    int intTpage = int(inTpage);
    v_vertBpp = (intTpage >> 7) & 3;
    v_translucency = (intTpage >> 5) & 3;
    if (textured)
    {
        if (tpageOverride.x == 0.0)
        {
            v_vertTpage = vec2(float((intTpage & 15) * 64), float(((intTpage & 16) != 0) ? 256 : 0));
        }
        else
        {
            v_vertTpage = tpageOverride;
        }
        if (clutOverride.x == 0.0)
        {
            int intClut = int(inClut);
            v_vertClut = vec2(float((intClut & 63) * 16), float(intClut >> 6));
        }
        else
        {
            v_vertClut = clutOverride;
        }
        for (int clutAnimationIndex = 0; clutAnimationIndex < 1024; clutAnimationIndex++)
        {
            vec4 anim = _320.clutAnimations[clutAnimationIndex];
            if (anim.x == (-1.0))
            {
                break;
            }
            if (all(equal(anim.xy, v_vertClut)))
            {
                v_vertClut = anim.zw;
                break;
            }
        }
        bool _349 = v_vertBpp == 0;
        bool _356;
        if (!_349)
        {
            _356 = v_vertBpp == 1;
        }
        else
        {
            _356 = _349;
        }
        if (_356)
        {
            int widthDivisor = 1 << (2 - v_vertBpp);
            v_widthMultiplier = 1.0 / float(widthDivisor);
            v_widthMask = widthDivisor - 1;
            v_indexShift = v_vertBpp + 2;
            v_indexMask = int(pow(16.0, float(v_vertBpp + 1)) - 1.0);
        }
    }
    gl_Position = (_392.camera * t.model) * pos;
    compatViewspaceZ = gl_Position.z;
    if (_408.projectionMode == 1.0)
    {
        float z = clamp(gl_Position.z, 0.0, 65536.0);
        if (z != 0.0)
        {
            vec4 _429 = gl_Position;
            vec2 _431 = _429.xy * (_408.zfar / z);
            gl_Position.x = _431.x;
            gl_Position.y = _431.y;
        }
    }
    if (_408.projectionMode == 2.0)
    {
        v_depthOffset = t.screenOffset.z * _408.zdiffInv;
    }
    else
    {
        if (t.screenOffset.z != 0.0)
        {
            gl_Position.z += t.screenOffset.z;
        }
    }
    vec4 _464 = gl_Position;
    vec2 _466 = _464.xy + t.screenOffset.xy;
    gl_Position.x = _466.x;
    gl_Position.y = _466.y;
    gl_Position = _392.projection * gl_Position;
    v_vertUv = inUv;
    v_depth = gl_Position.z;
}


// Same four face positions on every emitted triangle vertex. For a triangle,
// inFace0.w is zero and inFace3 is ignored. A split quad keeps its fourth point.
layout(location = 7) in vec4 inFace0;
layout(location = 8) in vec4 inFace1;
layout(location = 9) in vec4 inFace2;
layout(location = 10) in vec4 inFace3;
uniform int usePs1Depth;

vec4 clipFacePosition(vec4 viewPosition, vec4 offset)
{
    if (_408.projectionMode == 1.0)
    {
        float z = clamp(viewPosition.z, 0.0, 65536.0);
        if (z != 0.0) viewPosition.xy *= _408.zfar / z;
    }
    if (_408.projectionMode != 2.0) viewPosition.z += offset.z;
    viewPosition.xy += offset.xy;
    return _392.projection * viewPosition;
}

void main()
{
    shadeVertex();
    mat4 viewModel = _392.camera * _88.modelTransforms[int(modelIndex)].model;
    vec4 offset = _88.modelTransforms[int(modelIndex)].screenOffset;
    vec4 a = viewModel * vec4(inFace0.xyz, 1.0);
    vec4 b = viewModel * vec4(inFace1.xyz, 1.0);
    vec4 c = viewModel * vec4(inFace2.xyz, 1.0);
    vec4 d = viewModel * vec4(inFace3.xyz, 1.0);
    bool quad = inFace0.w != 0.0;
    if (_408.zfar >= a.z * 2.0 || _408.zfar >= b.z * 2.0 ||
        _408.zfar >= c.z * 2.0 || (quad && _408.zfar >= d.z * 2.0))
    {
        // The original geometry stage drops the whole face in this case.
        gl_Position = vec4(0.0, 0.0, 2.0, 1.0);
        return;
    }
    if (usePs1Depth != 0)
    {
        float depth = clipFacePosition(a, offset).z + clipFacePosition(b, offset).z + clipFacePosition(c, offset).z;
        if (quad) depth += clipFacePosition(d, offset).z;
        gl_Position.z = depth / (quad ? 4.0 : 3.0);
    }
}
