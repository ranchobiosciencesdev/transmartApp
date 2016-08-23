package com.recomdata.transmart

import com.recomdata.transmart.domain.i2b2.ExtData
import com.recomdata.transmart.domain.i2b2.ExtDataType
import grails.transaction.Transactional
import groovy.sql.Sql
import org.transmart.authorization.CurrentUserBeanProxyFactory
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.core.ontology.Study
import org.transmartproject.core.users.User
import org.transmartproject.db.concept.ConceptKey

import javax.annotation.Resource
import javax.sql.DataSource

import static org.transmartproject.core.users.ProtectedOperation.WellKnownOperations.EXPORT

class ExternalDataService {

    @Resource(name = CurrentUserBeanProxyFactory.BEAN_BAME)
    User user

    DataSource dataSource;
    ConceptsResource conceptsResourceService

    def fileDownloadService
    def externalFilesDownloadService

    /**
     * Check if current user has export permission for specified study.
     *
     * @param study_concept_key
     * @return
     */
    boolean hasExportPermission(String study_concept_key) {
        Study study = conceptsResourceService.getByKey(study_concept_key).study
        return user.canPerform(EXPORT, study)
    }

    /**
     * Check if current user has permission for add/edit/delete external data links
     * (only admin can edit study)
     */
    boolean hasManagePermission(String study_concept_key) {
        return org.transmartproject.db.user.User.get(user.id).isAdmin()
    }

    /**
     * Validate that current user has export permission for specified study.
     *
     * If user hasn't permission then *AccessDeniedException* will be raised.
     *
     * @param study_concept_key
     */
    void checkExportPermissions(String study_concept_key) {
        if (!hasExportPermission(study_concept_key)) {
            def conceptKey = new ConceptKey(study_concept_key)
            throw new AccessDeniedException("User " +
                    "${user.username} has no EXPORT permission on " +
                    "study ${conceptKey.conceptFullName}")
        }
    }

    /**
     * Validate that current user has permission to add/edit/delete external data links.
     *
     * If user hasn't permission then *AccessDeniedException* will be raised.
     *
     * @param study_concept_key
     */
    void checkManagePermissions(String study_concept_key) {
        if (!hasManagePermission(study_concept_key)) {
            def conceptKey = new ConceptKey(study_concept_key)
            throw new AccessDeniedException("User " +
                    "${user.username} has no manage external study permission on " +
                    "study ${conceptKey.conceptFullName}")
        }
    }

    /**
     * Exports external data.
     *
     * @param extDataIdMap Map object with structure like ["subset1": [<extDataId1>, <extDataId2>, ...], "subset2": [ ... ]]
     * @param outputDir folder for saving output data
     */
    void exportExternalData(Map<String, List<Long>> extDataIdMap, String outputDir) {
        Map<String, List<ExtData>> extDataMap = [:]
        extDataIdMap.each { subset, idList ->
            extDataMap[subset] = idList.collect { id ->
                def extData = ExtData.get(id)
                checkExportPermissions(extData.studyConceptKey)
                return extData
            }
        }

        // export external data
        extDataMap.each { subset, extDataList ->
            File extStudyDir = new File(outputDir, subset + "_ext_data")
            extDataList.each { extData ->
                File extDir = new File(extStudyDir, "external_" + extData.name.replaceAll(" ", "_").replaceAll("[^a-zA-Z0-9]", "") + "_" + extData.id)
                extDir.mkdirs()
                def printDataInfo = { writer ->
                    writer.writeLine "Data node:  ${extData.pathNode}"
                    writer.writeLine "Name: ${extData.name}"
                    writer.writeLine "Data type: ${extData.dataType.name}"
                    writer.writeLine "Link: ${extData.link}"
                    writer.writeLine "Description: ${extData.description}"
                }
                try {
                    String protocol = new URL(extData.link).protocol
                    switch (protocol) {
                        case "http":
                        case "https":
                            fileDownloadService.getFiles([extData.link], extDir.toString())
                            break;
                        case "ftp":
                            externalFilesDownloadService.downloadFileFromFTPServer([extData.link], extDir.toString())
                            break;
                        default:
                            new File(extDir, "ErrorLog.txt").withWriter("UTF-8") { writer ->
                                writer.writeLine "Error. Unsupported protocol \"${protocol}\""
                                printDataInfo(writer)
                            }
                            break;
                    }
                } catch (Exception e) {
                    new File(extDir, "ErrorLog.txt").withWriter { writer ->
                        writer.writeLine "Error. Exception \"${e}\""
                        printDataInfo(writer)
                        e.printStackTrace(new PrintWriter(writer))
                    }
                }
            }
        }
    }

