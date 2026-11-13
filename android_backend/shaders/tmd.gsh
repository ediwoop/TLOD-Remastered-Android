#version 320 es
precision highp float;
precision highp int;
layout(triangles_adjacency) in;
layout(max_vertices = 3, triangle_strip) out;

layout(std140) uniform projectionInfo
{
    float znear;
    float zfar;
    float zdiffInv;
    float projectionMode;
} _17;

uniform int usePs1Depth;

layout(location = 0) in VS_OUT
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
} vs_out[6];

layout(location = 0) out GS_OUT
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
    float depth;
    float depthOffset;
} gs_out;


void main()
{
    if (_17.zfar >= (vs_out[0].viewspaceZ * 2.0))
    {
        return;
    }
    if (_17.zfar >= (vs_out[2].viewspaceZ * 2.0))
    {
        return;
    }
    if (_17.zfar >= (vs_out[4].viewspaceZ * 2.0))
    {
        return;
    }
    float avgDepth = (gl_in[0].gl_Position.z + gl_in[2].gl_Position.z) + gl_in[4].gl_Position.z;
    int vertexCount = 3;
    if ((vs_out[0].vertFlags & 16) != 0)
    {
        if (_17.zfar >= (vs_out[1].viewspaceZ * 2.0))
        {
            return;
        }
        avgDepth += gl_in[1].gl_Position.z;
        vertexCount++;
    }
    avgDepth /= float(vertexCount);
    // Keep EmitVertex in main: some mobile translators miss clip conversion
    // when the built-in is reached through a user helper.
    for (int vertex = 0; vertex < 3; vertex++)
    {
        int i = vertex * 2;
        gs_out.vertUv = vs_out[i].vertUv;
        gs_out.vertTpage = vs_out[i].vertTpage;
        gs_out.vertClut = vs_out[i].vertClut;
        gs_out.vertBpp = vs_out[i].vertBpp;
        gs_out.vertColour = vs_out[i].vertColour;
        gs_out.vertFlags = vs_out[i].vertFlags;
        gs_out.translucency = vs_out[i].translucency;
        gs_out.widthMultiplier = vs_out[i].widthMultiplier;
        gs_out.widthMask = vs_out[i].widthMask;
        gs_out.indexShift = vs_out[i].indexShift;
        gs_out.indexMask = vs_out[i].indexMask;
        gs_out.depth = vs_out[i].depth;
        gs_out.depthOffset = vs_out[i].depthOffset;
        gl_Position = gl_in[i].gl_Position;
        if (usePs1Depth != 0)
        {
            gl_Position.z = avgDepth;
        }
        EmitVertex();
    }
    EndPrimitive();
}

