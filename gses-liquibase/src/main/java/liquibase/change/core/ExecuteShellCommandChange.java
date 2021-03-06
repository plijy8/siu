package liquibase.change.core;

import liquibase.change.AbstractChange;
import liquibase.change.ChangeMetaData;
import liquibase.database.Database;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.exception.ValidationErrors;
import liquibase.exception.Warnings;
import liquibase.executor.Executor;
import liquibase.executor.ExecutorService;
import liquibase.executor.LoggingExecutor;
import liquibase.sql.Sql;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.CommentStatement;
import liquibase.statement.core.RuntimeStatement;
import liquibase.util.StreamUtil;
import liquibase.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Executes a given shell executable.
 */
public class ExecuteShellCommandChange extends AbstractChange {

    private String executable;
    private List<String> os;
    private List<String> args = new ArrayList<String>();
    Logger logger = Logger.getLogger(getClass().getName());

    public ExecuteShellCommandChange() {
        super("executeCommand", "Execute Shell Command", ChangeMetaData.PRIORITY_DEFAULT);
    }


    public String getExecutable() {
        return executable;
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }

    public void addArg(String arg) {
        this.args.add(arg);
    }


    public void setOs(String os) {
        this.os = StringUtils.splitAndTrim(os, ",");
    }

    public List<String> getOs() {
        return os;
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }


    @Override
    public Warnings warn(Database database) {
        return new Warnings();
    }

    public SqlStatement[] generateStatements(final Database database) {
        boolean shouldRun = true;
        if (os != null && os.size() > 0) {
            String currentOS = System.getProperty("os.name");
            if (!os.contains(currentOS)) {
                shouldRun = false;
                logger.info("Not executing on os "+currentOS+" when "+os+" was specified");
            }
        }

    	// check if running under not-executed mode (logging output)
        boolean nonExecutedMode = false;
        Executor executor = ExecutorService.getInstance().getExecutor(database);
        if (executor instanceof LoggingExecutor) {
        	nonExecutedMode = true;
        }
        
        if (shouldRun && !nonExecutedMode) {


            return new SqlStatement[]{new RuntimeStatement() {

                @Override
                public Sql[] generate(Database database) {
                    List<String> commandArray = new ArrayList<String>();
                    commandArray.add(executable);
                    commandArray.addAll(args);

                    try {
                        ProcessBuilder pb = new ProcessBuilder(commandArray);
                        pb.redirectErrorStream(true);
                        Process p = pb.start();
                        int returnCode = 0;
                        try {
                            returnCode = p.waitFor();
                        } catch (InterruptedException e) {
                            ;
                        }

                        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
                        ByteArrayOutputStream inputStream = new ByteArrayOutputStream();
                        StreamUtil.copy(p.getErrorStream(), errorStream);
                        StreamUtil.copy(p.getInputStream(), inputStream);

                        logger.severe(errorStream.toString());
                        logger.info(inputStream.toString());

                        if (returnCode != 0) {
                            throw new RuntimeException(getCommandString() + " returned an code of " + returnCode);
                        }
                    } catch (IOException e) {
                        throw new UnexpectedLiquibaseException("Error executing command: " + e);
                    }

                    return null;
                }
            }};
        }
        
        if (nonExecutedMode) {
        	return new SqlStatement[] {
        			new CommentStatement(getCommandString())
        	};
        }
        
        return new SqlStatement[0];
    }

    public String getConfirmationMessage() {
        return "Shell command '" + getCommandString() + "' executed";
    }

    private String getCommandString() {
        return executable + " " + StringUtils.join(args, " ");
    }
}
