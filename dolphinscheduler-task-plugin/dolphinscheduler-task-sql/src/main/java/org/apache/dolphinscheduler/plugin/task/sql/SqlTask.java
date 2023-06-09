/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.plugin.task.sql;

import org.apache.dolphinscheduler.plugin.datasource.api.plugin.DataSourceClientProvider;
import org.apache.dolphinscheduler.plugin.datasource.api.utils.CommonUtils;
import org.apache.dolphinscheduler.plugin.datasource.api.utils.DatasourceUtil;
import org.apache.dolphinscheduler.plugin.datasource.api.utils.PasswordUtils;
import org.apache.dolphinscheduler.plugin.task.api.AbstractTaskExecutor;
import org.apache.dolphinscheduler.plugin.task.api.ShellCommandExecutor;
import org.apache.dolphinscheduler.plugin.task.api.TaskException;
import org.apache.dolphinscheduler.plugin.task.api.TaskResponse;
import org.apache.dolphinscheduler.plugin.task.util.MapUtils;
import org.apache.dolphinscheduler.plugin.task.util.OSUtils;
import org.apache.dolphinscheduler.spi.datasource.BaseConnectionParam;
import org.apache.dolphinscheduler.spi.enums.DbType;
import org.apache.dolphinscheduler.spi.enums.TaskTimeoutStrategy;
import org.apache.dolphinscheduler.spi.task.*;
import org.apache.dolphinscheduler.spi.task.paramparser.ParamUtils;
import org.apache.dolphinscheduler.spi.task.paramparser.ParameterUtils;
import org.apache.dolphinscheduler.spi.task.request.SQLTaskExecutionContext;
import org.apache.dolphinscheduler.spi.task.request.TaskRequest;
import org.apache.dolphinscheduler.spi.task.request.UdfFuncRequest;
import org.apache.dolphinscheduler.spi.utils.JSONUtils;
import org.apache.dolphinscheduler.spi.utils.StringUtils;

import org.apache.commons.collections.CollectionUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.hive.jdbc.HivePreparedStatement;
import org.apache.hive.jdbc.HiveStatement;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.apache.dolphinscheduler.spi.task.TaskConstants.RWXR_XR_X;

public class SqlTask extends AbstractTaskExecutor {

    /**
     * taskExecutionContext
     */
    private TaskRequest taskExecutionContext;

    /**
     * shell command executor
     */
    private ShellCommandExecutor shellCommandExecutor;

    /**
     * sql parameters
     */
    private SqlParameters sqlParameters;

    /**
     * base datasource
     */
    private BaseConnectionParam baseConnectionParam;

    /**
     * create function format
     */
    private static final String CREATE_FUNCTION_FORMAT = "create temporary function {0} as ''{1}''";

    /**
     * default query sql limit
     */
    private static final int QUERY_LIMIT = 10000;

    /**
     *  sign status
     */
    protected volatile int statusCode = 0;

    /**
     *  default new line char
     */
    private static final String DEFAULT_NEW_LINE_CHAR = "\n";

    /**
     *  default sql split char
     */
    private static final String DEFAULT_SQL_SPLIT_CHAR = ";";

    private static final String DEFAULT_SQL_RGEX = "\\$\\{((?!hiveconf:).*?)\\}";

    /**
     * Abstract Yarn Task
     *
     * @param taskRequest taskRequest
     */
    public SqlTask(TaskRequest taskRequest) {
        super(taskRequest);
        this.taskExecutionContext = taskRequest;
        this.sqlParameters = JSONUtils.parseObject(taskExecutionContext.getTaskParams(), SqlParameters.class);
        this.shellCommandExecutor = new ShellCommandExecutor(this::logHandle,
            taskExecutionContext,
            logger);
        assert sqlParameters != null;
        if (!sqlParameters.checkParameters()) {
            throw new RuntimeException("sql task params is not valid");
        }
    }

    @Override
    public AbstractParameters getParameters() {
        return sqlParameters;
    }

