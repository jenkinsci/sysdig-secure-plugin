# Sysdig Secure Jenkins Plugin

![Jenkins Plugins](https://img.shields.io/jenkins/plugin/v/sysdig-secure)
![Jenkins Plugin installs](https://img.shields.io/jenkins/plugin/i/sysdig-secure?color=blue)

[Sysdig Secure](https://sysdig.com/products/secure/) is a container security platform that brings together Docker image
scanning and run-time protection to identify vulnerabilities, block threats, enforce compliance, and audit activity
across your microservices. The Sysdig Secure Jenkins plugin can be used in a Pipeline job, or added as a build step to a
Freestyle job to automate the process of running an image analysis, evaluating custom policies against images, and
performing security scans.

# Getting Started

## Pre-requisites

Requires a
valid [Sysdig Secure API token](https://docs.sysdig.com/en/find-the-super-admin-credentials-and-api-token.html#al_UUID-be84a2f1-b996-c30c-b5d8-5b8e4663146a_UUID-87bc65c6-ef79-6225-3910-39f619617a2c)

Jenkins workers **need to have access to the image storage**, either to the local storage where the image was created or
to the registry where it is stored.

## Installation

The Sysdig Secure plugin is published as a Jenkins plugin and is available for installation on any Jenkins server using
the *Plugin Manager* in the web UI through the *Manage Jenkins > Manage Plugins* view, available to administrators of a
Jenkins environment.

See https://www.jenkins.io/doc/book/managing/plugins/

## Global configuration

### Required Config

To configure the Sysdig Secure plugin:

1. Complete these steps after installing the plugin.

2. From the main Jenkins menu, select `Manage Jenkins`.

3. Click the `Configure System` link.

   ![Confgure Jenkins](https://wiki.jenkins.io/display/JENKINS/attachments/145359144/143495649.png)
4. Scroll down to the `Sysdig Secure Plugin` section.

5. In the `Sysdig Secure API Credentials` drop-down, click `Add` button to create a new credential containing the Sysdig
   Secure API Token that you can find in Sysdig Secure -> Settings -> User Profile. You only need to fill the password
   field and keep the user blank. Oncre created, select the new credential in the `Sysdig Secure API Credentials`
   drop-down.

![Sysdig Token Configuration](docs/images/SysdigTokenConfiguration.png)

6. Enter the Sysdig Secure API endpoint in the `Sysdig Secure Engine URL` field. For On-Prem installations, this is the
   URL where your Secure API is exposed. For SaaS service:

    * Default region US East (North Virginia): `https://secure.sysdig.com`
    * US West (Oregon): `https://us2.app.sysdig.com`
    * European Union: `https://eu1.app.sysdig.com`
   > Check [SaaS Regions and IP Ranges](https://docs.sysdig.com/en/saas-regions-and-ip-ranges.html) for a complete list of regions

![Sysdig Plugin Configuration](docs/images/SysdigPluginConfig.png)

7. If you are connecting to an On-Prem instance using an invalid TLS certificate, then you need to either configure
   Jenkins to trust the certificate, or uncheck the `Verify SSL` checkbox.

8. Click `Save`.

# Usage

In order to use the plugin you need to include the Sysdig Image Scanning build step either on your freestyle projects or
your pipelines.

## Integrate the Sysdig Secure Plugin with a Freestyle Project

1. Using the Jenkins Docker plugin for this example, you could start by building the image and writing the image name to
   the `sysdig_secure_images` file

    <img src="docs/images/new/FreestyleDockerBuild.png" height="500px" />

2. Open the `Add build step` drop-down menu, and select
   `Sysdig Image Scanning`. This creates a new build step labeled `Sysdig Image Scanning Build Options`.

   ![Add step in Freestyle Job](docs/images/new/FreestyleAddStep.png)
3. Configure the available options, and click `Save`.

   <img src="docs/images/new/FreestyleConfigStep.png" height="600px" />

## Executing the Sysdig plugin inside a pipeline

The following is a simplified example executing the Sysdig plugin as a stage inside a pipeline

```
stages {
    stage('Checkout') {
        steps {
            checkout scm
        }
    }
    stage('Build Image') {
        steps {
            sh "docker build -f Dockerfile -t ${params.DOCKER_REPOSITORY} ."
        }
    }
    stage('Scanning Image') {
        steps {
            sysdigImageScan engineCredentialsId: 'sysdig-secure-api-credentials', imageName: "${params.DOCKER_REPOSITORY}"
        }
    }
}
```

## Execution options

The table below describes the available configuration options.


| Option                                 | Parameter        | Description                                                                                                                                                                                                                                                  | Default |
|----------------------------------------|------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| ------ |
| Image name                             | imageName        | The name of the image to scan. This parameter is now mandatory, and there is no default value (null).                                                                                                                                                        | `null` |
| Fail build on policy check STOP result | bailOnFail       | If the Sysdig Secure policy evaluate returns a fail (STOP), then the Jenkins job should be failed. If this is not selected then a failed policy evaluation will allow the build to continue.                                                                 | `true` |
| Fail build on critical plugin error    | bailOnPluginFail | If selected, and the Sysdig Secure Plugin experiences a critical error, the build will be failed. This is typically used to ensure that a fault with Sysdig Secure (eg. service not available) does not permit a failing image to be promoted to production. | `true` |
| Identifiers of policies to apply       | policiesToApply  | List of policies to apply to the image in addition to those marked as always apply in the sysdig ui                                                                                                                                                          | |
| Sysdig Secure URL                      | engineURL        | Override the Sysdig Secure URL from the global options                                                                                                                                                                                                       | |
| Sysdig Secure API Credentials          | engineCredentialsId | Override the Sysdig Secure API Credentials from the global options                                                                                                                                                                                        | |
| Verify SSL                             | engineVerify     | Override the Verify SSL option from the global options                                                                                                                                                                                                       | |
| CLI Scanner extra parameters           | inlineScanExtraParams  | Override the additional parameters for the Sysdig CLI Scanner execution from the global options                                                                                                                                                        | |
| Scanner Binary Path                    | scannerBinaryPath | Override the default path for the CLI scanner binary from the global options                                                                                                                                                                                | |

### Examples


The following is simple example of executing the Sysdig Secure plugin as a Jenkinsfile step:

```
sysdigImageScan imageName: 'ruby'
```


You can override any of the default parameters:

```
sysdigImageScan bailOnFail: false, bailOnPluginFail: false, engineCredentialsId: 'sysdig-secure-api-credentials', engineURL: 'https://secure.sysdig.com', engineVerify: false, imageName: 'ruby', policiesToApply: 'foo', scannerBinaryPath: '/bin/sysdig-cli-scanner'
```

### Example: enable the debug log for CLI execution

You can use the `inlineScanExtraParams` option to enable the debug logging when executing the CLI, useful for troubleshooting:

```
sysdigImageScan imageName: ' ruby:latest', inlineScanExtraParams: '--console-log --loglevel=debug'
```

## Providing registry credentials

The CLI scanner will try to use locally available credentials, i.e. from the docker daemon configuration, when available. Additionally, it supports registry user and password credentials via `REGISTRY_USER` and `REGISTRY_PASSWORD` environmental variables as described in https://docs.sysdig.com/en/sysdig-secure/cli-scanner-vm-mode/#registry-credentials

You can use the [Credentials Binding Plugin](https://www.jenkins.io/doc/pipeline/steps/credentials-binding/) to map Jenkins credentials to the corresponding variables, for example:

```
withCredentials([usernamePassword(credentialsId: 'my-registry-credentials', passwordVariable: 'REGISTRY_PASSWORD', usernameVariable: 'REGISTRY_USER')]) {
    sysdigImageScan sysdigImageScan imageName: 'myregistry.com/repo/my-private-image:latest'
}
```

In case of dynamic credentials (i.e. a temporary ECR token), you can fetch the token and then bind to the corresponding variables like in the following example:

```
 stages {
        stage('Get ECR Registry Token') {
            steps {
                script {
                    REGISTRY_PASSWORD = sh(script: 'aws ecr get-login-password --region us-east-1', returnStdout: true).trim()
                }
            }
        }
        stage('Sysdig Vulnerability Scanning') {
            steps {
                script {
                    env.REGISTRY_USER = "AWS";
                    env.REGISTRY_PASSWORD = REGISTRY_PASSWORD;
                }
                sysdigImageScan imageName: "123456789012.dkr.ecr.us-east-1.amazonaws.com/myrepo/myprivate-image:latest"
            }
        }
    }

```

# Plugin outputs

Once the scanning and evaluation is complete, you will have the following build artifacts and reports in the workspace

<img src="docs/images/new/AfterExecution.png" height="500px" />

`sysdig_secure_gates.json` Scanning results for the
Sysdig [policy evaluation](https://docs.sysdig.com/en/manage-scanning-policies.html).

`sysdig_secure_security.json` Detected vulnerability data

`sysdig_secure_raw-vulns_report-.json` Raw vulnerability data

Additionally, the plugin offers you an HTML formatted table output that you can directly display from the
interface (`Sysdig Secure Report (FAIL)` in the image above)

<img src="docs/images/new/ScanCompleteResults.png" height="600px" />

# Local development and installation

Use docker to build the sysdig-secure.hpi file:

```sh
docker run -it --rm --name maven-jenkins-builder -v "$(pwd)":/usr/src/app -w /usr/src/app maven:3.6.3-jdk-8 mvn package
```

You can then install the plugin via the Jenkins UI, or by copying it into $JENKINS_HOME/plugins.
