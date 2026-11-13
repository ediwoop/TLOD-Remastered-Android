#version 320 es

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
layout(location = 0) out VS_OUT
{
    vec2 vertUv;
    flat vec2 vertTpage;
    flat vec2 vertClut;
    flat int vertBpp;
    vec4 vertColour;
    flat int vertFlags;
    flat int translucency;
    flat float widthMultiplier;
    flat int widthMask;
    flat int indexShift;
    flat int indexMask;
    float viewspaceZ;
    float depth;
    float depthOffset;
} vs_out;

layout(location = 6) in float inFlags;
layout(location = 5) in vec4 inColour;
layout(location = 1) in vec3 inNorm;
layout(location = 3) in float inTpage;
layout(location = 4) in float inClut;
layout(location = 2) in vec2 inUv;

void main()
{
    vec4 pos = vec4(inPos.xyz, 1.0);
    vs_out.vertFlags = int(inFlags);
    bool ctmd = (ctmdFlags & 32) != 0;
    bool uniformLit = (ctmdFlags & 16) != 0;
    bool _52 = (vs_out.vertFlags & 8) != 0;
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
    bool coloured = (vs_out.vertFlags & 4) != 0;
    bool textured = (vs_out.vertFlags & 2) != 0;
    bool lit = (vs_out.vertFlags & 1) != 0;
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
        vs_out.vertColour.x = _142.x;
        vs_out.vertColour.y = _142.y;
        vs_out.vertColour.z = _142.z;
        if (vs_out.vertColour.x > 2.0)
        {
            vs_out.vertColour.x = mod(vs_out.vertColour.x, 2.0);
        }
        if (vs_out.vertColour.y > 2.0)
        {
            vs_out.vertColour.y = mod(vs_out.vertColour.y, 2.0);
        }
        if (vs_out.vertColour.z > 2.0)
        {
            vs_out.vertColour.z = mod(vs_out.vertColour.z, 2.0);
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
            vs_out.vertColour.x = _222.x;
            vs_out.vertColour.y = _222.y;
            vs_out.vertColour.z = _222.z;
        }
        else
        {
            if (coloured)
            {
                vs_out.vertColour = inColour;
            }
            else
            {
                vs_out.vertColour = vec4(1.0);
            }
        }
    }
    int intTpage = int(inTpage);
    vs_out.vertBpp = (intTpage >> 7) & 3;
    vs_out.translucency = (intTpage >> 5) & 3;
    if (textured)
    {
        if (tpageOverride.x == 0.0)
        {
            vs_out.vertTpage = vec2(float((intTpage & 15) * 64), float(((intTpage & 16) != 0) ? 256 : 0));
        }
        else
        {
            vs_out.vertTpage = tpageOverride;
        }
        if (clutOverride.x == 0.0)
        {
            int intClut = int(inClut);
            vs_out.vertClut = vec2(float((intClut & 63) * 16), float(intClut >> 6));
        }
        else
        {
            vs_out.vertClut = clutOverride;
        }
        for (int clutAnimationIndex = 0; clutAnimationIndex < 1024; clutAnimationIndex++)
        {
            vec4 anim = _320.clutAnimations[clutAnimationIndex];
            if (anim.x == (-1.0))
            {
                break;
            }
            if (all(equal(anim.xy, vs_out.vertClut)))
            {
                vs_out.vertClut = anim.zw;
                break;
            }
        }
        bool _349 = vs_out.vertBpp == 0;
        bool _356;
        if (!_349)
        {
            _356 = vs_out.vertBpp == 1;
        }
        else
        {
            _356 = _349;
        }
        if (_356)
        {
            int widthDivisor = 1 << (2 - vs_out.vertBpp);
            vs_out.widthMultiplier = 1.0 / float(widthDivisor);
            vs_out.widthMask = widthDivisor - 1;
            vs_out.indexShift = vs_out.vertBpp + 2;
            vs_out.indexMask = int(pow(16.0, float(vs_out.vertBpp + 1)) - 1.0);
        }
    }
    gl_Position = (_392.camera * t.model) * pos;
    vs_out.viewspaceZ = gl_Position.z;
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
        vs_out.depthOffset = t.screenOffset.z * _408.zdiffInv;
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
    vs_out.vertUv = inUv;
    vs_out.depth = gl_Position.z;
}

