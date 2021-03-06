package liquibase.statement.core;

import liquibase.statement.AbstractSqlStatement;

public class DropPrimaryKeyStatement extends AbstractSqlStatement {

    private String schemaName;
    private String tableName;
    private String constraintName;

    public DropPrimaryKeyStatement(String schemaName, String tableName, String constraintName) {
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.constraintName = constraintName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getConstraintName() {
        return constraintName;
    }

}
