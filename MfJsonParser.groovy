import groovy.json.JsonSlurper
import groovy.json.JsonOutput

@NonCPS
def prettify(txt){
	println("Prettifying...")
	println("Orig content: ${txt}")
	
	def slurper = new JsonSlurper()
	println("Created slurper")
	
	def obj = slurper.parseText(txt)
	println("Parsed object of type ${obj.getClass()}")
	println("Basic parse: ${obj}")
	
	if(obj instanceof Map){
		if(obj.config != null && obj.config instanceof String){
			def config = obj.config
			//assert config instanceof String
			obj.config = slurper.parseText(config)
		}
		
		if(obj.policyConfig != null && obj.policyConfig instanceof String){
			def policyConfig = obj.policyConfig
			//assert policyConfig instanceof String
			obj.policyConfig = slurper.parseText(policyConfig)
		}
	}
	println("Final parse: ${obj}")
	
	String json = JsonOutput.toJson(obj)
	println("JSONified content: ${json}")
	
	def pretty = JsonOutput.prettyPrint(json)
	println("Prettified content: ${pretty}")
	
	return pretty
}
return this;
