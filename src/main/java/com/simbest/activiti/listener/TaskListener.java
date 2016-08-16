/**
 * 版权所有 © 北京晟壁科技有限公司 2008-2016。保留一切权利!
 */
package com.simbest.activiti.listener;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.simbest.activiti.listener.jobs.TaskCompletedJob;
import com.simbest.activiti.listener.jobs.TaskCreateJob;
import com.simbest.activiti.query.model.ActBusinessStatus;
import com.simbest.activiti.query.model.ActTaskAssigne;
import com.simbest.activiti.query.service.IActBusinessStatusService;
import com.simbest.activiti.query.service.IActTaskAssigneService;
import com.simbest.activiti.query.service.ICustomTaskService;
import com.simbest.cores.exceptions.Exceptions;
import com.simbest.cores.utils.DateUtil;
import com.simbest.cores.utils.SpringContextUtil;
import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用途：监听记录任务签收、任务分配、任务委托的人员
 * 作者: lishuyi
 * 时间: 2016-08-10  15:36
 */
@Component
public class TaskListener implements ActivitiEventListener {
    public transient final Log log = LogFactory.getLog(getClass());

    @Autowired
    private IActTaskAssigneService assigneService;

    @Autowired
    private IActBusinessStatusService statusService;

    @Autowired
    private SpringContextUtil context;

    @Autowired
    private ICustomTaskService taskService;

    int ret = 0;

    @Override
    public void onEvent(ActivitiEvent event) {
        ActBusinessStatus businessStatus = null;
        ActivitiEventType eventType = event.getType();
        ActivitiEntityEvent entityEvent = (ActivitiEntityEvent) event;
        TaskEntity task = (TaskEntity) entityEvent.getEntity();
        switch (eventType) {
            case TASK_CREATED:
                //更新业务全局状态表任务信息
                businessStatus = updateBusinessStatusTaskInfo(task);

                createTaskCallback(businessStatus);
                break;
            case TASK_ASSIGNED: //监听记录任务签收claim、任务分配setAssignee、任务委托的人员delegateTask，但不记录任务候选人/组addCandidateUser/Group，以便用于查询我的已办
                //1.将原有任务办理人待办撤销
                Map<String, Object> params = Maps.newHashMap();
                params.put("processDefinitionId", task.getProcessDefinitionId());
                params.put("processInstanceId", task.getProcessInstanceId());
                Collection<ActBusinessStatus> oldStatusList = statusService.getAll(params);
                if (oldStatusList.size() > 0) {
                    String assignee = assigneService.queryAssignee(oldStatusList.iterator().next().getTaskId());
                    if(StringUtils.isNotEmpty(assignee)){
                        TaskCompletedJob job = (TaskCompletedJob) context.getBeanByClass(TaskCompletedJob.class);
                        List<String> oldAssignee = Lists.newArrayList();
                        oldAssignee.add(assignee);
                        job.execution(businessStatus, oldAssignee);
                    }
                }
                //2.更新业务全局状态表任务信息，
                businessStatus = updateBusinessStatusTaskInfo(task);
                //3.推送新办理人待办
                TaskCreateJob job = (TaskCreateJob) context.getBeanByClass(TaskCreateJob.class);
                List<String> newAssignee = Lists.newArrayList();
                newAssignee.add(task.getAssignee());
                job.execution(businessStatus, newAssignee);

                //4.记录新办理人，以便用于查询我的已办
                ActTaskAssigne taskAssigne = new ActTaskAssigne();
                taskAssigne.setProcessDefinitionId(task.getProcessDefinitionId());
                taskAssigne.setProcessInstanceId(task.getProcessInstanceId());
                taskAssigne.setExecutionId(task.getExecutionId());
                taskAssigne.setTaskId(task.getId());
                taskAssigne.setOwner(task.getOwner());
                taskAssigne.setAssignee(task.getAssignee());
                taskAssigne.setHandleTime(DateUtil.getCurrent());
                ret = assigneService.create(taskAssigne);
                log.debug(ret);
                break;
            case TASK_COMPLETED:
                //更新业务全局状态表任务信息
                businessStatus = updateBusinessStatusTaskInfo(task);

                removeTaskCallback(businessStatus);
                break;
            default:
                log.debug("------------------------------------------------");
                log.debug("捕获到事件[Do Nothing]：" + eventType.name() + ", type=" + event.getType() + " ToString is :" + ToStringBuilder.reflectionToString(event));

        }
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }

    private ActBusinessStatus updateBusinessStatusTaskInfo(TaskEntity task) {
        int ret = 0;
        ActBusinessStatus businessStatus = null;
        Map<String, Object> params = Maps.newHashMap();
        params.put("processDefinitionId", task.getProcessDefinitionId());
        params.put("processInstanceId", task.getProcessInstanceId());
        Collection<ActBusinessStatus> businessStatusList = statusService.getAll(params);
        if (businessStatusList.size() > 0) {
            businessStatus = businessStatusList.iterator().next();
            businessStatus.setExecutionId(task.getExecutionId());
            businessStatus.setTaskId(task.getId());
            businessStatus.setTaskKey(task.getTaskDefinitionKey());
            businessStatus.setTaskName(task.getName());
            businessStatus.setTaskOwner(task.getOwner());
            businessStatus.setTaskAssignee(task.getAssignee());
            businessStatus.setDelegationState(task.getDelegationState());
            //businessStatus.setTaskStartTime(task.getCreateTime());
            businessStatus.setTaskStartTime(DateUtil.getCurrent());
            ret = statusService.update(businessStatus);
            log.debug(ret);
        }
        return ret > 0 ? businessStatus : null;
    }

    private void createTaskCallback(ActBusinessStatus businessStatus) {
        //调用外部创建待办回调
        try {
            TaskCreateJob job = (TaskCreateJob) context.getBeanByClass(TaskCreateJob.class);
            List<String> toDoUsers = assigneService.queryToDoUser(businessStatus.getTaskId());
            job.execution(businessStatus, toDoUsers);
        } catch (Exception e) {
            log.error("@@@@Error:" + Exceptions.getStackTraceAsString(e));
        }
    }

    private void removeTaskCallback(ActBusinessStatus businessStatus) {
        //调用外部取消待办回调
        try {
            TaskCompletedJob job = (TaskCompletedJob) context.getBeanByClass(TaskCompletedJob.class);
            List<String> toDoUsers = assigneService.queryToDoUser(businessStatus.getTaskId());
            job.execution(businessStatus, toDoUsers);
        } catch (Exception e) {
            log.error("@@@@Error:" + Exceptions.getStackTraceAsString(e));
        }
    }
}