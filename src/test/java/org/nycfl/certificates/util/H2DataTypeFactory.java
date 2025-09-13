package org.nycfl.certificates.util;

import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.DataTypeException;
import org.dbunit.dataset.datatype.StringDataType;

public class H2DataTypeFactory extends org.dbunit.ext.h2.H2DataTypeFactory {
    @Override
    public DataType createDataType(int sqlType, String sqlTypeName) throws DataTypeException {
        if (sqlTypeName != null && sqlTypeName.startsWith("ENUM(")) { //
            // Replace
            // with your
            // actual enum type name
            return new StringDataType("your_enum_type_name", sqlType); // Or a more specific
            // DataType if available
        }
        return super.createDataType(sqlType, sqlTypeName);
    }
}