    @Override
    public void handle() throws Exception {

        logger.info("Full sql parameters: {}", sqlParameters);
        logger.info("sql type : {}, datasource : {}, sql : {} , localParams : {},udfs : {},showType : {},connParams : {},varPool : {} ,query max result limit  {}",
                sqlParameters.getType(),
                sqlParameters.getDatasource(),
                sqlParameters.getSql(),
                sqlParameters.getLocalParams(),
                sqlParameters.getUdfs(),
                sqlParameters.getShowType(),
                sqlParameters.getConnParams(),
                sqlParameters.getVarPool(),
                sqlParameters.getLimit());
        try {
            SQLTaskExecutionContext sqlTaskExecutionContext = taskExecutionContext.getSqlTaskExecutionContext();

            // get datasource
            baseConnectionParam = (BaseConnectionParam) DatasourceUtil.buildConnectionParams(
                    DbType.valueOf(sqlParameters.getType()),
                    sqlTaskExecutionContext.getConnectionParams());

            // ready to execute SQL and parameter entity Map
            SqlBinds mainSqlBinds = getSqlAndSqlParamsMap(sqlParameters.getSql());
            List<SqlBinds> preStatementSqlBinds = Optional.ofNullable(sqlParameters.getPreStatements())
                    .orElse(new ArrayList<>())
                    .stream()
                    .map(this::getSqlAndSqlParamsMap)
                    .collect(Collectors.toList());
            List<SqlBinds> postStatementSqlBinds = Optional.ofNullable(sqlParameters.getPostStatements())
                    .orElse(new ArrayList<>())
                    .stream()
                    .map(this::getSqlAndSqlParamsMap)
                    .collect(Collectors.toList());

            List<String> createFuncs = createFuncs(sqlTaskExecutionContext.getUdfFuncTenantCodeMap(),
                    sqlTaskExecutionContext.getDefaultFS(), logger);

            // execute sql task
            executeFuncAndSql(mainSqlBinds, preStatementSqlBinds, postStatementSqlBinds, createFuncs);

            if (statusCode ==  TaskConstants.EXIT_CODE_SUCCESS) {
                setExitStatusCode(TaskConstants.EXIT_CODE_SUCCESS);
            }
        } catch (Exception e) {
            setExitStatusCode(TaskConstants.EXIT_CODE_FAILURE);
            logger.error("sql task error: {}", e.toString());
            throw e;
        }
    }

