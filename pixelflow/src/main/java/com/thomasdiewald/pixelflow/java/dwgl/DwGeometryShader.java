package com.thomasdiewald.pixelflow.java.dwgl;


import com.thomasdiewald.pixelflow.java.utils.DwUtils;

import javax.media.opengl.GL4;

import jogamp.opengl.es3.GLES3Impl;
import processing.core.PApplet;
import processing.opengl.PGLES;
import processing.opengl.PShader;

public class DwGeometryShader extends PShader {
  
  public int glGeometry;
  public String geometrySource;
  public String[] src_geom;
  public String filename_geom;

  public DwGeometryShader(PApplet papplet, String filename_vert, String filename_geom, String filename_frag) {
    super(papplet, filename_vert, filename_frag);
    
    this.filename_geom = filename_geom;
    
    this.src_geom = papplet.loadStrings(filename_geom);
    for(int i = 0; i < src_geom.length; i++){
      src_geom[i] += DwUtils.NL;
    }
  }
  
  
  public DwGeometryShader(PApplet papplet, String[] src_vert, String[] src_geom, String[] src_frag) {
    super(papplet, src_vert, src_frag);
    this.src_geom = src_geom;
    
    for(int i = 0; i < src_geom.length; i++){
      src_geom[i] += DwUtils.NL;
    }
  }

  
  @Override
  protected void setup(){
    PGLES pjogl = (PGLES) pgl;
//    GLES3Impl gl = pjogl.gl.getGL3();
    
//    glGeometry = GL4.glCreateShader(GL4.GL_GEOMETRY_SHADER);
//    GL4.glShaderSource(glGeometry, src_geom.length, src_geom, (int[]) null, 0);
//    GL4.glCompileShader(glGeometry);

    DwGLSLShader.getShaderInfoLog( glGeometry, GL4.GL_GEOMETRY_SHADER+" ("+filename_geom+")");

    pgl.attachShader(glProgram, glGeometry);
  }
}