/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */



package com.thomasdiewald.pixelflow.java.dwgl;

import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;


import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;

import jogamp.opengl.es3.GLES3Impl;

public class DwGLSLShader{
  
  public DwPixelFlow context;

  public int type;
  public int HANDLE;
  public String type_str;
  public String path;
  public String[] content;
  
  public HashMap<String, GLSLDefine> glsl_defines = new HashMap<String, GLSLDefine>();
  
  public boolean flag_rebuild = true;

  public DwGLSLShader(DwPixelFlow context, int type, String path){
    this.context = context;
    this.type = type;
    this.path = path;
    
    if(type == GLES30.GL_VERTEX_SHADER && path == null || path.length() == 0){
      this.content = createDefaultVertexShader();
      this.path = "fullscreenquad.vert";
    } else {
      this.content = loadSource(path);
    }
    
    parseDefines();
  }
  
  
  // vertex shader (fullscreenquad) 
  private static String[] createDefaultVertexShader(){
    String[]content = {
           " "
          ,"#version 300 es"
          ,""
          ,"precision mediump float;" // TODO
          ,"precision mediump int;"   // TODO
          ,""                                     
          ,"void main(){"                         
          ,"  int x = ((gl_VertexID<<1) & 2) - 1;"
          ,"  int y = ((gl_VertexID   ) & 2) - 1;"
          ,"  gl_Position = vec4(x,y,0,1);"
          ,"}"
          ," "
        };
    
    for(int i = 0; i < content.length; i++){
      content[i] += DwUtils.NL;
    }
    return content;
  }
  
  
  public GLSLDefine getDefine(String name){
    return glsl_defines.get(name);
  }
  public void setDefine(String name, int value){
    setDefine(name, Integer.toString(value));
  }
  public void setDefine(String name, float value){
    setDefine(name, Float.toString(value));
  }
  public void setDefine(String name, String value){
    GLSLDefine define = glsl_defines.get(name);
    if(define == null){
      System.out.println("DwGLSLShader ERROR: "+type_str+" ("+path+") - GLSLDefine \""+name+"\" does not exist");
      return;
    }
    Log.d("heyibin" ,"DwGLSLShader define:" + define);
    define.setValue(value);
  }

  public static class GLSLDefine{
    private DwGLSLShader parent;
    private String name;
    private String value;
    private int line;
    GLSLDefine(DwGLSLShader parent, int line, String name, String value){
      this.parent = parent;
      this.line = line;
      this.name = name;
      this.value = value;
    }
    public void setValue(String value){
      this.parent.flag_rebuild |= !this.value.equals(value);
      this.value = value;
    }
    public String getValue(){
      return value;
    }
    public void print(){
      System.out.printf("[%2d] #define %s %s\n", line, name, value);
    }
    public String get(){
      return "#define "+name+" "+value;
    }
    @Override
    public String toString(){
      return name;
    }
  }
  
  
  public String[] loadSource(String path){

    ArrayList<String> source = new ArrayList<String>();

    loadSource(0, source, new File(path));
     
    String[] content = new String[source.size()];
    source.toArray(content);

    return content;
  }
  
  public void parseDefines(){
    for(int i = 0; i < content.length; i++){
      String line = content[i].trim();
      if(line.startsWith("#define ")){
        line = line.substring("#define ".length());
        String[] ltoken = line.split("\\s+");
        
        if(ltoken[0].contains("(")){
          // not my #define
        } else {
//          if(ltoken.length >= 2){
            String name = ltoken[0].trim();
            
            int from = name.length();
            int to = line.indexOf("//");
            if( to == -1) to = line.length();
  
            String value = line.substring(from, to).trim();
            GLSLDefine define = new GLSLDefine(this, i, name, value);
//            System.out.print(define.get());
            glsl_defines.put(define.name, define);
//          }
        }
      }
    }
  }
  
  
  public void printDefines(){
    Set<String> keys = glsl_defines.keySet();
    for(String key : keys ){
      GLSLDefine def = glsl_defines.get(key);
      System.out.printf("[%3d] %s\n", def.line, def.get());
    }
  }

  
  public void loadSource(int depth, ArrayList<String> source, File file){
//    System.out.println("parsing file: "+file);
    
    String path = file.getPath().replace("\\", "/");
    String[] lines = context.utils.readASCIIfile(path);
    
    if(depth++ > 5){
      throw new StackOverflowError("recursive #include: "+file);
    }
    
    File file_dir = file.getParentFile();
    
    for(int i = 0; i < lines.length; i++){
      String line = lines[i];
      String line_trim = line.trim();
      if(line_trim.startsWith("#include")){
        String include_file = line_trim.substring("#include".length()).replace("\"", "").trim();
        File file_to_include = new File(file_dir, include_file);
        loadSource(depth, source, file_to_include);
      } else {
        source.add(line + DwUtils.NL);
      }
    }
  }
  
  public void release(){
    GLES30.glDeleteShader(HANDLE); HANDLE = 0;
  }

  public boolean build() {
    if(flag_rebuild || HANDLE == 0){

      // apply defines
      Set<String> keys = glsl_defines.keySet();
      StringBuilder builder = new StringBuilder();
      for(String key : keys ){
        GLSLDefine def = glsl_defines.get(key);
        content[def.line] = def.get() + DwUtils.NL;
//        builder.append(content[def.line]);
      }
      for(int i = 0;i < content.length;i++){
        builder.append(content[i]);
      }

      String source = builder.toString();
      Log.d("heyibin","sourceStr 是：" + source);
      if(HANDLE == 0) HANDLE  = GLES30.glCreateShader(type);

      GLES30.glShaderSource(HANDLE, source);
      GLES30.glCompileShader(HANDLE);

      String s = GLES30.glGetShaderInfoLog(HANDLE);

     Log.d("heyibin","检查shader状态：" + s);
      
      flag_rebuild = false;
      DwGLSLShader.getShaderInfoLog ( HANDLE, type_str+" ("+path+")");
      DwGLError.debug( "DwGLSLShader.build");
      return true;
    } else {
      return false;
    }
  }


  public void printShader(){
    System.out.println("");
    System.out.println(type_str+": "+path);
    for(int i = 0; i < content.length; i++){
      System.out.printf("[%3d]  %s", i, content[i]);
    }
    System.out.println("");
  }

  public void printCompiledShader(){
    getShaderSource(HANDLE);
  }


  public static void getShaderInfoLog( int shader_id, String info) {
    if(shader_id==-1) return;

    IntBuffer log_len = IntBuffer.allocate(1);
    GLES30.glGetShaderiv(shader_id, GLES30.GL_INFO_LOG_LENGTH, log_len);

    ByteBuffer buffer = ByteBuffer.allocate(log_len.get(0));
    GLES30.glGetShaderInfoLog(shader_id);

    String log = Charset.forName("US-ASCII").decode(buffer).toString();

    if( log.length() > 1 && log.charAt(0) != 0){
      System.out.println(info);
      System.out.println(log);
    }
  }


  public static void getShaderSource(int shader_id){
    if(shader_id == -1) return;

    IntBuffer log_len = IntBuffer.allocate(1);
    GLES30.glGetShaderiv(shader_id, GLES30.GL_SHADER_SOURCE_LENGTH, log_len);

    ByteBuffer buffer = ByteBuffer.allocate(log_len.get(0));
    GLES30.glGetShaderSource(shader_id);

    String log = Charset.forName("US-ASCII").decode(buffer).toString();

    if(log.length() > 1 && log.charAt(0) != 0){
      System.out.println(log);
    }
  }
  




}
