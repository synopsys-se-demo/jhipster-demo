mport groovy.json.JsonSlurper

def testEnv() {
    println "The value is " + ${env.TESTENV}

}
          
def scheduleScan() {

    echo "Verify target if not exists create new target."
    createTargetIfNotExists()
    String targetId = getTargetId()
    String serviceId = getServiceId()
    String productVersionId = getProductVersionId(serviceId)
    List scopingQuestionnaire = getQuestionnaire(productVersionId)
    String scopingQuestionnaire1=scopingQuestionnaire[0]
    String scopingQuestionnaire2=scopingQuestionnaire[1]
          
    def web_scan = \
        httpRequest ignoreSslErrors: false,\
        acceptType: 'APPLICATION_JSON',\
        quiet: false,\
        httpMode: 'POST',\
        customHeaders:[[name:'token',\
        value:"${env.token}"],\
        [name:'Content-Type',value:'application/json']],\
        url:"${env.HostUrl}/api/public/v3/scans",\
        requestBody:"{\"name\":\"webTarget\",\
        \"targetId\":$targetId,\"target\":\"${env.targetUrl}\",\
        \"scheduledDate\":\"${env.ScheduleDate}\",\
        \"demoDate\":null,\
        \"assessmentType\":\"DAST Essential (DAST-E)\",\
        \"testType\":\"NON_INTRUSIVE\",\
        \"riskRatingMethodology\":null,\
        \"deploymentType\":\"STAGING_TESTING\",\
        \"revalidationScan\":false,\
        \"revalidationScanId\":null,\
        \"testWindowStartTime\":0,\
        \"testWindowDuration\":0,\
        \"serviceId\":$serviceId,\
        \"flagged\":false,\
        \"comments\":\"\",\
        \"state\":\"Scheduled\",\
        \"artifacts\":[],\
        \"pointOfContacts\":\ [{\"responsibilityDetails\":\"Emergency details.\",\
        \"responsibility\":\"Emergency Contact\",\
        \"phoneNumber\":\"0909090909\",\
        \"email\":\"kajsdfaz2@lasdjflsadjfa.com\",\
        \"required\":true}],\
        \"scopingForm\":{\"answers\":[{\"values\":[{\"value\":\"no\",\
        \"index\":0}],\
        \"questionId\":$scopingQuestionnaire1},\
        {\"values\":[{\"value\":\"no\",\
        \"index\":0}],\
        \"questionId\":$scopingQuestionnaire2},\
        {\"values\":[{\"value\":\"no\",\
        \"index\":0}],\
        \"questionId\":\"QUESTION_1540310146\"},\
        {\"values\":[{\"value\":\"no\",\
        \"index\":0}],\
        \"questionId\":\"QUESTION_2006791028\"},\
        {\"values\":[{\"value\":\"no\",\"index\":0}],\
        \"questionId\":\"QUESTION_1790325166\"}]},\
        \"targetSubType\":\"WEB_APPLICATION\"}"

    println(web_scan)
    uploadArtifacts()

}

def uploadArtifacts() {

    def filecontent = """${sh(returnStdout: true,script: "curl -X POST \
        '${env.HostUrl}/api/public/v3/scans/${env.scanId}/files' \
        -H 'accept: */*' \
        -H 'token: ${env.token}' \
        -H 'Content-Type: multipart/form-data' \
        -F 'assetType=supporting' \
        -F 'isInternal=true' \
        -F 'upfile=@${fileName};type=application/zip'")}""".trim()

}

def List getQuestionnaire(String productVersionId) {

    List arr = [];
    def allProductsResponse = httpRequest ignoreSslErrors: false,\
        acceptType: 'APPLICATION_JSON',quiet: true,\
        httpMode: 'GET',customHeaders:\
            [[name:'token',value:"${env.token}"],\
        [name:'contentType',value:'application/json']],\
        url:"${env.HostUrl}/api/public/v3/products/"\
            +productVersionId+"/scoping-forms"

    JsonSlurper slurper = new JsonSlurper()
    Map parsedJson = slurper.parseText(allProductsResponse.content)
      
    arr << parsedJson.questions[0].id
    arr << parsedJson.questions[1].id
    println(arr)
    return arr

}
          
def String getProductVersionId(String serviceId) {

    def allProductsResponse = httpRequest ignoreSslErrors: false,\
        acceptType: 'APPLICATION_JSON',quiet: true,\
        httpMode: 'GET',customHeaders:\
            [[name:'token',value:"${env.token}"],\
        [name:'contentType',value:'application/json']],\
        url:"${env.HostUrl}/api/public/v3/products?productType=WEB\
            &serviceId="+serviceId+"\
            &revalidationProduct=false"

    JsonSlurper slurper = new JsonSlurper()
    Map parsedJson = slurper.parseText( allProductsResponse.content )
    String productVersionId = parsedJson.products[0].id
    return productVersionId  

}

def String getTargetId() {

    def getTargetResponse = httpRequest ignoreSslErrors: false,\
        acceptType: 'APPLICATION_JSON',quiet: true,\
        httpMode: 'GET',customHeaders:\
            [[name:'token',value:"${env.token}"],\
        [name:'contentType',value:'application/json']],\
        url:"${env.HostUrl}/api/public/v3/targets?target="+env.TargetUrl

    println("Reponse:"+getTargetResponse.content)
    JsonSlurper slurper = new JsonSlurper()
    Map parsedJson = slurper.parseText(getTargetResponse.content)
    String targetId = parsedJson.targets[0].id
    return targetId        

}

def String getServiceId() {

    def allServicesResponse = httpRequest ignoreSslErrors: false,\
        acceptType: 'APPLICATION_JSON',quiet: true,\
        httpMode: 'GET',customHeaders:\
            [[name:'token',value:"${env.token}"],\
        [name:'contentType',value:'application/json']],\
        url:"${env.HostUrl}/api/public/v3/services?limit=9007199254740991\
            &targetType=WEB&status=ACTIVE&retestService=false"

    JsonSlurper slurper = new JsonSlurper()
    Map parsedJson = slurper.parseText(allServicesResponse.content)
    String serviceId = parsedJson.service[0].id
    return serviceId        

}

def createTargetIfNotExists() {

    boolean isTargetExists = verifyTarget()

    if ( !isTargetExists ) {
        println("target is not exists.. ")
        def createWebTarget = \
            httpRequest ignoreSslErrors: false,\
            quiet: false,\
            httpMode: 'POST',\
            customHeaders:[[name:'token',\
            value:"${env.Token}"],\
            [name:'Content-Type',value:'application/json']],\
            url:"${env.HostUrl}/api/public/v3/targets",\
            requestBody:"{\"name\":\"${env.targetName}\",\
            \"description\":\"${env.targetDesc}\",\
            \"type\":\"WEB\",\
            \"url\":\"${env.targetUrl}\"}"
    }

}
          
def boolean verifyTarget() {

    def getTargetResponse = httpRequest ignoreSslErrors: false,\
        acceptType: 'APPLICATION_JSON',\
        quiet: true,\
        httpMode: 'GET',customHeaders:\
            [[name:'token',value:"${env.token}"],\
        [name:'contentType',value:'application/json']],\
        url:"${env.HostUrl}/api/public/v3/targets?target="+env.TargetUrl
          
    println("Reponse:"+getTargetResponse.content)
    def totalTargetsCount = readJSON text: getTargetResponse.content
    return totalTargetsCount.total > 0 ? true : false

}
