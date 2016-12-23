
def MfJsonParser, PathHelper, servicesChanged

node{
	stage('Load Groovy modules'){
		def workspace = pwd() 
		MfJsonParser = load("${workspace}@script/MfJsonParser.groovy")
		PathHelper = load("${workspace}@script/PathHelper.groovy")
		echo "Done loading Groovy modules"
	}
}

node('mobilefabric') {
	def gitProtocol, gitDomain, orgName, gitProject
	def gitCredId = GIT_CREDENTIALS //Credentials parameter
	def exportRepoUrl = EXPORT_REPO_URL //String parameter
	def gitBranch = EXPORT_REPO_BRANCH //'master' //String parameter
	def mfCredId = MF_CREDENTIALS
	def mfAccountId = MOBILE_FABRIC_ACCOUNT_ID
	def mfAppId = MOBILE_FABRIC_APP_ID
	def mfCliLocation = MF_CLI_LOCATION //HTTPS or S3 URL to fetch mfcli.jar from.
	def commitAuthor = COMMIT_AUTHOR //The name of the user that will be logged as the author of the changes pushed.
	def authorEmail = AUTHOR_EMAIL //The e-mail address of the user that will be logged as author of any changes pushed.
	def commitMessage = COMMIT_MESSAGE?COMMIT_MESSAGE:"Automatic backup of MobileFabric services." //The comments to commit any changes found.
	
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
	
	stage("Export from Mobile Fabric"){
		if(mfCliLocation.startsWith("s3")){
			sh("aws s3 cp ${mfCliLocation} ./")
		}
		else{
			sh("curl -o mfcli.jar ${mfCliLocation}")
		}
		
		withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: mfCredId, usernameVariable: 'mfUser', passwordVariable: 'mfPassword']]) {		
			sh("java -jar mfcli.jar export -t ${mfAccountId} -u ${mfUser} -p ${mfPassword} -a ${mfAppId} -f ${gitProject}.zip")
		}
		
		//Unzips the exported zip into a temporary /path/to/workspace/export directory.
		sh("unzip ${gitProject}.zip -d ./export")
		sh("ls -la")
		
		/* Verify whether there are any changes between the last export and this one.
		This is to avoid extracting and prettifying the export if there are no changes.*/
		servicesChanged = sh (
			script: "zdiff ${gitProject}_OLD.zip ${gitProject}.zip",
			returnStatus: true
		) != 0
		// servicesChanged != 0 => There have been changes ot the Mobile Fabric service definitions.
		echo ("servicesChanged=${servicesChanged}")
	}
	
	stage('Clone Git repo'){
		if(servicesChanged){
			echo("Cloning Git repo...")
			withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: gitCredId, usernameVariable: 'gitUser', passwordVariable: 'gitPassword']]) {		
				
				//If password contains '@' character it must be encoded to avoid being mistaken by the '@' that separates user:password@url expression.
				String encodedGitPassword = gitPassword.contains("@") ? URLEncoder.encode(gitPassword) : gitPassword
			
				sh ("git clone ${gitProtocol}//${gitUser}:${encodedGitPassword}@${gitDomain}/${orgName}/${gitProject}.git")
				sh("ls -la")
				// Config local user and push mode.
				dir(gitProject) {
					sh("""
						pwd
						ls -la
						git config --local push.default simple
						git checkout ${gitBranch}
					""")
					if(commitAuthor != ""){
						sh("git config --local user.name '${commitAuthor}'")
					}
					else{
						sh("git config --local user.name Jenkins")
						commitMessage += " Used credentials of user ${gitUser}"
					}
					if(authorEmail != ""){
						sh("git config --local user.email ${authorEmail}")
					}
					sh("git config -l")
				}
			}
		}
		else{
			echo("Mobile Fabric services haven't changed so skipping clone.")
		}
	}

	////////

	stage("Prettify JSON defs"){
		if(servicesChanged){
			sh("""
				pwd
				ls -la
			""")
			
			//Create a list of all the JSON files we have to prettify.
			//sh("find ./pretty -type f -name *.json > json-files-found.txt")
			sh("find ./export -type f -name *.json > json-files-found.txt")
			
			//Prettify all JSON files found.
			def jsonFilePaths = readFile("json-files-found.txt").split("\n")
			echo("JSON files found: ${jsonFilePaths}")
			
			for(int k = 0; k < jsonFilePaths.size(); k++){
				def jsonPath = jsonFilePaths[k]
				echo("File path: ${jsonPath}")
				def pretty = MfJsonParser.prettify(readFile(jsonPath))
				//echo("Prettified content: ${pretty}")
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
			
			sh("""
				cd ${gitProject}
				pwd
				ls -la 
			""")
		}
		else{
			echo("Mobile Fabric services haven't changed so skipping prettification.")
		}
	}

	stage ('Push changes to remote'){
		if(servicesChanged){
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
					echo ("dirty=${dirty}")
					
					if(dirty){
						echo "Changes found."
						sh ("""
							git commit -m '${commitMessage}'
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
		else{
			echo("Mobile Fabric services haven't changed so skipping push.")
		}
	}
	
	stage("Wrap up"){
		/*Store this export so that in the next execution we
		can compare the checksum of this zip with the newer one to quickly see
		if there are any changes, before going through the trouble of pulling,,prettifying and pushing*/
		sh("mv ${gitProject}.zip ${gitProject}_OLD.zip")
	}
}
