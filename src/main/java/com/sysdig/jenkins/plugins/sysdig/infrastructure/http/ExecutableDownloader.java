/*
Copyright (C) 2016-2024 Sysdig

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
package com.sysdig.jenkins.plugins.sysdig.infrastructure.http;

import hudson.FilePath;
import java.io.IOException;
import java.net.URL;

/** Downloads a remote executable into the workspace and returns its path. */
public interface ExecutableDownloader {
    FilePath downloadExecutable(URL url, String fileName) throws IOException, InterruptedException;
}
