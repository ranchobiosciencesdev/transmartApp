package com.recomdata.transmart.domain.i2b2

class ExtData {
    String name
    String description
    String link
    String study
    static belongsTo = [dataType: ExtDataType]

    static mapping = {
        table 'I2B2METADATA.I2B2_EXTDATA'
        version false
        //id generator: 'sequence', params: [sequence: 'I2B2METADATA.EXTDATA_SEQ']
        columns {
            id               column: 'extdata_id'
            name             column: 'NAME'
            description      column: 'DESCRIPTION'
            link             column: 'LINK'
            study            column: 'STUDY'
            dataType         column: 'DATATYPE_ID'
        }
    }
    static constraints = {
        name(nullable: false, maxSize: 100)
    }
}
