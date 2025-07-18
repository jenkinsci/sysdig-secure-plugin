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
package com.sysdig.jenkins.plugins.sysdig.infrastructure.log;

import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;

public class NopLogger implements SysdigLogger {
    @Override
    public void logDebug(String msg) {}

    @Override
    public void logDebug(String msg, Throwable t) {}

    @Override
    public void logInfo(String msg) {}

    @Override
    public void logInfo(String msg, Throwable t) {}

    @Override
    public void logWarn(String msg) {}

    @Override
    public void logWarn(String msg, Throwable t) {}

    @Override
    public void logError(String msg) {}

    @Override
    public void logError(String msg, Throwable t) {}
}
