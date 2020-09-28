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
package com.sysdig.jenkins.plugins.sysdig.scanner;

import com.sysdig.jenkins.plugins.sysdig.BuildConfig;
import com.sysdig.jenkins.plugins.sysdig.client.*;
import hudson.AbortException;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.remoting.*;
import org.antlr.v4.runtime.misc.NotNull;

import java.util.*;

public class InlineScanner extends Scanner {

  public InlineScanner(Launcher launcher, TaskListener listener, BuildConfig config) throws AbortException {
    super(launcher, listener, config);
  }

  @Override
  public @NotNull
  ArrayList<ImageScanningResult> scanImages(Map<String, String> imagesAndDockerfiles) throws AbortException {
    if (imagesAndDockerfiles == null) {
      return new ArrayList<>();
    }

    ArrayList<ImageScanningResult> resultList = new ArrayList<>();
    try {
      VirtualChannel channel = launcher.getChannel();
      if (channel == null) {
        throw new AbortException("There's no channel to communicate with the worker");
      }

      for (Map.Entry<String, String> entry : imagesAndDockerfiles.entrySet()) {
        InlineScannerRemoteExecutor task = new InlineScannerRemoteExecutor(entry.getKey(), entry.getValue(), logger, config);

        ImageScanningResult result = channel.call(task);
        resultList.add(result);
      }
    } catch (Exception e) {
      throw new AbortException(e.toString());
    }

    return resultList;
  }

}
