package com.sysdig.jenkins.plugins.sysdig.infrastructure.json;

import com.google.gson.*;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.Package;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.Vulnerability;
import com.sysdig.jenkins.plugins.sysdig.domain.vm.scanresult.diff.ScanResultDiff;
import java.lang.reflect.Type;

public class ScanResultDiffSerializer implements JsonSerializer<ScanResultDiff> {
    @Override
    public JsonElement serialize(ScanResultDiff src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();

        JsonArray added = new JsonArray();
        for (Vulnerability vuln : src.getVulnerabilitiesAdded()) {
            added.add(serializeVulnerability(vuln));
        }
        jsonObject.add("vulnerabilitiesAdded", added);

        JsonArray fixed = new JsonArray();
        for (Vulnerability vuln : src.getVulnerabilitiesFixed()) {
            fixed.add(serializeVulnerability(vuln));
        }
        jsonObject.add("vulnerabilitiesFixed", fixed);

        return jsonObject;
    }

    private JsonObject serializeVulnerability(Vulnerability vuln) {
        JsonObject vulnObject = new JsonObject();
        vulnObject.addProperty("cve", vuln.cve());
        vulnObject.addProperty("severity", vuln.severity().toString());
        vuln.fixVersion().ifPresent(fix -> vulnObject.addProperty("fixVersion", fix));
        vulnObject.addProperty("packageName", getPackageName(vuln));
        vulnObject.addProperty("packageVersion", getPackageVersion(vuln));
        vulnObject.addProperty("packageType", getPackageType(vuln));
        vulnObject.addProperty("exploitable", vuln.exploitable());
        return vulnObject;
    }

    private String getPackageName(Vulnerability vuln) {
        return vuln.foundInPackages().stream().map(Package::name).findFirst().orElse("N/A");
    }

    private String getPackageVersion(Vulnerability vuln) {
        return vuln.foundInPackages().stream().map(Package::version).findFirst().orElse("N/A");
    }

    private String getPackageType(Vulnerability vuln) {
        return vuln.foundInPackages().stream()
                .map(p -> p.type().toString())
                .findFirst()
                .orElse("N/A");
    }
}
