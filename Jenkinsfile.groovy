node('mobilefabric') {
	def gitProtocol, gitDomain, orgName, gitProject
	def gitCredId = GIT_CREDENTIALS //Credentials parameter
	def exportRepoUrl = EXPORT_REPO_URL //String parameter
	def gitBranch = EXPORT_REPO_BRANCH //'master' //String parameter
	def mfCredId = MF_CREDENTIALS
	def mfAccountId = MOBILE_FABRIC_ACCOUNT_ID
	def mfAppId = MOBILE_FABRIC_APP_ID
	def javaLibsS3Path = JAVA_LIBS_S3_PATH //s3://kony-ci0001-storage1/libraries/kony-appfactory-libraries/

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
		sh("aws s3 cp ${javaLibsS3Path}/mfcli.jar ./")
		
		withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: mfCredId, usernameVariable: 'mfUser', passwordVariable: 'mfPassword']]) {		
			sh("java -jar mfcli.jar export -t ${mfAccountId} -u ${mfUser} -p ${mfPassword} -a ${mfAppId} -f ${gitProject}.zip")
			sh("unzip ${gitProject}.zip -d ./export")
			sh("ls -la")
		}
	}

	stage("Prettify JSON defs"){
		sh("aws s3 cp ${javaLibsS3Path}/PrettyJSON.jar ./")
		sh("find ./export -type f -name *.json")
		sh("cp -R ./export ./pretty")
		sh("java -jar PrettyJSON.jar ./pretty")
		sh("find ./pretty -type f -name *.json")
		sh("mv -f ./export ./${gitProject}/export")
		sh("mv -f ./pretty ./${gitProject}/pretty")
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

			//Add changes and commit.
			def dirty = sh (
				script: """
					pwd
					ls -la
					cd ${gitProject}
					pwd
					ls -la
					git diff --exit-code
				""",
				returnStatus: true
			) != 0

			// dirty != 0 => There are changes to push.
			echo "dirty=${dirty}"

			if(dirty){
				echo "Changes found."
				sh ("""
					cd ${gitProject}
					git status
					git add .
					git commit -m 'Updates definitions of Mobile Fabric services'
					git push ${gitProtocol}//${GIT_USERNAME}:${encodedGitPassword}@${gitDomain}/${orgName}/${gitProject}.git
				""")
				echo "Done pushing changes."
			}
			else {
				echo "No changes to commit."
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
