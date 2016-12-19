import groovy.json.JsonSlurper
import groovy.json.JsonOutput

@NonCPS
def prettify(txt){
	println("Prettifying...")
	println("Orig content: ${txt}")
	
	def slurper = new JsonSlurper()
	
	def obj = slurper.parseText(txt)
	
	if(obj.config != null){
		def config = obj.config
		assert config instanceof String
		obj.config = slurper.parseText(config)
	}
	
	if(obj.policyConfig != null){
		def policyConfig = obj.policyConfig
		assert policyConfig instanceof String
		obj.policyConfig = slurper.parseText(policyConfig)
	}
	
	println("Parsed object: ${obj}")
	
	def pretty = JsonOutput.prettyPrint(JsonOutput.toJson(obj))
	println("Prettified content: ${pretty}")
	
	return pretty
}

return this;
