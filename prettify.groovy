
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

@NonCPS
def prettify(txt){
    
    def jsonOutput = new JsonOutput()
    def unescaped = jsonOutput.unescaped(txt)
    assert unescaped instanceof JsonOutput.JsonUnescaped

    println(unescaped)
    println("flag 0")

    def fixed = unescaped.toString().replaceAll(/"\{/, "{").replaceAll(/\}"/, "}")
    println(fixed)
    println("flag 2")

    return JsonOutput.prettyPrint(fixed)
}

return this;
