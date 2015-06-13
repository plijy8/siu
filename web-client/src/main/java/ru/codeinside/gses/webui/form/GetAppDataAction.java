package ru.codeinside.gses.webui.form;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.ServiceImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.osgi.framework.ServiceReference;
import ru.codeinside.adm.AdminServiceProvider;
import ru.codeinside.gses.beans.ActivitiExchangeContext;
import ru.codeinside.gses.beans.StartFormExchangeContext;
import ru.codeinside.gses.service.F2;
import ru.codeinside.gses.service.Fn;
import ru.codeinside.gses.webui.Flash;
import ru.codeinside.gses.webui.osgi.Activator;
import ru.codeinside.gses.webui.wizard.ResultTransition;
import ru.codeinside.gses.webui.wizard.TransitionAction;
import ru.codeinside.gws.api.Client;
import ru.codeinside.gws.api.ClientRequest;
import ru.codeinside.gws.api.ExchangeContext;

import java.util.List;

public class GetAppDataAction implements TransitionAction {

  private final String serviceName;
  private final DataAccumulator dataAccumulator;

  GetAppDataAction(final DataAccumulator dataAccumulator) {
    this.serviceName = dataAccumulator.getServiceName();
    this.dataAccumulator = dataAccumulator;
  }

  /**
   * Выполнить действие перехода.
   */
  @Override
  public ResultTransition doIt() throws IllegalStateException {
    final ServiceReference reference = FormSeqUtils.getServiceReference(serviceName, Client.class);
    final Client client = FormSeqUtils.getService(reference, Client.class);
    dataAccumulator.setClient(client);

    final ClientRequest request;
    try {
      request = Fn.withEngine(new GetClientRequest(), Flash.login(), dataAccumulator);
    } finally {
      Activator.getContext().ungetService(reference);
    }
    dataAccumulator.setClientRequest(request);

    if (!dataAccumulator.isNeedOv()) {
      //чтобы были ссылки
      dataAccumulator.setSoapMessage(null);
      dataAccumulator.setRequestId(0L);
    }

    return new ResultTransition(request.appData);
  }

  final static class GetClientRequest implements F2<ClientRequest, String, DataAccumulator> {
    @Override
    public ClientRequest apply(ProcessEngine engine, String login, DataAccumulator dataAccumulator) {
      CommandExecutor commandExecutor = ((ServiceImpl) engine.getFormService()).getCommandExecutor();
      return (ClientRequest) commandExecutor.execute(new GetClientRequestCmd(
          dataAccumulator.getTaskId(),
          dataAccumulator.getClient(),
          dataAccumulator.getFormFields()
      ));
    }
  }

  final private static class GetClientRequestCmd implements Command {
    private final String taskId;
    private final Client client;
    private final List<FormField> formFields;

    GetClientRequestCmd(String taskId, Client client, List<FormField> formFields) {
      this.taskId = taskId;
      this.client = client;
      this.formFields = formFields;
    }

    @Override
    public Object execute(CommandContext commandContext) {
      ExchangeContext context;

      if (taskId != null) {
        final String processInstanceId = AdminServiceProvider.get().getBidByTask(taskId).getProcessInstanceId();

        DelegateExecution execution = Context.getCommandContext()
            .getExecutionManager()
            .findExecutionById(processInstanceId);
        context = new ActivitiExchangeContext(execution);
      } else {
        context = new StartFormExchangeContext();
        setContextVariables(context);
      }

      return client.createClientRequest(context);
    }

    private void setContextVariables(ExchangeContext context) {
      for (FormField field : formFields) {
        context.setVariable(field.getPropId(), field.getValue());
      }
    }
  }
}
