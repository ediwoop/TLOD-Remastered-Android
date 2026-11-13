#version 320 es
precision mediump float;
precision highp int;

layout(std140) uniform scissor
{
    highp float scissorX;
    highp float scissorY;
    highp float scissorW;
    highp float scissorH;
} _18;

layout(std140) uniform projectionInfo
{
    highp float znear;
    highp float zfar;
    highp float zdiffInv;
    highp float projectionMode;
} _67;

uniform int ctmdFlags;
uniform int tmdTranslucency;
uniform highp vec2 uvOffset;
layout(location = 6, binding = 3) uniform highp usampler2D tex15;
layout(location = 5, binding = 2) uniform highp sampler2D tex24;
uniform highp float discardTranslucency;
uniform highp vec3 recolour;

layout(location = 0) in GS_OUT
{
    highp vec2 vertUv;
    flat highp vec2 vertTpage;
    flat highp vec2 vertClut;
    flat int vertBpp;
    highp vec4 vertColour;
    flat int vertFlags;
    flat int translucency;
    flat highp float widthMultiplier;
    flat int widthMask;
    flat int indexShift;
    flat int indexMask;
    highp float depth;
    highp float depthOffset;
} _79;

layout(location = 0) out highp vec4 outColour;

void main()
{
    bool _24 = gl_FragCoord.x < _18.scissorX;
    bool _37;
    if (!_24)
    {
        _37 = gl_FragCoord.x >= (_18.scissorX + _18.scissorW);
    }
    else
    {
        _37 = _24;
    }
    bool _48;
    if (!_37)
    {
        _48 = gl_FragCoord.y < _18.scissorY;
    }
    else
    {
        _48 = _37;
    }
    bool _61;
    if (!_48)
    {
        _61 = gl_FragCoord.y >= (_18.scissorY + _18.scissorH);
    }
    else
    {
        _61 = _48;
    }
    if (_61)
    {
        discard;
    }
    if (_67.projectionMode == 2.0)
    {
        gl_FragDepth = ((_79.depth - _67.znear) * _67.zdiffInv) + _79.depthOffset;
    }
    else
    {
        gl_FragDepth = gl_FragCoord.z;
    }
    bool ctmd = (ctmdFlags & 32) != 0;
    bool uniformLit = (ctmdFlags & 16) != 0;
    bool _117 = (_79.vertFlags & 8) != 0;
    bool _124;
    if (!_117)
    {
        _124 = (ctmdFlags & 2) != 0;
    }
    else
    {
        _124 = _117;
    }
    bool translucent = _124;
    bool textured = (_79.vertFlags & 2) != 0;
    outColour = _79.vertColour;
    int translucencyMode = _79.translucency + 1;
    bool _148;
    if (translucent)
    {
        _148 = (!textured) || uniformLit;
    }
    else
    {
        _148 = translucent;
    }
    if (_148)
    {
        translucencyMode = tmdTranslucency + 1;
    }
    if (textured)
    {
        bool _159 = _79.vertBpp == 0;
        bool _166;
        if (!_159)
        {
            _166 = _79.vertBpp == 1;
        }
        else
        {
            _166 = _159;
        }
        highp vec4 texColour;
        if (_166)
        {
            ivec2 uv = ivec2(int(_79.vertTpage.x + ((_79.vertUv.x + uvOffset.x) * _79.widthMultiplier)), int((_79.vertTpage.y + _79.vertUv.y) + uvOffset.y));
            ivec4 indexVec = ivec4(texelFetch(tex15, uv, 0));
            int p = (indexVec.x >> ((int(_79.vertTpage.x + _79.vertUv.x) & _79.widthMask) << _79.indexShift)) & _79.indexMask;
            uint pixel = texelFetch(tex15, ivec2(int(_79.vertClut.x + float(p)), int(_79.vertClut.y)), 0).x;
            texColour.w = float((pixel >> uint(15)) & 31u) / 31.0;
            texColour.z = float((pixel >> uint(10)) & 31u) / 31.0;
            texColour.y = float((pixel >> uint(5)) & 31u) / 31.0;
            texColour.x = float(pixel & 31u) / 31.0;
        }
        else
        {
            if (_79.vertBpp == 2)
            {
                ivec2 uv_1 = ivec2(int(_79.vertTpage.x + (_79.vertUv.x + uvOffset.x)), int((_79.vertTpage.y + _79.vertUv.y) + uvOffset.y));
                uint pixel_1 = texelFetch(tex15, uv_1, 0).x;
                texColour.w = float((pixel_1 >> uint(15)) & 31u) / 31.0;
                texColour.z = float((pixel_1 >> uint(10)) & 31u) / 31.0;
                texColour.y = float((pixel_1 >> uint(5)) & 31u) / 31.0;
                texColour.x = float(pixel_1 & 31u) / 31.0;
            }
            else
            {
                texColour = texture(tex24, _79.vertUv + uvOffset);
            }
        }
        bool _348 = texColour.w == 0.0;
        bool _354;
        if (_348)
        {
            _354 = texColour.x == 0.0;
        }
        else
        {
            _354 = _348;
        }
        bool _360;
        if (_354)
        {
            _360 = texColour.y == 0.0;
        }
        else
        {
            _360 = _354;
        }
        bool _366;
        if (_360)
        {
            _366 = texColour.z == 0.0;
        }
        else
        {
            _366 = _360;
        }
        if (_366)
        {
            discard;
        }
        bool _375 = (discardTranslucency == 1.0) && translucent;
        bool _381;
        if (_375)
        {
            _381 = texColour.w != 0.0;
        }
        else
        {
            _381 = _375;
        }
        bool _399;
        if (!_381)
        {
            bool _386 = discardTranslucency == 2.0;
            bool _398;
            if (_386)
            {
                bool _390 = !translucent;
                bool _397;
                if (!_390)
                {
                    _397 = texColour.w == 0.0;
                }
                else
                {
                    _397 = _390;
                }
                _398 = _397;
            }
            else
            {
                _398 = _386;
            }
            _399 = _398;
        }
        else
        {
            _399 = _381;
        }
        if (_399)
        {
            discard;
        }
        outColour = clamp(outColour * texColour, vec4(0.0), vec4(1.0));
    }
    else
    {
        bool _413 = (discardTranslucency == 1.0) && translucent;
        bool _422;
        if (!_413)
        {
            _422 = (discardTranslucency == 2.0) && (!translucent);
        }
        else
        {
            _422 = _413;
        }
        if (_422)
        {
            discard;
        }
    }
    highp vec4 _430 = outColour;
    highp vec3 _432 = _430.xyz * recolour;
    outColour.x = _432.x;
    outColour.y = _432.y;
    outColour.z = _432.z;
    if (translucent && (translucencyMode == 1))
    {
        outColour.w = 0.5;
    }
    else
    {
        outColour.w = 1.0;
    }
}

