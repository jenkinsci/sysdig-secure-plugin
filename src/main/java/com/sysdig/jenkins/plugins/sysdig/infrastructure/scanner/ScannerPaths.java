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
package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner;

import hudson.FilePath;
import java.io.Serializable;

/** Holds the folder hierarchy used by a single scanner execution. */
public class ScannerPaths implements Serializable {
    private static final String SCANNER_EXEC_FOLDER_BASE_PATH_PATTERN = "sysdig-secure-scan-%d";
    private final FilePath baseFolder;
    private final FilePath databaseFolder;
    private final FilePath cacheFolder;

    public ScannerPaths(final FilePath basePath) {
        this.baseFolder =
                basePath.child(String.format(SCANNER_EXEC_FOLDER_BASE_PATH_PATTERN, System.currentTimeMillis()));
        this.databaseFolder = baseFolder.child("db");
        this.cacheFolder = baseFolder.child("cache");
    }

    public FilePath getBaseFolder() {
        return this.baseFolder;
    }

    public FilePath getDatabaseFolder() {
        return this.databaseFolder;
    }

    public FilePath getCacheFolder() {
        return this.cacheFolder;
    }

    public void create() throws Exception {
        this.baseFolder.mkdirs();
        this.databaseFolder.mkdirs();
        this.cacheFolder.mkdirs();
    }
}
