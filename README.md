# Sysdig Secure Jenkins Plugin

This plugin allows Sysdig Secure users to submit container images for
analysis, and gate the build based on the policy evaluation of the image.

## Setup

Refer to https://wiki.jenkins.io/display/JENKINS/Anchore+Container+Image+Scanner+Plugin

## Local development and installation

Use docker to build the sysdig-secure.hpi file:

```sh
docker run -it --rm --name maven-jenkins-builder -v "$(pwd)":/usr/src/app -w /usr/src/app maven:3.3-jdk-8 mvn package
```

You can then install the plugin via the Jenkins UI, or by copying it into $JENKINS_HOME/plugins.
