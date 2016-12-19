def replaceLast(String text, String regex, String replacement) {
    return text.replaceFirst("(?s)"+regex+"(?!.*?"+regex+")", replacement);
}

def getPrettyFilePathName(path){
    
    String[] parts = path.split("/")
    String uglyFileName = parts[parts.size()-1]
    println(uglyFileName)

    int extIndex = uglyFileName.toLowerCase().lastIndexOf(".json")
    String prettyFileName = uglyFileName.substring(0, extIndex) + ".pretty.json"
    println(prettyFileName)

    String newPath = replaceLast(path, uglyFileName, prettyFileName)
    return newPath
}

return this;