    /**
     * Add new external data record.
     *
     * @param conceptKey concept key of the study
     * @param name
     * @param description
     * @param dataType
     * @param link
     * @param login
     * @param password
     */
    @Transactional
    void addExtData(String conceptKey, String name, String description, ExtDataType dataType,
                    String link, login = null, password = null) {
        checkManagePermissions(conceptKey)
        ExtData extData = new ExtData()
        extData.studyConceptKey = conceptKey
        extData.name = name
        extData.description = description
        extData.dataType = dataType
        extData.link = link
        extData.login = login
        extData.password = password
        extData.save(failOnError: true)
        // add node
        String trialId = conceptsResourceService.getByKey(conceptKey).study.id
        Sql sql = new Sql(dataSource)
        try {
            sql.call("{call TM_CZ.I2B2_ADD_NODE($trialId, $extData.pathNode, $extData.pathNodeName, null)}")
            if (dataType.name.compareToIgnoreCase("Clinical Data")) {
                sql.executeUpdate("update i2b2metadata.i2b2 set C_VISUALATTRIBUTES='LA' where C_FULLNAME = $extData.pathNode")
            } else {
                sql.executeUpdate("update i2b2metadata.i2b2 set C_VISUALATTRIBUTES='LAH' where C_FULLNAME = $extData.pathNode")
            }
        } finally {
            sql.close()
        }
    }

    /**
     * Delete external data
     *
     * @param id
     */
    @Transactional
    void deleteExtData(long id) {
        ExtData extData = ExtData.get(id)
        checkManagePermissions(extData.studyConceptKey)
        Sql sql = new Sql(dataSource)
        try {
            sql.call("{call TM_CZ.I2B2_DELETE_ALL_NODES($extData.pathNode, null)}")
        } finally {
            sql.close()
        }
        extData.delete()
    }

    /**
     * Update external data record.
     *
     * @param id
     * @param name
     * @param description
     * @param dataType
     * @param link
     * @param login
     * @param password
     */
    @Transactional
    void updateExtData(long id, String name, String description, ExtDataType dataType,
                       String link, login = null, password = null) {
        ExtData extData = ExtData.get(id)
        String oldNodePath = extData.pathNode
        checkManagePermissions(extData.studyConceptKey)
        extData.name = name
        extData.description = description
        extData.dataType = dataType
        extData.link = link
        extData.login = login
        extData.password = password
        extData.save(failOnError: true)
        // update visual node
        String trialId = conceptsResourceService.getByKey(extData.studyConceptKey).study.id
        Sql sql = new Sql(dataSource)
        try {
            sql.call("{call TM_CZ.I2B2_DELETE_ALL_NODES($oldNodePath, null)}")
            sql.call("{call TM_CZ.I2B2_ADD_NODE($trialId, $extData.pathNode, $extData.pathNodeName, null)}")
            if (dataType.name.compareToIgnoreCase("Clinical Data")) {
                sql.executeUpdate("update i2b2metadata.i2b2 set C_VISUALATTRIBUTES='LA' where C_FULLNAME = $extData.pathNode")
            } else {
                sql.executeUpdate("update i2b2metadata.i2b2 set C_VISUALATTRIBUTES='LAH' where C_FULLNAME = $extData.pathNode")
            }
        } finally {
            sql.close()
        }
    }

    /**
     * Get external data object.
     *
     * @param id
     * @return
     */
    ExtData getExtData(long id) {
        ExtData extData = ExtData.get(id)
        // check study permission
        checkManagePermissions(extData.studyConceptKey)
        return extData
    }

    /**
     * Get all *ExtData* objects for specified study.
     *
     * @param conceptKey
     * @return
     */
    List getExtDataForStudy(String conceptKey) {
        return ExtData.findAllByStudyConceptKey(conceptKey, [sort: "id", order: "asc"])
    }
}
