package org.apache.dolphinscheduler.api.dto;

import java.util.ArrayList;
import java.util.List;

public class ProcessDefinitionNode {

    long workFlowCode;

    List<ProcessDefinitionNode> nextNodeList = new ArrayList<>();

    public ProcessDefinitionNode(long workFlowCode) {
        this.workFlowCode = workFlowCode;
    }

    public long getWorkFlowCode() {
        return workFlowCode;
    }

    public void setWorkFlowCode(long workFlowCode) {
        this.workFlowCode = workFlowCode;
    }

    public List<ProcessDefinitionNode> getNextNodeList() {
        return nextNodeList;
    }

    public void setNextNodeList(List<ProcessDefinitionNode> nextNodeList) {
        this.nextNodeList = nextNodeList;
    }

    @Override
    public boolean equals(Object obj) {
        return this.getWorkFlowCode() == ((ProcessDefinitionNode)obj).getWorkFlowCode();
    }

    @Override
    public String toString() {
        return String.valueOf(this.getWorkFlowCode());
    }

}
