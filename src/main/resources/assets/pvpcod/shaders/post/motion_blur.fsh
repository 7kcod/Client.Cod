#version 330

uniform sampler2D InSampler;
uniform sampler2D PrevSampler;

in vec2 texCoord;

layout(std140) uniform MotionBlurConfig {
    float BlurAmount;
};

out vec4 fragColor;

void main(){
    vec4 now = texture(InSampler, texCoord);
    vec4 before = texture(PrevSampler, texCoord);
    fragColor = vec4(mix(now.rgb, before.rgb, BlurAmount), 1.0);
}