    /**
     * execute function and sql
     *
     * @param mainSqlBinds main sql binds
     * @param preStatementsBinds pre statements binds
     * @param postStatementsBinds post statements binds
     * @param createFuncs create functions
     */
    public void executeFuncAndSql(SqlBinds mainSqlBinds,
                                  List<SqlBinds> preStatementsBinds,
                                  List<SqlBinds> postStatementsBinds,
                                  List<String> createFuncs) throws Exception {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet resultSet = null;
        String dbType = sqlParameters.getType();
        try {
            // 如果是hive 或者 spark的非查询，使用shell来执行脚本
            if (sqlParameters.getSqlType() == SqlType.NON_QUERY.ordinal()
                   && DbType.HIVE.getDescp().equalsIgnoreCase(dbType)) {
                StringBuffer sqlScript = new StringBuffer();
                // UDF
                if (CollectionUtils.isNotEmpty(createFuncs)) {
                    for (String createFunc : createFuncs) {
                        String trimFunc = trimRight(createFunc);
                        sqlScript.append(trimFunc).append(DEFAULT_NEW_LINE_CHAR);
                        logger.info("hive create function sql: {}", trimFunc);
                    }
                }

                // pre sql
                for (SqlBinds sqlBind : preStatementsBinds) {
                    String trimPreSql = trimRight(sqlBind.getSql());
                    sqlScript.append(trimPreSql).append(DEFAULT_NEW_LINE_CHAR);
                    logger.info("pre execute sql: {}", trimPreSql);
                }

                // main sql
                String trimMainSql = trimRight(mainSqlBinds.getSql());
                sqlScript.append(trimMainSql).append(DEFAULT_NEW_LINE_CHAR);
                logger.info("main execute sql: {}", trimMainSql);

                // post sql
                for (SqlBinds sqlBind : postStatementsBinds) {
                    String trimPostSql = trimRight(sqlBind.getSql());
                    sqlScript.append(trimPostSql).append(DEFAULT_NEW_LINE_CHAR);
                    logger.info("post execute sql: {}", trimPostSql);
                }

                // 以上放到文件 sql
                String sqlFile = getSqlFile(sqlScript.toString());

                // shell exec sql
                // 获取sh命令
                String rawScript = String.format("sudo -u hive beeline -n %s -p \"%s\" -u \"%s;%s\" -f %s",
                    baseConnectionParam.getUser(),
                    PasswordUtils.decodePassword(baseConnectionParam.getPassword()),
                    baseConnectionParam.getJdbcUrl(),
                    baseConnectionParam.getOther().replaceAll("SSL=1", "ssl=true").replaceAll("ssl=1", "ssl=true"),
                    sqlFile
                );
//                String rawScript = String.format("sudo -u hive beeline -n %s -p \"%s\" -u \"%s\" -f %s",
//                        baseConnectionParam.getUser(),
//                        PasswordUtils.decodePassword(baseConnectionParam.getPassword()),
//                        DatasourceUtil.getJdbcUrl(DbType.valueOf(dbType), baseConnectionParam),
//                        sqlFile);
                String command = buildCommand(rawScript);
                TaskResponse commandExecuteResult = shellCommandExecutor.run(command);
                statusCode = commandExecuteResult.getExitStatusCode();
                // 设置执行状态
                setExitStatusCode(commandExecuteResult.getExitStatusCode());

            } else {
                // create connection
                connection = DataSourceClientProvider.getInstance().getConnection(DbType.valueOf(sqlParameters.getType()), baseConnectionParam);
                // create temp function
                if (CollectionUtils.isNotEmpty(createFuncs)) {
                    createTempFunction(connection, createFuncs);
                }

                // pre sql
                preSql(connection, preStatementsBinds);

                // bind sql
                stmt = prepareStatementAndBind(connection, mainSqlBinds);
                String result = null;
                // decide whether to executeQuery or executeUpdate based on sqlType
                if (sqlParameters.getSqlType() == SqlType.QUERY.ordinal()) {
                    // query statements need to be convert to JsonArray and inserted into Alert to send
                    resultSet = stmt.executeQuery();
                    result = resultProcess(resultSet);
                } else if (sqlParameters.getSqlType() == SqlType.NON_QUERY.ordinal()) {
                    stmt = prepareStatementAndBind(connection, mainSqlBinds);
                    String updateResult = String.valueOf(stmt.executeUpdate());
                    result = setNonQuerySqlReturn(updateResult, sqlParameters.getLocalParams());
                }
                // deal out params
                sqlParameters.dealOutParam(result);

                // post sql
                postSql(connection, postStatementsBinds);
            }

        } catch (Exception e) {
            logger.error("execute sql error: {}", e.getMessage());
            throw e;
        } finally {
            close(resultSet, stmt, connection);
        }
    }

    private String setNonQuerySqlReturn(String updateResult, List<Property> properties) {
        String result = null;
        for (Property info : properties) {
            if (Direct.OUT == info.getDirect()) {
                List<Map<String, String>> updateRL = new ArrayList<>();
                Map<String, String> updateRM = new HashMap<>();
                updateRM.put(info.getProp(), updateResult);
                updateRL.add(updateRM);
                result = JSONUtils.toJsonString(updateRL);
                break;
            }
        }
        return result;
    }

