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

    public String chwd() {

        return this.apath.toAbsolutePath().toString();
    }

    private void makeUrl() {
        try {
            @SuppressWarnings("deprecation")
            URL uarl = new URL("https", "download.sysdig.com", "/scanning/sysdig-cli-scanner/latest_version.txt");
            Scanner s = new Scanner(uarl.openStream());
            if (this.version.isEmpty() || this.version.equalsIgnoreCase("latest")){
                this.version = s.nextLine();
            }
            this.url = String.format(
                    "https://download.sysdig.com/scanning/bin/sysdig-cli-scanner/%s/%s/%s/sysdig-cli-scanner",
                    this.version,
                    this.os.toLowerCase(), this.arch.toLowerCase());
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
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
    
    private void downloader() throws Exception {
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

    public CLIDownloadAction(String name, String basePath,String ver) throws Exception {
        this.name = name;
        this.basePath = basePath;
        this.os = System.getProperty("os.name");
        this.version = ver;
        this.arch = System.getProperty("os.arch");
        makeUrl();
        downloader();
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

// import java.io.BufferedInputStream;
// import java.io.File;
// import java.io.FileOutputStream;
// import java.io.IOException;
// import java.net.URL;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.nio.file.attribute.PosixFilePermissions;
// import java.util.Scanner;

// import com.sun.jndi.url.ldaps.ldapsURLContextFactory;
// import com.sysdig.external.CLIDownloader;

// import hudson.model.Run;
// import jenkins.model.RunAction2;

// public class HelloWorldAction implements RunAction2 {
// private String name;
// private String os;
// private String arch;
// private transient Run run;
// private String url;
// private String apath;
// private File file;

// public String chwd() {

// return this.file.getAbsolutePath();
// }

// private void makeUrl() {
// try {
// @SuppressWarnings("deprecation")
// URL uarl = new URL("https", "download.sysdig.com",
// "/scanning/sysdig-cli-scanner/latest_version.txt");
// Scanner s = new Scanner(uarl.openStream());
// String ver = s.nextLine();
// this.url = String.format(
// "https://download.sysdig.com/scanning/bin/sysdig-cli-scanner/%s/%s/%s/sysdig-cli-scanner",
// ver,
// this.os.toLowerCase(), this.arch.toLowerCase());
// } catch (Exception e) {
// e.printStackTrace();
// }
// }

// public HelloWorldAction(String name) {
// CLIDownloader dl = new CLIDownloader("/usr/local/bin/");

// this.name = name;

// this.os = dl.getOs();

// this.arch = dl.getArch();
// makeUrl();
// try {
// file = dl.Download();

// } catch (UnsupportedOperationException | IOException | InterruptedException
// e) {
// // TODO Auto-generated catch block
// e.printStackTrace();
// }

// }

// @Override
// public void onAttached(Run<?, ?> run) {
// this.run = run;
// }

// @Override
// public void onLoad(Run<?, ?> run) {
// this.run = run;
// }

// public Run getRun() {
// return run;
// }

// @Override
// public String getIconFileName() {
// return "document.png";
// }

// public String getOs() {
// return os;
// }

// public String getArch() {
// return arch;
// }

// public String getName() {
// return name;
// }

// public String getUrl() {
// return url;
// }

// @Override
// public String getDisplayName() {
// return "Greeting";
// }

// @Override
// public String getUrlName() {
// return "greeting";
// }
// }