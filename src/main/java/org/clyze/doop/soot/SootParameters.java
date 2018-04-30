package org.clyze.doop.soot;

import org.clyze.doop.util.filter.ClassFilter;

import java.util.ArrayList;
import java.util.List;

public class SootParameters {
    public enum Mode {INPUTS, FULL}
     Mode _mode = null;
     List<String> _inputs = new ArrayList<>();
     List<String> _libraries = new ArrayList<>();
     String _outputDir = null;
     String _main = null;
     boolean _ssa = false;
     boolean _android = false;
     String _androidJars = null;
     boolean _allowPhantom = false;
     public boolean _onlyApplicationClassesFactGen = false;
     ClassFilter applicationClassFilter;
     String appRegex = "**";
     boolean _runFlowdroid = false;
     boolean _noFacts = false;
     boolean _uniqueFacts = false;
     boolean _generateJimple = false;
     boolean _toStdout = false;
     //FIXME: added
     public boolean _moduleMode = false;
     String _moduleName = null;

     public void set_mode(Mode _mode) {
          this._mode = _mode;
     }

     public void set_inputs(List<String> _inputs) {
          this._inputs = _inputs;
     }

     public void set_libraries(List<String> _libraries) {
          this._libraries = _libraries;
     }

     public void set_outputDir(String _outputDir) {
          this._outputDir = _outputDir;
     }

     public void set_main(String _main) {
          this._main = _main;
     }

     public void set_ssa(boolean _ssa) {
          this._ssa = _ssa;
     }

     public void set_android(boolean _android) {
          this._android = _android;
     }

     public void set_androidJars(String _androidJars) {
          this._androidJars = _androidJars;
     }

     public void set_allowPhantom(boolean _allowPhantom) {
          this._allowPhantom = _allowPhantom;
     }

     public void set_onlyApplicationClassesFactGen(boolean _onlyApplicationClassesFactGen) {
          this._onlyApplicationClassesFactGen = _onlyApplicationClassesFactGen;
     }

     public void setApplicationClassFilter(ClassFilter applicationClassFilter) {
          this.applicationClassFilter = applicationClassFilter;
     }

     public void setAppRegex(String appRegex) {
          this.appRegex = appRegex;
     }

     public void set_runFlowdroid(boolean _runFlowdroid) {
          this._runFlowdroid = _runFlowdroid;
     }

     public void set_noFacts(boolean _noFacts) {
          this._noFacts = _noFacts;
     }

     public void set_uniqueFacts(boolean _uniqueFacts) {
          this._uniqueFacts = _uniqueFacts;
     }

     public void set_generateJimple(boolean _generateJimple) {
          this._generateJimple = _generateJimple;
     }

     public void set_toStdout(boolean _toStdout) {
          this._toStdout = _toStdout;
     }

     public void set_moduleMode(boolean _moduleMode) {
          this._moduleMode = _moduleMode;
     }

     public void set_moduleName(String _moduleName) {
          this._moduleName = _moduleName;
     }
}
