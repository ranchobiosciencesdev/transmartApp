package com.recomdata.transmart.domain.i2b2

import org.transmartproject.db.concept.ConceptKey

class ExtData {
    String name
    String description
    String link
    String login
    String base64Password
    String studyConceptKey
    // pathNode mustn't be modified directly
    String pathNode
    static belongsTo = [dataType: ExtDataType]

    static transients = ['password']

    static mapping = {
        table 'I2B2METADATA.I2B2_EXTDATA'
        version false
        // TODO: Fix problem with generator strategy
        id generator: 'sequence' //, params: [sequence: 'I2B2METADATA.EXTDATA_SEQ']
        columns {
            id              column: 'EXTDATA_ID'
            name            column: 'NAME'
            description     column: 'DESCRIPTION'
            link            column: 'LINK'
            login           column: 'LINK_LOGIN'
            base64Password  column: 'LINK_PASSWORD'
            studyConceptKey column: 'STUDY_CONCEPT_KEY'
            dataType        column: 'DATATYPE_ID'
            pathNode        column: 'PATH_NODE'
        }
    }
    static constraints = {
        name(blank: false, nullable: false, maxSize: 100)
        password(blank: true, nullable: true)
        base64Password(blank: true, nullable: true)
        login(blank: true, nullable: true)
    }

    String getPassword() {
        if (base64Password == null) {
            return null
        } else {
            return new String(base64Password.decodeBase64())
        }
    }

    void setPassword(String val) {
        if (val == null) {
            base64Password = null
        } else {
            base64Password = val.bytes.encodeBase64().toString()
        }
    }

    void updatePathNode() {
        if (studyConceptKey != null & name != null) {
            pathNode = (new ConceptKey(studyConceptKey)).conceptFullName.toString() + pathNodeName + '\\'
        }
    }

    void setStudyConceptKey(String val) {
        studyConceptKey = val
        updatePathNode()
    }

    void setName(String val) {
        name = val
        updatePathNode()
    }

    String getPathNodeName() {
        return name + ' (ext)'
    }
}
