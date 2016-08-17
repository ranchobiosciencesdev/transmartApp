package com.recomdata.transmart.data.export

import com.recomdata.transmart.domain.i2b2.ExtData
import grails.converters.JSON
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.ontology.Study
import org.transmartproject.core.querytool.QueryDefinition
import org.transmartproject.core.users.User

import java.util.regex.Matcher
import java.util.regex.Pattern

import static org.transmartproject.core.users.ProtectedOperation.WellKnownOperations.EXPORT

class DataExportController {

    def exportService
    def exportMetadataService
    def springSecurityService
    def queriesResourceAuthorizationDecorator
    def studiesResourceService
    def currentUserBean
    def queryDefinitionXmlService

    private static final String ROLE_ADMIN = 'ROLE_ADMIN'

    def index = {}

    //We need to gather a JSON Object to represent the different data types.
    def getMetaData() {
        checkParamResultInstanceIds()
        // get information for base select checkboxes
        Map result = exportMetadataService.getMetaData(
                params.long("result_instance_id1"),
                params.long("result_instance_id2"))

        // add data for checkboxes with external data
        // function to creation data for checkboxes
        Closure<Map> generateExternalDataRecord = { String dataTypeId, String dataTypeName,
                                                    boolean inSubset1, boolean inSubset2 ->
            Map res = [
                    "subsetId1"        : "subset1",
                    "subsetId2"        : "subset2",
                    "subsetName1"      : "Subset 1",
                    "subsetName2"      : "Subset 2",
                    "dataTypeId"       : dataTypeId,
                    "dataTypeName"     : dataTypeName,
                    "subset1"          : [
                            "dataTypeHasCounts": false,
                            "exporters"        : [
                                    ["format"     : "ExtFiles",
                                     "description": "External files"]],
                    ],
                    "subset2"          : [
                            "dataTypeHasCounts": false,
                            "exporters"        : [
                                    ["format"     : "ExtFiles",
                                     "description": "External files"]],
                    ]
            ]
            if (!inSubset1) {
                res["subset1"]["patientsNumber"] = 0
            }
            if (!inSubset2) {
                res["subset2"]["patientsNumber"] = 0
            }
            return res
        }
        // parse raw data of the query to get data which attached external
        Map<String, QueryDefinition> queryDefinitions = [
                "subset1": queryDefinitionXmlService.fromXml(new StringReader(params.crc_query_1)),
                "subset2": queryDefinitionXmlService.fromXml(new StringReader(params.crc_query_2))
        ]
        // fetch information about external files in the queries
        Map externalFiles = [:]
        Closure conceptKeyToSqlLikeCondition = { key -> key.substring(key.indexOf("\\", 2)).replace("\\", "\\\\") + "%"}
        queryDefinitions.each { subset, queryDefinition ->
            if (queryDefinition.panels.size() > 0) {
                List<ExtData> results = ExtData.createCriteria().list {
                    and {
                        for (def panel : queryDefinition.panels) {
                            if (!panel.invert) {
                                or {
                                    for (def item : panel.items) {
                                        like("study", conceptKeyToSqlLikeCondition(item.conceptKey))
                                    }
                                }
                            } else {
                                not {
                                    or {
                                        for (def item : panel.items) {
                                            like("study", conceptKeyToSqlLikeCondition(item.conceptKey))
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
                for (ExtData extData : results) {
                    if (!(extData.id in externalFiles)) {
                        externalFiles[extData.id] = [
                                "dataTypeId"  : "extFile-${extData.id}",
                                "dataTypeName": "External ${extData.dataType.name} \"${extData.name}\" (${extData.study})",
                                "subset1"     : false,
                                "subset2"     : false
                        ]
                    }
                    externalFiles[extData.id][subset] = true
                }
            }
        }
        // add information about external data to checkboxes
        for (Map externalFile : externalFiles.values().sort { a, b -> a["dataTypeName"].compareToIgnoreCase(b["dataTypeName"]) }) {
            result["exportMetaData"].add(generateExternalDataRecord(
                    externalFile["dataTypeId"],
                    externalFile["dataTypeName"],
                    externalFile["subset1"],
                    externalFile["subset2"]
            ))
        }

        render result as JSON
    }

    def downloadFileExists() {
        checkJobAccess params.jobname

        def InputStream inputStream = exportService.downloadFile(params);
        def result = [:]

        if (inputStream) {
            result.fileStatus = true
            inputStream.close()
        } else {
            result.fileStatus = false
            result.message = "Download failed as file could not be found on the server"
        }

        render result as JSON
    }

    def downloadFile() {
        checkJobAccess params.jobname

        def InputStream inputStream = exportService.downloadFile(params)

        def fileName = params.jobname + ".zip"
        response.setContentType 'application/zip'
        response.setHeader "Content-disposition", "attachment;filename=${fileName}"
        response.outputStream << inputStream
        response.outputStream.flush()
        inputStream.close();
        return true;
    }

    /**
     * Method that will create the new asynchronous job name
     * Current methodology is username-jobtype-ID from sequence generator
     */
    def createnewjob() {
        def result = exportService.createExportDataAsyncJob(params, springSecurityService.getPrincipal().username)

        response.setContentType("text/json")
        response.outputStream << result.toString()
    }

    /**
     * Method that will run a data export and is called asynchronously from the datasetexplorer -> Data Export tab
     */
    def runDataExport() {
        checkParamResultInstanceIds()

        def jsonResult = exportService.exportData(params, springSecurityService.getPrincipal().username)

        response.setContentType("text/json")
        response.outputStream << jsonResult.toString()
    }

    private void checkParamResultInstanceIds() {
        def resultInstanceId1 = params.long("result_instance_id1")
        def resultInstanceId2 = params.long("result_instance_id2")

        if (!resultInstanceId1 && !resultInstanceId2) {
            throw new InvalidArgumentsException("No result instance id provided")
        }

        checkResultInstanceIds resultInstanceId1, resultInstanceId2
    }

    private checkJobAccess(String jobName) {
        if (isAdmin()) {
            return
        }

        String loggedInUsername = springSecurityService.principal.username
        String jobUsername = extractUserFromJobName(jobName)

        if (jobUsername != loggedInUsername) {
            log.warn("Denying access to job $jobName because the " +
                    "corresponding username ($jobUsername) does not match " +
                    "that of the current user")
            throw new AccessDeniedException("Job $jobName was not started by " +
                    "this user")
        }
    }

    private void checkResultInstanceIds(... rids) {
        // check that the user has export access in the studies of patients
        Set<Study> studies = (rids as List).
                findAll().collect {
            queriesResourceAuthorizationDecorator.getQueryResultFromId it
        }*.
                patients.
                inject { a, b -> a + b }. // merge two patient sets into one
                inject([] as Set, { a, b -> a + b.trial }).
                collect { studiesResourceService.getStudyById it }

        studies.each { study ->
            assert currentUserBean instanceof User
            if (!currentUserBean.canPerform(EXPORT, study)) {
                throw new AccessDeniedException("User " +
                        "$currentUserBean.username has no EXPORT permission on " +
                        "study $study.id")
            }
        }
    }

    // copied from Rmodules' AnalysisFilesController
    // Job control and file download access ought to be unified
    private boolean isAdmin() {
        springSecurityService.principal.authorities.any {
            it.authority == ROLE_ADMIN
        }
    }

    static private String extractUserFromJobName(String jobName) {
        Pattern pattern = ~/(.+)-[a-zA-Z]+-\d+/
        Matcher matcher = pattern.matcher(jobName)

        if (!matcher.matches()) {
            throw new IllegalStateException('Invalid job name')
        }

        matcher.group(1)
    }
}


