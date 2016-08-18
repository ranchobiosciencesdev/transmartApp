package com.recomdata.transmart.domain.i2b2

class ExtData {
    String name
    String description
    String link
    String study
    String pathnode
    static belongsTo = [dataType: ExtDataType]

    static mapping = {
        table 'I2B2METADATA.I2B2_EXTDATA'
        version false
        // TODO: Fix problem with generator strategy
        id generator: 'sequence' //, params: [sequence: 'I2B2METADATA.EXTDATA_SEQ']
        columns {
            id               column: 'EXTDATA_ID'
            name             column: 'NAME'
            description      column: 'DESCRIPTION'
            link             column: 'LINK'
            study            column: 'STUDY'
            dataType         column: 'DATATYPE_ID'
            pathnode         column: 'PATHNODE'
        }
    }
    static constraints = {
        name(blank:false, nullable: false, maxSize: 100)
    }
}
