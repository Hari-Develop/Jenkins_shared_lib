#!groovy
def decidePipeline(Map configMap){
    application = configMap.get("application")
    switch(application) {
        case 'nodejsVM':
            nodejsVM(configMap)
            break
        case 'JavajsVM':
            javajsVM(configMap)
            break
        case 'nodeEKS':
            nodeEKS(configMap)
            break
        default:
            error "application is not  recognised"
            break
    }
}
