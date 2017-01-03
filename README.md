# Export Mobile Fabric App Services

This repository includes a [Jenkinsfile](https://jenkins.io/doc/book/pipeline/jenkinsfile/) and auxiliary [Groovy](http://www.groovy-lang.org/) scripts to implement a [Jenkins pipeline job](https://jenkins.io/solutions/pipeline/) that pushes app services from the [Kony Mobile Fabric](http://www.kony.com/products/mobilefabric) Console into a Git server. This delivers the following benefits for your project:

* It allows you to back up your back-end integrations.
* It allows you to track changes to those integrations.
* It allows you to actually keep specific snapshots/versions of your integrations frozen in specific branches of your repo.

## Context

Kony Mobile Fabric offers a feature to manually export your app configurations into zip files. This is handy of course if you just want do a one-off export of one app from one instance and import it into another. However this is not ideal when it comes to keeping track of the changes made to several apps across time, and backing up the different versions produced.

This job automates this process for you, so your backend developers can focus on building the integrations and not worry about versioning and backup.

This script will do the following tasks:

1. Export your app's services from Mobile Fabric.

    java -jar mfcli.jar export

2. Unzip the contents of the export.

3. Determine whether there are any changes.

4. Prettify the *Meta.json* files which define the configuration parameters for each service.

5. Configure local git user to push changes in your name.

    git config --local user.name [name]
    git config --local user.email [email]

6. Switch to branch of your choosing.

    git checkout ${gitBranch}

7. Commit the changes with a message of your choosing and push to your chosen git repository.

    git commit -m [COMMIT_MESSAGE]

## Pre-requisites

* An existing configuration of your app services in Mobile Fabric. This job won't create them for you.
* An existing git repository. This job won't create it for you.
* Mobile Fabric credentials.
* Git credentials.

## How to Use

In order **to use this job you don't have to do anything with this repository**. You just have to create a Jenkins job that uses the scripts here to back up and version the Mobile Fabric services for your app.

1. Create a Jenkins Pipeline job that pulls from this repo.

2. Add an input parameter called *MF_CLI_LOCATION* of type String and set it to the download url of the Kony mfcli.jar.

3. Add an input parameter called *EXPORT_REPO_URL* of type String.

4. Add an input parameter called *GIT_CREDENTIALS* of type Credentials.

5. Add an input parameter called *MOBILE_FABRIC_APP_ID* of type String.

6. Add an input parameter called *MF_CREDENTIALS* of type Credentials.

7. Add an input parameter called *COMMIT_AUTHOR* of type String.

8. Add an input parameter called *AUTHOR_EMAIL* of type String.

9. Add an input parameter called *COMMIT_MESSAGE* of type String.

Then, as you work on your services, every time you've made and tested a change to your Mobile Fabric services and wish to save your progress, go to your Jenkins Console and run this job.

## TO-DO:
1. Create the requested branch to push into if it doesn't already exist.
2. Compare zip exports via MD5 or another mechanism that disregards metadata.
  1. Right now it always executes because two exports of the exact same MF services have different metadata.
  2. May also try to compare unziped exports.
3. Take an optional input parameter to create a git tag for the changes pushed.
4. Package mfcli.jar into a Jenkins plugin.