    /**
     * result process
     *
     * @param resultSet resultSet
     * @throws Exception Exception
     */
    private String resultProcess(ResultSet resultSet) throws Exception {
        ArrayNode resultJSONArray = JSONUtils.createArrayNode();
        if (resultSet != null) {
            ResultSetMetaData md = resultSet.getMetaData();
            int num = md.getColumnCount();

            int rowCount = 0;
            int limit = sqlParameters.getLimit() == 0 ? QUERY_LIMIT : sqlParameters.getLimit();

            while (rowCount < limit && resultSet.next()) {
                ObjectNode mapOfColValues = JSONUtils.createObjectNode();
                for (int i = 1; i <= num; i++) {
                    mapOfColValues.set(md.getColumnLabel(i), JSONUtils.toJsonNode(resultSet.getObject(i)));
                }
                resultJSONArray.add(mapOfColValues);
                rowCount++;
            }
            int displayRows = sqlParameters.getDisplayRows() > 0 ? sqlParameters.getDisplayRows() : TaskConstants.DEFAULT_DISPLAY_ROWS;
            displayRows = Math.min(displayRows, resultJSONArray.size());
            logger.info("display sql result {} rows as follows:", displayRows);
            for (int i = 0; i < displayRows; i++) {
                String row = JSONUtils.toJsonString(resultJSONArray.get(i));
                logger.info("row {} : {}", i + 1, row);
            }
            if (resultSet.next()) {
                logger.info("sql result limit : {} exceeding results are filtered", limit);
                String log = String.format("sql result limit : %d exceeding results are filtered", limit);
                resultJSONArray.add(JSONUtils.toJsonNode(log));
            }
        }
        String result = JSONUtils.toJsonString(resultJSONArray);
        if (sqlParameters.getSendEmail() == null || sqlParameters.getSendEmail()) {
            sendAttachment(sqlParameters.getGroupId(), StringUtils.isNotEmpty(sqlParameters.getTitle())
                    ? sqlParameters.getTitle()
                    : taskExecutionContext.getTaskName() + " query result sets", result);
        }
        logger.debug("execute sql result : {}", result);
        return result;
    }

    /**
     * send alert as an attachment
     *
     * @param title title
     * @param content content
     */
    private void sendAttachment(int groupId, String title, String content) {
        setNeedAlert(Boolean.TRUE);
        TaskAlertInfo taskAlertInfo = new TaskAlertInfo();
        taskAlertInfo.setAlertGroupId(groupId);
        taskAlertInfo.setContent(content);
        taskAlertInfo.setTitle(title);
        setTaskAlertInfo(taskAlertInfo);
    }

    /**
     * pre sql
     *
     * @param connection connection
     * @param preStatementsBinds preStatementsBinds
     */
    private void preSql(Connection connection,
                        List<SqlBinds> preStatementsBinds) throws Exception {
        for (SqlBinds sqlBind : preStatementsBinds) {
            try (PreparedStatement pstmt = prepareStatementAndBind(connection, sqlBind)) {
                int result = pstmt.executeUpdate();
                logger.info("pre statement execute result: {}, for sql: {}", result, sqlBind.getSql());

            }
        }
    }

    /**
     * post sql
     *
     * @param connection connection
     * @param postStatementsBinds postStatementsBinds
     */
    private void postSql(Connection connection,
                         List<SqlBinds> postStatementsBinds) throws Exception {
        for (SqlBinds sqlBind : postStatementsBinds) {
            try (PreparedStatement pstmt = prepareStatementAndBind(connection, sqlBind)) {
                int result = pstmt.executeUpdate();
                logger.info("post statement execute result: {},for sql: {}", result, sqlBind.getSql());
            }
        }
    }

    /**
     * create temp function
     *
     * @param connection connection
     * @param createFuncs createFuncs
     */
    private void createTempFunction(Connection connection,
                                    List<String> createFuncs) throws Exception {
        try (Statement funcStmt = connection.createStatement()) {
            for (String createFunc : createFuncs) {
                logger.info("hive create function sql: {}", createFunc);
                funcStmt.execute(createFunc);
            }
        }
    }

