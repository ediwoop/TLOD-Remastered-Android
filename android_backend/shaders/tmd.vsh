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
} _63;

layout(std140) uniform lighting
{
    Light lights[128];
} _84;

layout(std140) uniform clutAnimation
{
    vec4 clutAnimations[1024];
} _242;

layout(std140) uniform transforms
{
    mat4 camera;
    mat4 projection;
} _315;

layout(std140) uniform projectionInfo
{
    float znear;
    float zfar;
    float zdiffInv;
    float projectionMode;
} _331;

uniform float modelIndex;
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
layout(location = 1) in vec3 inNorm;
layout(location = 5) in vec4 inColour;
layout(location = 3) in float inTpage;
layout(location = 4) in float inClut;
layout(location = 2) in vec2 inUv;

void main()
{
    vec4 pos = vec4(inPos.xyz, 1.0);
    vs_out.vertFlags = int(inFlags);
    bool coloured = (vs_out.vertFlags & 4) != 0;
    bool textured = (vs_out.vertFlags & 2) != 0;
    bool lit = (vs_out.vertFlags & 1) != 0;
    int _67 = int(modelIndex);
    ModelTransforms t;
    t.model = _63.modelTransforms[_67].model;
    t.screenOffset = _63.modelTransforms[_67].screenOffset;
    int _86 = int(modelIndex);
    Light l;
    l.lightDirection = _84.lights[_86].lightDirection;
    l.lightColour = _84.lights[_86].lightColour;
    l.backgroundColour = _84.lights[_86].backgroundColour;
    if (lit)
    {
        float range = 1.0;
        if (textured)
        {
            range = 2.0;
        }
        vec3 _139 = clamp(clamp((l.lightColour * clamp(l.lightDirection * vec4(inNorm, 1.0), vec4(0.0), vec4(8.0)).xyz) + l.backgroundColour.xyz, vec3(0.0), vec3(8.0)) * inColour.xyz, vec3(0.0), vec3(range));
        vs_out.vertColour.x = _139.x;
        vs_out.vertColour.y = _139.y;
        vs_out.vertColour.z = _139.z;
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
            vec4 anim = _242.clutAnimations[clutAnimationIndex];
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
        bool _271 = vs_out.vertBpp == 0;
        bool _278;
        if (!_271)
        {
            _278 = vs_out.vertBpp == 1;
        }
        else
        {
            _278 = _271;
        }
        if (_278)
        {
            int widthDivisor = 1 << (2 - vs_out.vertBpp);
            vs_out.widthMultiplier = 1.0 / float(widthDivisor);
            vs_out.widthMask = widthDivisor - 1;
            vs_out.indexShift = vs_out.vertBpp + 2;
            vs_out.indexMask = int(pow(16.0, float(vs_out.vertBpp + 1)) - 1.0);
        }
    }
    gl_Position = (_315.camera * t.model) * pos;
    vs_out.viewspaceZ = gl_Position.z;
    if (_331.projectionMode == 1.0)
    {
        float z = clamp(gl_Position.z, 0.0, 65536.0);
        if (z != 0.0)
        {
            vec4 _352 = gl_Position;
            vec2 _354 = _352.xy * (_331.zfar / z);
            gl_Position.x = _354.x;
            gl_Position.y = _354.y;
        }
    }
    if (_331.projectionMode == 2.0)
    {
        vs_out.depthOffset = t.screenOffset.z * _331.zdiffInv;
    }
    else
    {
        if (t.screenOffset.z != 0.0)
        {
            gl_Position.z += t.screenOffset.z;
        }
    }
    vec4 _387 = gl_Position;
    vec2 _389 = _387.xy + t.screenOffset.xy;
    gl_Position.x = _389.x;
    gl_Position.y = _389.y;
    gl_Position = _315.projection * gl_Position;
    vs_out.vertUv = inUv;
    vs_out.depth = gl_Position.z;
}

