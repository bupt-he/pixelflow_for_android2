/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 300 es

out vec4 glFragColor;

uniform sampler2D	tex;
uniform vec2 wh_rcp; 
uniform vec2 mad;

void main(){
  glFragColor = texture(tex, gl_FragCoord.xy * wh_rcp) * mad.x + mad.y;
}





