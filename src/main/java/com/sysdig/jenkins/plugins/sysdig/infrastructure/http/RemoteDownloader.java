package com.sysdig.jenkins.plugins.sysdig.infrastructure.http;

import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;
import com.sysdig.jenkins.plugins.sysdig.infrastructure.jenkins.RunContext;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.FilePath;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

public class RemoteDownloader {
    private final RunContext runContext;
    protected final SysdigLogger logger;
    private final EnvVars envVars;

    public RemoteDownloader(@NonNull RunContext runContext) {
        this.runContext = runContext;
        this.logger = runContext.getLogger();
        this.envVars = runContext.getEnvVars();
    }

    public FilePath downloadExecutable(URL url, String fileName) throws IOException, InterruptedException {
        FilePath executableFile = downloadFile(url, fileName);

        // Make the file executable
        executableFile.chmod(0755);
        logger.logInfo("Permissions set: " + executableFile.getRemote());

        return executableFile;
    }

    public FilePath downloadFile(URL url, String fileName) throws IOException, InterruptedException {
        // Create the destination directory in the workspace
        FilePath binFolder = runContext.getPathFromWorkspace().child("bin");
        binFolder.mkdirs();

        FilePath outputFile = binFolder.child(fileName);

        // Obtain the proxy configuration
        Proxy proxy = getHttpProxyFromEnvVars(envVars);

        logger.logInfo(String.format("Downloading %s to %s", url, fileName));
        boolean hostMustBeProxied = proxy != Proxy.NO_PROXY && !isThereAProxyExceptionForHost(url.getHost());
        if (hostMustBeProxied) {
            // Use proxy for the connection
            logger.logInfo("Downloading with proxy: " + proxy.address());
            URLConnection connection = url.openConnection(proxy);
            outputFile.copyFrom(connection.getInputStream());
        } else {
            // Download without proxy
            logger.logInfo("Downloading without proxy");
            outputFile.copyFrom(url);
        }

        return outputFile;
    }

    private static Stream<String> getURLsThatMustNotBeProxiedFromEnvVars(@NonNull EnvVars envVars) {
        var envVarStream = envVars.entrySet().stream();
        var firstNoProxyEnvVar = envVarStream
                .filter(entry -> entry.getKey().equalsIgnoreCase("no_proxy"))
                .findFirst();
        var firstNoProxyEnvVarValue = firstNoProxyEnvVar.map(Map.Entry::getValue);
        var allURLsInNoProxy = firstNoProxyEnvVarValue.stream().flatMap(value -> Arrays.stream(value.split(",")));
        var allURLsTrimmed = allURLsInNoProxy.map(String::trim);
        return allURLsTrimmed;
    }

    private static Proxy getHttpProxyFromEnvVars(@NonNull EnvVars envVars) throws MalformedURLException {
        var possibleProxyAddress = envVars.entrySet().stream()
                .filter(ks -> ks.getKey().equalsIgnoreCase("https_proxy")
                        || ks.getKey().equalsIgnoreCase("http_proxy"))
                .findFirst()
                .map(Map.Entry::getValue);

        if (possibleProxyAddress.isEmpty()) {
            return Proxy.NO_PROXY;
        }

        String proxyAddress = possibleProxyAddress.get();
        if (!proxyAddress.startsWith("http://") && !proxyAddress.startsWith("https://")) {
            proxyAddress = "http://" + proxyAddress;
        }
        URL proxyURL = new URL(proxyAddress);
        int port = proxyURL.getPort() != -1 ? proxyURL.getPort() : 80;
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyURL.getHost(), port));
    }

    private boolean isThereAProxyExceptionForHost(String host) {
        return getURLsThatMustNotBeProxiedFromEnvVars(envVars).anyMatch(host::contains);
    }
}
