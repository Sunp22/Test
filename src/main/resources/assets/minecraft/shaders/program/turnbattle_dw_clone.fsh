#version 150

// TurnBattle: the Dark World colour grade (vanilla's color_convolve, the same maths as the world
// tint), for the battle screen's 3D models - but keeping each pixel's alpha, so the models can be
// laid back over the screen instead of filling it.

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;

uniform vec3 Gray;
uniform vec3 RedMatrix;
uniform vec3 GreenMatrix;
uniform vec3 BlueMatrix;
uniform vec3 Offset;
uniform vec3 ColorScale;
uniform float Saturation;

out vec4 fragColor;

void main() {
    vec4 InTexel = texture(DiffuseSampler, texCoord);
    if (InTexel.a <= 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    float RedValue = dot(InTexel.rgb, RedMatrix);
    float GreenValue = dot(InTexel.rgb, GreenMatrix);
    float BlueValue = dot(InTexel.rgb, BlueMatrix);
    vec3 OutColor = vec3(RedValue, GreenValue, BlueValue);

    OutColor = (OutColor * ColorScale) + Offset;

    float Luma = dot(OutColor, Gray);
    vec3 Chroma = OutColor - Luma;
    OutColor = (Chroma * Saturation) + Luma;

    fragColor = vec4(clamp(OutColor, 0.0, 1.0), InTexel.a);
}
