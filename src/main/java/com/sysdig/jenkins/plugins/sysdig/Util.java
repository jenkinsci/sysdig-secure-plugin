/*
Copyright (C) 2016-2020 Sysdig

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.sysdig.jenkins.plugins.sysdig;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

public class Util {

  public enum GATE_ACTION {PASS, FAIL}
  public enum GATE_SUMMARY_COLUMN {Repo_Tag, Stop_Actions, Warn_Actions, Go_Actions, Final_Action}

  public static boolean isValidLocalExistingFile(String path) {
    try {
      Paths.get(path);
    } catch (InvalidPathException | NullPointerException ex) {
      return false;
    }
    File f = new File(path);
    if (!path.startsWith("/") || !f.isFile()){
      return false;
    }
    return true;
  }
}