    /**
     * close jdbc resource
     *
     * @param resultSet resultSet
     * @param pstmt pstmt
     * @param connection connection
     */
    private void close(ResultSet resultSet,
                       PreparedStatement pstmt,
                       Connection connection) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                logger.error("close result set error : {}", e.getMessage(), e);
            }
        }

        if (pstmt != null) {
            try {
                pstmt.close();
            } catch (SQLException e) {
                logger.error("close prepared statement error : {}", e.getMessage(), e);
            }
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.error("close connection error : {}", e.getMessage(), e);
            }
        }
    }

    /**
     * preparedStatement bind
     *
     * @param connection connection
     * @param sqlBinds sqlBinds
     * @return PreparedStatement
     * @throws Exception Exception
     */
    private PreparedStatement prepareStatementAndBind(Connection connection, SqlBinds sqlBinds) {
        // is the timeout set
        boolean timeoutFlag = taskExecutionContext.getTaskTimeoutStrategy() == TaskTimeoutStrategy.FAILED
                || taskExecutionContext.getTaskTimeoutStrategy() == TaskTimeoutStrategy.WARNFAILED;
        try {
            PreparedStatement stmt = connection.prepareStatement(sqlBinds.getSql());
            if (timeoutFlag) {
                stmt.setQueryTimeout(taskExecutionContext.getTaskTimeout());
            }
//            Map<Integer, Property> params = sqlBinds.getParamsMap();
//            System.out.println("------------------替换参数");
//            if (params != null) {
//                for (Map.Entry<Integer, Property> entry : params.entrySet()) {
//                    Property prop = entry.getValue();
//                    ParameterUtils.setInParameter(entry.getKey(), stmt, prop.getType(), prop.getValue());
//                    System.out.println("------------------替换参数2");
//                }
//            }
            logger.info("prepare statement replace sql : {} ", stmt);
            return stmt;
        } catch (Exception exception) {
            throw new TaskException("SQL task prepareStatementAndBind error", exception);
        }
    }

    /**
     * preparedStatement bind
     *
     * @param connection connection
     * @param sql sql
     * @param params params
     * @return PreparedStatement
     * @throws Exception Exception
     */
    private PreparedStatement prepareStatementAndBind(Connection connection, String sql, Map<Integer, Property> params) {
        // is the timeout set
        boolean timeoutFlag = taskExecutionContext.getTaskTimeoutStrategy() == TaskTimeoutStrategy.FAILED
            || taskExecutionContext.getTaskTimeoutStrategy() == TaskTimeoutStrategy.WARNFAILED;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            if (timeoutFlag) {
                stmt.setQueryTimeout(taskExecutionContext.getTaskTimeout());
            }
            if (params != null) {
                for (Map.Entry<Integer, Property> entry : params.entrySet()) {
                    Property prop = entry.getValue();
                    ParameterUtils.setInParameter(entry.getKey(), stmt, prop.getType(), prop.getValue());
                }
            }
            logger.info("prepare statement replace sql : {} ", stmt);
            return stmt;
        } catch (Exception exception) {
            throw new TaskException("SQL task prepareStatementAndBind error", exception);
        }
    }

    /**
     * print replace sql
     *
     * @param content content
     * @param formatSql format sql
     * @param rgex rgex
     * @param sqlParamsMap sql params map
     */
    private void printReplacedSql(String content, String formatSql, String rgex, Map<Integer, Property> sqlParamsMap) {
        //parameter print style
        logger.info("after replace sql , preparing : {}", formatSql);
        StringBuilder logPrint = new StringBuilder("replaced sql , parameters:");
        if (sqlParamsMap == null) {
            logger.info("printReplacedSql: sqlParamsMap is null.");
        } else {
            for (int i = 1; i <= sqlParamsMap.size(); i++) {
                logPrint.append(sqlParamsMap.get(i).getValue()).append("(").append(sqlParamsMap.get(i).getType()).append(")");
            }
        }
        logger.info("Sql Params are {}", logPrint);
    }

    /**
     * ready to execute SQL and parameter entity Map
     *
     * @return SqlBinds
     */
    private SqlBinds getSqlAndSqlParamsMap(String sql) {
        Map<Integer, Property> sqlParamsMap = new HashMap<>();
        StringBuilder sqlBuilder = new StringBuilder();

        // combining local and global parameters
        Map<String, Property> paramsMap = ParamUtils.convert(taskExecutionContext, getParameters());

        // spell SQL according to the final user-defined variable
        if (paramsMap == null) {
            sqlBuilder.append(sql);
            return new SqlBinds(sqlBuilder.toString(), sqlParamsMap);
        }

        if (StringUtils.isNotEmpty(sqlParameters.getTitle())) {
            String title = ParameterUtils.convertParameterPlaceholders(sqlParameters.getTitle(),
                    ParamUtils.convert(paramsMap));
            logger.info("SQL title : {}", title);
            sqlParameters.setTitle(title);
        }

        //new
        //replace variable TIME with $[YYYYmmddd...] in sql when history run job and batch complement job
        sql = ParameterUtils.replaceScheduleTime(sql, taskExecutionContext.getScheduleTime());
        // special characters need to be escaped, ${} needs to be escaped
        setSqlParamsMap(sql, rgex, sqlParamsMap, paramsMap,taskExecutionContext.getTaskInstanceId());
        //Replace the original value in sql ！{...} ，Does not participate in precompilation
        String rgexo = "\\!\\{(.*?)\\}";
        sql = replaceOriginalValue(sql, rgexo, paramsMap);
        // replace the ${} of the SQL statement with the Placeholder
//        String formatSql = sql.replaceAll(rgex, "?");
        // TODO 存在sql注入风险
        String formatSql = replaceOriginalValue(sql, DEFAULT_SQL_RGEX, paramsMap);
        sqlBuilder.append(formatSql);

        // print repalce sql
        printReplacedSql(sql, formatSql, rgex, sqlParamsMap);
        return new SqlBinds(sqlBuilder.toString(), sqlParamsMap);
    }

    private String replaceOriginalValue(String content, String rgex, Map<String, Property> sqlParamsMap) {
        StringBuffer sb = new StringBuffer();
        Pattern pattern = Pattern.compile(rgex);
        Matcher m = pattern.matcher(content);
        while (m.find()) {
            String paramName = m.group(1);
            if (sqlParamsMap.containsKey(paramName)) {
                String paramValue = sqlParamsMap.get(paramName).getValue();
                m.appendReplacement(sb, paramValue);
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * create function list
     *
     * @param udfFuncTenantCodeMap key is udf function,value is tenant code
     * @param logger logger
     * @return create function list
     */
    public static List<String> createFuncs(Map<UdfFuncRequest, String> udfFuncTenantCodeMap, String defaultFS, Logger logger) {

        if (MapUtils.isEmpty(udfFuncTenantCodeMap)) {
            logger.info("can't find udf function resource");
            return null;
        }
        List<String> funcList = new ArrayList<>();

        // build jar sql
        buildJarSql(funcList, udfFuncTenantCodeMap, defaultFS);

        // build temp function sql
        buildTempFuncSql(funcList, new ArrayList<>(udfFuncTenantCodeMap.keySet()));

        return funcList;
    }

    /**
     * build temp function sql
     *
     * @param sqls sql list
     * @param udfFuncRequests udf function list
     */
    private static void buildTempFuncSql(List<String> sqls, List<UdfFuncRequest> udfFuncRequests) {
        if (CollectionUtils.isNotEmpty(udfFuncRequests)) {
            for (UdfFuncRequest udfFuncRequest : udfFuncRequests) {
                sqls.add(MessageFormat
                        .format(CREATE_FUNCTION_FORMAT, udfFuncRequest.getFuncName(), udfFuncRequest.getClassName()));
            }
        }
    }

    /**
     * build jar sql
     * @param sqls                  sql list
     * @param udfFuncTenantCodeMap  key is udf function,value is tenant code
     */
    private static void buildJarSql(List<String> sqls, Map<UdfFuncRequest,String> udfFuncTenantCodeMap, String defaultFS) {
        String resourceFullName;
        Set<Entry<UdfFuncRequest, String>> entries = udfFuncTenantCodeMap.entrySet();
        for (Map.Entry<UdfFuncRequest, String> entry : entries) {
            String prefixPath = defaultFS.startsWith("file://") ? "file://" : defaultFS;
            String uploadPath = CommonUtils.getHdfsUdfDir(entry.getValue());
            resourceFullName = entry.getKey().getResourceName();
            resourceFullName = resourceFullName.startsWith("/") ? resourceFullName : String.format("/%s", resourceFullName);
            sqls.add(String.format("add jar %s%s%s", prefixPath, uploadPath, resourceFullName));
        }
    }

    /**
     * create command
     *
     * @return file name
     * @throws Exception exception
     */
    private String buildCommand(String rawScript) throws Exception {
        // generate scripts
        String fileName = String.format("%s/%s_node.%s",
            taskExecutionContext.getExecutePath(),
            taskExecutionContext.getTaskAppId(), OSUtils.isWindows() ? "bat" : "sh");

        Path path = new File(fileName).toPath();

        // dos2unix
        String script = rawScript.replaceAll("\\r\\n", "\n");

        logger.info("raw script : {}", script);
        logger.info("task execute path : {}", taskExecutionContext.getExecutePath());

        Set<PosixFilePermission> perms = PosixFilePermissions.fromString(RWXR_XR_X);
        FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);

        // 如果存在path，先删除再创建
        Files.deleteIfExists(path);

        if (OSUtils.isWindows()) {
            Files.createFile(path);
        } else {
            Files.createFile(path, attr);
        }

        Files.write(path, script.getBytes(), StandardOpenOption.APPEND);

        return fileName;
    }

    /**
     * create sqlFile
     *
     * @return file name
     * @throws Exception exception
     */
    private String getSqlFile(String sql) throws Exception {
        // generate scripts
        String fileName = String.format("%s/%s_sql.%s",
            taskExecutionContext.getExecutePath(),
            taskExecutionContext.getTaskAppId(), "sql");

        Path path = new File(fileName).toPath();

        // dos2unix
        String script = sql.replaceAll("\\r\\n", "\n");

        logger.info("sql script : {}", script);
        logger.info("sql execute path : {}", taskExecutionContext.getExecutePath());

        Set<PosixFilePermission> perms = PosixFilePermissions.fromString(RWXR_XR_X);
        FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);

        // 如果存在path，先删除再创建
        Files.deleteIfExists(path);

        if (OSUtils.isWindows()) {
            Files.createFile(path);
        } else {
            Files.createFile(path, attr);
        }

        Files.write(path, script.getBytes(), StandardOpenOption.APPEND);

        return fileName;
    }

    /**
     * 去掉右边的空白,并判断右边是否存在";",不存在则加上
     *
     * @return str
     * @throws Exception exception
     */
    private String trimRight(String s){
        if (s == null) {
            return "";
        }
        String whitespace = " \t\n\r";
        String str = s;
        if (whitespace.indexOf(str.charAt(str.length()-1)) != -1){
            int i = str.length() - 1;
            while (i >= 0 && whitespace.indexOf(str.charAt(i)) != -1){
                i--;
            }
            str = str.substring(0, i+1);
        }
        if (str.length() > 0 && !DEFAULT_SQL_SPLIT_CHAR.equalsIgnoreCase(str.substring(str.length() - 1))) {
            str += DEFAULT_SQL_SPLIT_CHAR;
        }
        return str;
    }
}
