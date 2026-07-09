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

import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.application.vm.ImageScanningConfig;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Resolves which version of the Sysdig CLI scanner should be used, honoring the
 * custom version configured by the user and falling back to a pinned version.
 */
public class ScannerVersionResolver {
    private static final String FIXED_SCANNED_VERSION = "1.27.2"; // newest-version-marker

    private final ImageScanningConfig config;

    public ScannerVersionResolver(@NonNull ImageScanningConfig config) {
        this.config = config;
    }

    public String resolve() {
        if (Strings.isNullOrEmpty(config.getCliVersionToApply())) {
            return pinnedVersion();
        }

        if (!config.getCliVersionToApply().equals("custom")) {
            return pinnedVersion();
        }

        if (config.getCustomCliVersion().isEmpty()) {
            return pinnedVersion();
        }

        return config.getCustomCliVersion();
    }

    public String pinnedVersion() {
        return FIXED_SCANNED_VERSION;
    }
}
