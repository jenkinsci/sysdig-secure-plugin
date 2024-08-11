package com.sysdig.jenkins.plugins.sysdig;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Scanner;

import hudson.model.Run;
import jenkins.model.RunAction2;

public class CLIDownloadAction implements RunAction2 {
    private String name;
    private String os;
    private String arch;
    private String version;
    private transient Run run;
    private String url;
    private String basePath = "/var/jenkins_home";

    private Path apath;

    public String cliExecPath() {

        return this.apath.toAbsolutePath().toString();
    }

    private void makeCLIDownladURL() {
    Scanner s = null;    
    try {
            @SuppressWarnings("deprecation")
            URL uarl = new URL("https", "download.sysdig.com", "/scanning/sysdig-cli-scanner/latest_version.txt");
            s = new Scanner(uarl.openStream());
            if (this.version.isEmpty() || this.version.equalsIgnoreCase("latest")){
                this.version = s.nextLine();
            }
            this.url = String.format(
                    "https://download.sysdig.com/scanning/bin/sysdig-cli-scanner/%s/%s/%s/sysdig-cli-scanner",
                    this.version,
                    this.os.toLowerCase(), this.arch.toLowerCase());
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        if (s != null) {
         s.close();
         }
         }
    }

     private void downloadUsingNIO( String file) throws IOException {
        URL url = new URL(this.url);
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(file);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
        rbc.close();
    }

    private void downloaderStream(String p) throws Exception {
        URL url = new URL(this.url);
        BufferedInputStream bis = new BufferedInputStream(url.openStream());
        FileOutputStream fis = new FileOutputStream(p);
        byte[] buffer = new byte[1024];
        int count = 0;
        while ((count = bis.read(buffer, 0, 1024)) != -1) {
            fis.write(buffer, 0, count);
        }
        fis.close();
        bis.close();
    }
    
    private void downloadCLI() throws Exception {
        if ("".equals(this.url)) {
            throw new Exception("empty url");
        }
        String tmpPath = String.format("%s/sysdig-cli-scanner.%s", this.basePath, this.version);
        try {
            downloaderStream(tmpPath);
        } catch (Exception e) {
            
            downloadUsingNIO(tmpPath);
        }
        
    }

    public CLIDownloadAction(String name, String basePath,String version) throws Exception {
        this.name = name;
        this.basePath = basePath;
        this.os = System.getProperty("os.name");
        this.version = version;
        this.arch = System.getProperty("os.arch");
        makeCLIDownladURL();
        downloadCLI();
        String tmpPath = String.format("%s/sysdig-cli-scanner.%s", this.basePath, this.version);
        this.apath = Paths.get(tmpPath);

        Files.setPosixFilePermissions(this.apath, PosixFilePermissions.fromString("rwx------"));

    }

    @Override
    public void onAttached(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        this.run = run;
    }

    public Run getRun() {
        return run;
    }

    @Override
    public String getIconFileName() {
        return "document.png";
    }

    public String getOs() {
        return os;
    }

    public String getArch() {
        return arch;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String getDisplayName() {
        return "Greeting";
    }

    @Override
    public String getUrlName() {
        return "greeting";
    }
}