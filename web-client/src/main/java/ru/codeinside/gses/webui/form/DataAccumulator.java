package ru.codeinside.gses.webui.form;

import ru.codeinside.gses.activiti.forms.Signatures;
import ru.codeinside.gses.activiti.forms.api.definitions.PropertyTree;
import ru.codeinside.gws.api.ClientRequest;
import ru.codeinside.gws.api.Enclosure;
import ru.codeinside.gws.api.ServerResponse;

import javax.xml.soap.SOAPMessage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Накапливает данные в результат выполнения шагов мастера. Используется пока как черновое решение.
 */
public class DataAccumulator implements Serializable {
  private boolean needOv;
  private String serviceName;
  private String requestType;
  private String taskId;

  private ClientRequest clientRequest;
  private ServerResponse serverResponse;
  private PropertyTree propertyTree;
  private List<FormField> formFields;
  private Map<Enclosure, String[]> usedEnclosures;
  private List<TmpAttachment> attachments;
  private Signatures spSignature;
  private Signatures ovSignatures;

  //List нужен, чтобы можно было меня значение, сохраняя ссылку. В списке всегда один элемент
  private List<Long> requestId;
  private List<Long> responseId;
  private List<SOAPMessage> soapMessage;

  public ClientRequest getClientRequest() {
    return clientRequest;
  }

  public void setClientRequest(ClientRequest clientRequest) {
    this.clientRequest = clientRequest;
  }

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public Signatures getSpSignature() {
    return spSignature;
  }

  public void setSpSignature(Signatures spSignature) {
    this.spSignature = spSignature;
  }

  public Signatures getOvSignatures() {
    return ovSignatures;
  }

  public void setOvSignatures(Signatures ovSignatures) {
    this.ovSignatures = ovSignatures;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public List<Long> getRequestId() {
    return requestId;
  }

  //всегда хранится только одно значение. Нужно, что бы значение было mutable
  public void setRequestId(Long requestId) {
    if (this.requestId == null) {
      this.requestId = new ArrayList<Long>();
      this.requestId.add(requestId);
    }
    this.requestId.set(0, requestId);
  }

  public List<Long> getResponseId() {
    return responseId;
  }

  //всегда хранится только одно значение. Нужно, что бы значение было mutable
  public void setResponseId(Long responseId) {
    if (this.responseId == null) {
      this.responseId = new ArrayList<Long>();
      this.responseId.add(responseId);
    }
    this.responseId.set(0, responseId);
  }

  public boolean isNeedOv() {
    return needOv;
  }

  public void setNeedOv(boolean needOv) {
    this.needOv = needOv;
  }

  public List<SOAPMessage> getSoapMessage() {
    return soapMessage;
  }

  public void setSoapMessage(SOAPMessage soapMessage) {
    if (this.soapMessage == null) {
      this.soapMessage = new ArrayList<SOAPMessage>();
      this.soapMessage.add(soapMessage);
    }
    this.soapMessage.set(0, soapMessage);
  }

  public String getRequestType() {
    return requestType;
  }

  public void setRequestType(String requestType) {
    this.requestType = requestType;
  }

  public ServerResponse getServerResponse() {
    return serverResponse;
  }

  public void setServerResponse(ServerResponse serverResponse) {
    this.serverResponse = serverResponse;
  }

  public PropertyTree getPropertyTree() {
    return propertyTree;
  }

  public void setPropertyTree(PropertyTree propertyTree) {
    this.propertyTree = propertyTree;
  }

  public List<FormField> getFormFields() {
    return formFields;
  }

  public void setFormFields(List<FormField> formFields) {
    this.formFields = formFields;
  }

  public Map<Enclosure, String[]> getUsedEnclosures() {
    return usedEnclosures;
  }

  public void setUsedEnclosures(Map<Enclosure, String[]> usedEnclosures) {
    this.usedEnclosures = usedEnclosures;
  }

  public List<TmpAttachment> getAttachments() {
    return attachments;
  }

  public TmpAttachment getAttachment(String name) {
    if (name == null) {
      return null;
    }

    for (TmpAttachment attachment : attachments) {
      if (name.equals(attachment.getName())) {
        return attachment;
      }
    }

    return null;
  }

  public void setAttachment(TmpAttachment attachment) {
    if (this.attachments == null) {
      this.attachments = new ArrayList<TmpAttachment>();
      attachments.add(attachment);
    } else {
      this.attachments.add(attachment);
    }
  }
}
