package com.recomdata.transmart.domain.i2b2

class ExtDataType {

    String name
    static hasMany = [extData: ExtData]
    static mapping = {
            table 'I2B2METADATA.I2B2_EXTDATATYPE'
            version false
            id generator: 'sequence', params: [sequence: 'EXTDATATYPE_SEQ']
            columns {
                id               column: 'EXTDATATYPE_ID'
                name             column: 'NAME'
            }
        }
}
