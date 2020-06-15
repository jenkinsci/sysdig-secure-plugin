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

import com.google.common.base.Splitter;
import java.util.regex.Pattern;

public class Util {

  // TODO This is probably the slowest way of formatting strings, should do for now but please figure out a better way
  public static final Splitter IMAGE_LIST_SPLITTER = Splitter.on(Pattern.compile("\\s+")).trimResults().omitEmptyStrings();

  public enum GATE_ACTION {STOP, WARN, GO, PASS, FAIL}

  public enum GATE_SUMMARY_COLUMN {Repo_Tag, Stop_Actions, Warn_Actions, Go_Actions, Final_Action}
}
