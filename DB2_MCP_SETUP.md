
# DB2 Query MCP Server Setup Guide

## Overview

This MCP server uses Java JDBC driver to connect to DB2 database (instead of native Node.js bindings which require DB2 client libraries).

## Setup

### 1. Update Your MCP Configuration

Use the following config in your MCP client (e.g., Claude Desktop config):

```json
{
  "mcpServers": {
    "db2-query": {
      "command": "node",
      "args": [
        "E:\\gitwork\\npx-mcp-collection\\db2-query-mcp\\index.js"
      ],
      "env": {
        "JAVA_HOME": "D:\\usr\\java\\jdk1.8.0_491x64",
        "DB2_JDBC_URL": "jdbc:db2://localhost:50000/MAXIMO:currentSchema=MAXIMO;",
        "DB2_USERNAME": "db2inst1",
        "DB2_PASSWORD": "Maximo123",
        "DB2_SCHEMA": "MAXIMO"
      }
    }
  }
}
```

### 2. Verify Java Class File

The Java DB2 query class should already be compiled:
- Location: `E:\gitwork\npx-mcp-collection\db2-query-mcp\java\Db2Query.class`
- JDBC Driver: `E:\gitwork\npx-mcp-collection\db2-query-mcp\java\jcc-11.5.9.0.jar`

If the class file is missing, recompile with:
```cmd
cd E:\gitwork\npx-mcp-collection\db2-query-mcp\java
"D:\usr\java\jdk1.8.0_491x64\bin\javac" -encoding UTF-8 -cp "jcc-11.5.9.0.jar" Db2Query.java
```

### 3. Test Connection First

Test the connection before starting MCP:
```cmd
cd E:\gitwork\npx-mcp-collection\db2-query-mcp\java
set DB2_JDBC_URL=jdbc:db2://localhost:50000/MAXIMO:currentSchema=MAXIMO;
set DB2_USERNAME=db2inst1
set DB2_PASSWORD=Maximo123
set DB2_SCHEMA=MAXIMO
"D:\usr\java\jdk1.8.0_491x64\bin\java" -cp ".;jcc-11.5.9.0.jar" Db2Query info
```

## Available Tools

### 1. `get_database_info`
Get basic database info (name, current schema, user)

### 2. `query_tables`
List tables in the database. Supports pattern matching with `%`.
- Parameters:
  - `tableName` (optional, default "%"): Table name pattern
  - `schema` (optional): Schema name
  - `limit` (optional, default 200): Max tables to return

### 3. `query_table_columns`
Get column definitions for a specific table
- Parameters:
  - `tableName` (required): Table name
  - `schema` (optional): Schema name

### 4. `query_by_sql`
Execute read-only SQL queries
- Parameters:
  - `sql` (required): SELECT statement
  - `limit` (optional, default 200, max 1000): Max rows to return

## Troubleshooting

### Java not found
Ensure `JAVA_HOME` is correctly set and points to a valid JDK 8 installation.

### DB2 connection issues
Verify:
1. DB2 server is running on `localhost:50000`
2. Username/password are correct
3. Database `MAXIMO` exists
4. JDBC URL format is correct

### Classpath issues
Ensure both `.` (current directory) and `jcc-11.5.9.0.jar` are in the classpath.

## License
MIT
