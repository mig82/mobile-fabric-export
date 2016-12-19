node('mobilefabric') {
	def gitProtocol, gitDomain, orgName, gitProject
	def gitCredId = GIT_CREDENTIALS //Credentials parameter
	def exportRepoUrl = EXPORT_REPO_URL //String parameter
	def gitBranch = EXPORT_REPO_BRANCH //'master' //String parameter
	def mfCredId = MF_CREDENTIALS
	def mfAccountId = MOBILE_FABRIC_ACCOUNT_ID
	def mfAppId = MOBILE_FABRIC_APP_ID
	def mfCliLocation = MF_CLI_LOCATION //HTTPS or S3
	def mfcliS3Url = MF_CLI_S3_URL //s3://your-s3-name/some/path/mfcli.jar
	def mfcliHttpsUrl = MF_CLI_HTTPS_URL //https://s3.amazonaws.com/plugins-updatesite-prod/onpremise/mobilefabric/mabilefabricCI/7.1.1.0/mfcli.jar
	
	stage('Validate input parameters'){
		echo exportRepoUrl
		def gitParams = exportRepoUrl.split('/')
		
		//def gitProtocol = 'https:'
		gitProtocol = gitParams[0]
		echo "gitProtocol=[${gitProtocol}]"
		
		//gitParams[1] is empty string between // after https:

		//def gitDomain = 'engie-src.ci.konycloud.com' //or the equivalent of 'github.com'
		gitDomain = gitParams[2]
		//def orgName = 'Kony'
		orgName = gitParams[3]
		//def gitProject = 'foo-mf'
		gitProject = gitParams[4].split('\\.')[0]
	}
	
	stage('Check environment'){
		sh("git --version")
		sh("whereis git")
		sh("pwd")
		sh("ls -la")
	}
	
	stage("Clean up"){
		sh("rm -rf ${gitProject}")
		sh("rm -rf export")
		sh("rm -rf pretty")
		sh("rm -f json-files-found.txt")
	}
	
	stage('Clone Git repo'){
		withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: gitCredId, usernameVariable: 'gitUser', passwordVariable: 'gitPassword']]) {		
			
			//If password contains '@' character it must be encoded to avoid being mistaken by the '@' that separates user:password@url expression.
			String encodedGitPassword = gitPassword.contains("@") ? URLEncoder.encode(gitPassword) : gitPassword
		
			sh ("git clone ${gitProtocol}//${gitUser}:${encodedGitPassword}@${gitDomain}/${orgName}/${gitProject}.git")
			sh("ls -la")
			// Config local user and push mode.
			// TODO: Do a curl to the Github API to get the user's email.
			sh("""
				cd ${gitProject}
				pwd
				ls -la
				git config --local user.name ${gitUser}
				git config --local push.default simple
				git checkout ${gitBranch}
			""")
		}
		sh("""
			cd ${gitProject}
			git config -l
		""")
	}

	stage("Export from Mobile Fabric"){
		if(mfCliLocation == "S3"){
			sh("aws s3 cp ${mfcliS3Url} ./")
		}
		else{
			sh("curl -o mfcli.jar ${mfcliHttpsUrl}")
		}
		
		withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: mfCredId, usernameVariable: 'mfUser', passwordVariable: 'mfPassword']]) {		
			sh("java -jar mfcli.jar export -t ${mfAccountId} -u ${mfUser} -p ${mfPassword} -a ${mfAppId} -f ${gitProject}.zip")
			sh("unzip ${gitProject}.zip -d ./export")
			sh("ls -la")
		}
	}

	stage("Prettify JSON defs"){
		
		def workspace = pwd() 
		def MfJsonParser = load("${workspace}@script/MfJsonParser.groovy")
		def PathHelper = load("${workspace}@script/PathHelper.groovy")
		echo "Done loading Groovy modules"
		
		/* Copy the exported files to a new directory in order to prettify.
		We avoid overwritting the originals in case we want to use them to import again.*/
		//sh("cp -R ./export ./pretty")
		
		//Create a list of all the JSON files we have to prettify.
		//sh("find ./pretty -type f -name *.json > json-files-found.txt")
		sh("find ./export -type f -name *.json > json-files-found.txt")
		
		//Prettify all JSON files found.
		def jsonFilePaths = readFile("json-files-found.txt").split("\n")
		echo("JSON files found: ${jsonFilePaths}")
		
		for(int k = 0; k < jsonFilePaths.size(); k++){
			def jsonPath = jsonFilePaths[k]
			echo("File: ${jsonPath}")
			def pretty = MfJsonParser.prettify(readFile(jsonPath))
			echo("Pretty: ${pretty}")
			def prettyJsonPath = PathHelper.getPrettyFilePathName(jsonPath)
			writeFile(
				//file: jsonPath,
				file: prettyJsonPath,
				text: pretty
			)
		}

		//Overwrite existing exports with newly exported and prettified ones.
		sh("""
			rm -rf ./${gitProject}/export
			mv -f ./export ./${gitProject}/
		""")
		//sh("mv -f ./pretty ./${gitProject}/pretty")
		sh("""
			cd ${gitProject}
			pwd
			ls -la 
		""")
	}

	stage ('Push changes to remote'){
		
		//Using ssh keys doesn't require credentials.
		//TODO: Move to use credentials through Jenkins Credentials Plugin.
		withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: gitCredId, usernameVariable: 'gitUser', passwordVariable: 'gitPassword']]) {	
			
			//If password contains '@' character it must be encoded to avoid being mistaken by the '@' that separates user:password@url expression.
			String encodedGitPassword = gitPassword.contains("@") ? URLEncoder.encode(gitPassword) : gitPassword

			dir(gitProject){
				//Check status and stage all changes for commit.
				sh("""
					git status
					git add .
					git status
				""")
				//Check whether there are in fact any changes to commit.
				def dirty = sh (
					script: "git diff --cached --exit-code",
					returnStatus: true
				) != 0
				// dirty != 0 => There are changes to push.
				echo "dirty=${dirty}"
				
				if(dirty){
					echo "Changes found."
					sh ("""
						git commit -m 'Updates definitions of Mobile Fabric services'
						git push ${gitProtocol}//${gitUser}:${encodedGitPassword}@${gitDomain}/${orgName}/${gitProject}.git
					""")
					echo "Done pushing changes."
				}
				else {
					echo "No changes to commit."
				}
			}
		}
	}
	
	stage("Wrap up"){
		/*Store this export so that in the next execution we
		can compare the checksum of this zip with the newer one to quickly see
		if there are any changes, before going through the trouble of pulling,,prettifying and pushing*/
		sh("mv ${gitProject}.zip ${gitProject}_OLD.zip")
	}
}
