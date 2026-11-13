#version 300 es
precision highp float;
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

uniform int tmdTranslucency;
uniform highp vec2 uvOffset;
uniform highp usampler2D tex15;
uniform highp sampler2D tex24;
uniform highp float discardTranslucency;
uniform highp vec3 recolour;

in highp vec2 v_vertUv;
flat in highp vec2 v_vertTpage;
flat in highp vec2 v_vertClut;
flat in highp int v_vertBpp;
in highp vec4 v_vertColour;
flat in highp int v_vertFlags;
flat in highp int v_translucency;
flat in highp float v_widthMultiplier;
flat in highp int v_widthMask;
flat in highp int v_indexShift;
flat in highp int v_indexMask;
in highp float v_depth;
in highp float v_depthOffset;

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
        gl_FragDepth = ((v_depth - _67.znear) * _67.zdiffInv) + v_depthOffset;
    }
    else
    {
        gl_FragDepth = gl_FragCoord.z;
    }
    bool translucent = (v_vertFlags & 8) != 0;
    bool textured = (v_vertFlags & 2) != 0;
    outColour = v_vertColour;
    int translucencyMode = v_translucency + 1;
    if (translucent && (!textured))
    {
        translucencyMode = tmdTranslucency + 1;
    }
    if (textured)
    {
        bool _137 = v_vertBpp == 0;
        bool _144;
        if (!_137)
        {
            _144 = v_vertBpp == 1;
        }
        else
        {
            _144 = _137;
        }
        highp vec4 texColour;
        if (_144)
        {
            ivec2 uv = ivec2(int(v_vertTpage.x + ((v_vertUv.x + uvOffset.x) * v_widthMultiplier)), int((v_vertTpage.y + v_vertUv.y) + uvOffset.y));
            ivec4 indexVec = ivec4(texelFetch(tex15, uv, 0));
            int p = (indexVec.x >> ((int(v_vertTpage.x + v_vertUv.x) & v_widthMask) << v_indexShift)) & v_indexMask;
            uint pixel = texelFetch(tex15, ivec2(int(v_vertClut.x + float(p)), int(v_vertClut.y)), 0).x;
            texColour.w = float((pixel >> uint(15)) & 31u) / 31.0;
            texColour.z = float((pixel >> uint(10)) & 31u) / 31.0;
            texColour.y = float((pixel >> uint(5)) & 31u) / 31.0;
            texColour.x = float(pixel & 31u) / 31.0;
        }
        else
        {
            if (v_vertBpp == 2)
            {
                ivec2 uv_1 = ivec2(int(v_vertTpage.x + (v_vertUv.x + uvOffset.x)), int((v_vertTpage.y + v_vertUv.y) + uvOffset.y));
                uint pixel_1 = texelFetch(tex15, uv_1, 0).x;
                texColour.w = float((pixel_1 >> uint(15)) & 31u) / 31.0;
                texColour.z = float((pixel_1 >> uint(10)) & 31u) / 31.0;
                texColour.y = float((pixel_1 >> uint(5)) & 31u) / 31.0;
                texColour.x = float(pixel_1 & 31u) / 31.0;
            }
            else
            {
                texColour = texture(tex24, v_vertUv + uvOffset);
            }
        }
        bool _326 = texColour.w == 0.0;
        bool _332;
        if (_326)
        {
            _332 = texColour.x == 0.0;
        }
        else
        {
            _332 = _326;
        }
        bool _338;
        if (_332)
        {
            _338 = texColour.y == 0.0;
        }
        else
        {
            _338 = _332;
        }
        bool _344;
        if (_338)
        {
            _344 = texColour.z == 0.0;
        }
        else
        {
            _344 = _338;
        }
        if (_344)
        {
            discard;
        }
        bool _353 = (discardTranslucency == 1.0) && translucent;
        bool _359;
        if (_353)
        {
            _359 = texColour.w != 0.0;
        }
        else
        {
            _359 = _353;
        }
        bool _377;
        if (!_359)
        {
            bool _364 = discardTranslucency == 2.0;
            bool _376;
            if (_364)
            {
                bool _368 = !translucent;
                bool _375;
                if (!_368)
                {
                    _375 = texColour.w == 0.0;
                }
                else
                {
                    _375 = _368;
                }
                _376 = _375;
            }
            else
            {
                _376 = _364;
            }
            _377 = _376;
        }
        else
        {
            _377 = _359;
        }
        if (_377)
        {
            discard;
        }
        outColour = clamp(outColour * texColour, vec4(0.0), vec4(1.0));
    }
    else
    {
        bool _391 = (discardTranslucency == 1.0) && translucent;
        bool _400;
        if (!_391)
        {
            _400 = (discardTranslucency == 2.0) && (!translucent);
        }
        else
        {
            _400 = _391;
        }
        if (_400)
        {
            discard;
        }
    }
    highp vec4 _408 = outColour;
    highp vec3 _410 = _408.xyz * recolour;
    outColour.x = _410.x;
    outColour.y = _410.y;
    outColour.z = _410.z;
    if (translucent && (translucencyMode == 1))
    {
        outColour.w = 0.5;
    }
    else
    {
        outColour.w = 1.0;
    }
}

