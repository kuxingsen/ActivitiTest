package cn.test;

import org.activiti.engine.*;
import org.activiti.engine.history.*;
import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.activiti.engine.impl.persistence.entity.UserEntity;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipInputStream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations ={"/application.xml"})
@Service
public class ActivitiUtil{
    private Logger logger = LoggerFactory.getLogger(ActivitiUtil.class);
//    ApplicationContext applicationContext = new ClassPathXmlApplicationContext("application.xml");

    @Autowired
    private RepositoryService repositoryService;//仓库服务类.管理流程定义//与流程定义和部署对象相关的Service
    @Autowired
    private RuntimeService runtimeService;//流程执行服务类.执行管理，包括启动、推进、删除流程实例等操作
    @Autowired
    private TaskService taskService;//任务服务类.任务管理
    @Autowired
    private HistoryService historyService;//查询历史信息的类.历史管理(执行完的数据的管理)
    @Autowired
    private FormService formService;//一个可选服务，任务表单管理
    @Autowired
    private IdentityService identityService;//组织机构管理

    /**
     * 部署流程定义（从zip）
     * @param name 部署的名错
     * @param zipResource 指定zip格式的文件
     * @return 部署对象
     */
    public Deployment createDeploymentByZip(String name, String zipResource){
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(zipResource);
        ZipInputStream zipInputStream = new ZipInputStream(in);
        Deployment deployment =  repositoryService.createDeployment()//创建一个部署对象
                .name(name)//添加部署的名称
                .addZipInputStream(zipInputStream)//指定zip格式的文件完成部署
                .deploy();//返回部署对象
        logger.info("部署流程。部署名称：{} ；资源：{} ；新部署的id：{}",name,zipResource,deployment.getId());
        return deployment;
    }


    /**
     * 查询所有最新版本的流程定义
     * @return 所有最新版本的流程定义
     */
    public List<ProcessDefinition> findLastVersionProcessDefinition(){
        List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery()
                .orderByProcessDefinitionVersion().asc()//使用流程定义的版本升序排列
                .list();
        //当map集合key值相同的情况下，后一次的值将替换前一次的值
        Map<String, ProcessDefinition> map = new LinkedHashMap<>();
        if(list!=null && list.size()>0){
            for(ProcessDefinition pd:list){
                map.put(pd.getKey(), pd);
            }
            logger.info("查询所有最新版本的流程定义。所有的最新版本：{}",map.values());
            return new ArrayList<>(map.values());
        }
        return null;
    }

    /**
     * 指定key的流程定义的最新版本
     * @param processDefinitionKey 流程定义的key
     * @return 最新版本的流程定义
     */
    public ProcessDefinition findLastVersionProcessDefinition(String processDefinitionKey){
        List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery()//
                .processDefinitionKey(processDefinitionKey)
                .orderByProcessDefinitionVersion().desc()//使用流程定义的版本升序排列
                .list();
        if(list != null && list.size()>0){
            logger.info("获取流程定义的最新版本：key：{} ; 最新版本{}",processDefinitionKey,list.get(0));
            return list.get(0);
        }
        logger.error("获取流程定义的最新版本失败.key:{}",processDefinitionKey);
        return null;
    }

    /**
     * 删除流程定义（删除key相同的所有不同版本的流程定义）
     * @param processDefinitionKey 流程定义的key
     * @param cascade 是否级联（true:不管流程是否启动，都能可以删除;false:只能删除没有启动的流程，如果流程启动，就会抛出异常)
     */
    public void deleteProcessDefinitionByKey(String processDefinitionKey,boolean cascade){
        logger.info("删除流程定义。程定义的key：{} ；是否级联删除：{}",processDefinitionKey,cascade?"级联":"不级联");
        //先使用流程定义的key查询流程定义，查询出所有的版本
        List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery()//
                .processDefinitionKey(processDefinitionKey)//使用流程定义的key查询
                .list();
        //遍历，获取每个流程定义的部署ID
        if(list!=null && list.size()>0){
            for(ProcessDefinition pd:list){
                //获取部署ID
                String deploymentId = pd.getDeploymentId();
                deleteProcessDefinition(deploymentId,cascade);
            }
        }
    }

    /**
     * 删除流程定义
     * @param deploymentId 部署id
     * @param cascade 是否级联删除（true:不管流程是否启动，都能可以删除;false:只能删除没有启动的流程，如果流程启动，就会抛出异常)
     */
    public void deleteProcessDefinition(String deploymentId,boolean cascade){
        logger.info("删除流程定义。部署id：{} ；是否级联删除：{}",deploymentId,cascade?"级联":"不级联");
        repositoryService.deleteDeployment(deploymentId, cascade);
    }
    /**
     * 查看流程图，将生成图片放到文件夹下
     * @param deploymentId 部署id
     * @param filePath 文件夹路径
     */
    public void viewPic(String deploymentId,String filePath){
        //获取图片资源名称
        List<String> list = repositoryService.getDeploymentResourceNames(deploymentId);
        //定义图片资源的名称
        String resourceName = "";
        if(list!=null && list.size()>0){
            for(String name:list){
                if(name.endsWith(".png")){
                    resourceName = name;
                }
            }
        }
        //获取图片的输入流
        InputStream in = repositoryService.getResourceAsStream(deploymentId, resourceName);
        logger.info("生成流程图。部署id：{} ；存放位置：{} ;文件名：{}",deploymentId,filePath,resourceName);
        File file = new File(filePath,resourceName);
        try{
            FileUtils.copyInputStreamToFile(in, file);
        }catch(IOException e){
            logger.error("下载图片失败");
        }
    }

    /**
     * 启动流程实例
     * @param processDefinitionKey 流程定义的key
     * @param vars 流程实例参数
     * @return 流程实例
     */
    public ProcessInstance startProcessInstanceByKey(String processDefinitionKey,Map<String,Object> vars){
        //使用流程定义的key启动流程实例，key对应.bpmn文件中id的属性值，使用key值启动，默认是按照最新版本的流程定义启动
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, vars);
        logger.info("启动流程实例。key：{} ；vars：{} ;实例id：{}",processDefinitionKey,vars,processInstance.getProcessDefinitionId());
        return processInstance;
    }

    /**
     * 查询历史流程实例
     * @param processInstanceId 流程实例id
     * @return 流程实例
     */
    public HistoricProcessInstance findHistoryProcessInstance(String processInstanceId){
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()//创建历史流程实例查询
                .processInstanceId(processInstanceId)//使用流程实例ID查询
                .singleResult();
        logger.info("查询历史流程实例。processInstanceId：{} ;流程实例：{}",processInstanceId,historicProcessInstance);
        return historicProcessInstance;
    }
    /**
     * 查询当前人未完成的个人任务
     * @param assignee 查询对象
     * @return 未完成个人任务
     */
    public List<Task> findMyPersonalTask(String assignee){
        List<Task> taskList =  taskService.createTaskQuery()//创建任务查询对象
                .taskAssignee(assignee)//指定个人任务查询，指定办理人
                .list();
        logger.info("查询当前人未完成的个人任务.查询对象：{} 。个人任务：{}",assignee,taskList);
        return taskList;
    }

    /**
     * 查询个人的历史任务
     * @param assignee 查询对象
     * @return 所有的历史任务
     */
    public List<HistoricTaskInstance> findPersonalHistoryTask(String assignee){
        List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery()//创建历史任务实例查询
                .taskAssignee(assignee)
                .list();
        logger.info("查询个人的历史任务.办理人：{} 。历史任务：{}",assignee,list);
        return list;
    }

    /**
     * 查询流程的历史任务
     * @param processInstanceId 流程实例
     * @return 历史任务实例
     */
    public List<HistoricTaskInstance> findProcessHistoryTask(String processInstanceId){
        List<HistoricTaskInstance> list = createHistoricTaskInstanceQueryByProcessInstanceId(processInstanceId)
                .list();
        logger.info("查询流程的历史任务.流程实例id：{} ；历史任务实例：{}",processInstanceId,list);
        return list;
    }

    /**
     * 查询流程的未完成任务
     * @param processInstanceId 流程实例
     * @return 未完成任务实例
     */
    public List<HistoricTaskInstance> findProcessUnfinishedTask(String processInstanceId){
        List<HistoricTaskInstance> list = createHistoricTaskInstanceQueryByProcessInstanceId(processInstanceId)
                .unfinished()
                .list();
        logger.info("查询流程的未完成任务.流程实例id：{} ；未完成任务实例：{}",processInstanceId,list);
        return list;
    }

    /**
     * 获取历史任务查询对象（私有）
     * @param pIId 流程实例
     * @return 历史任务查询对象
     */
    private HistoricTaskInstanceQuery createHistoricTaskInstanceQueryByProcessInstanceId(String pIId){
        return  historyService.createHistoricTaskInstanceQuery()//创建历史任务实例查询
                .processInstanceId(pIId);//
    }

    /**
     * 完成指定任务
     * @param taskId 任务id
     */
    public void completePersonalTask(String taskId){
        completePersonalTask(taskId,null);
    }
    /**
     * 完成指定任务
     * @param taskId 任务id
     * @param vars 变量
     */
    public void completePersonalTask(String taskId,Map<String,Object> vars){
        logger.info("完成个人任务。任务id：{} 。变量:{}",taskId,vars);
        taskService.complete(taskId,vars);
    }

    /**
     * 设置流程变量
     * @param taskId 任务
     * @param varName 变量名
     * @param varValue 变量值
     */
    @Test
    public void setTaskVariables(String taskId,String varName,Object varValue){
        logger.info("设置任务流程变量。任务id：{} ；变量名：{} ；变量值：{}",taskId,varName,varValue);
        taskService.setVariable(taskId, varName, varValue);
    }
    /**
     * 获取流程变量
     * @param taskId 任务
     * @param varName 变量名
     * @return 变量值
     */
    public Object getTaskVariables(String taskId,String varName){
        Object variable = taskService.getVariable(taskId, varName);
        logger.info("获取任务流程变量。任务id：{} ；变量名：{} ；变量值：{}",taskId,varName,variable);
        return variable;
    }

    /**
     * 查询流程的历史变量表
     * @param processInstanceId 流程实例
     * @return 历史变量表
     */
    public List<HistoricVariableInstance> findProcessHistoryVariables(String processInstanceId) {
        List<HistoricVariableInstance> list = historyService.createHistoricVariableInstanceQuery()//创建一个历史的流程变量查询对象
                .processInstanceId(processInstanceId)
                .list();
        logger.info("查询流程的历史变量表。流程实例id：{} ；变量表：{}",processInstanceId,list);
        return list;
    }
    /**
     * 设置流程变量
     * @param executionId 执行对象id
     * @param varName 变量名
     * @param varValue 变量值
     */
    public void setExecutionVariables(String executionId,String varName,Object varValue){
        logger.info("设置执行对象的流程变量。执行对象id：{} ；变量名：{} ；变量值：{}",executionId,varName,varValue);
        runtimeService.setVariable(executionId, varName, varValue);
    }
    /**
     * 获取流程变量
     * @param executionId 任务
     * @param varName 变量名
     * @return 变量值
     */
    public Object getExecutionVariables(String executionId,String varName){
        Object variable = runtimeService.getVariable(executionId, varName);
        logger.info("获取流程变量。执行对象id：{}； 变量名：{} ；变量值：{}",executionId,varName,variable);
        return variable;
    }
    /**
     * 向后执行一步，如果流程处于等待状态，使得流程继续执行
     * @param executionId 执行对象id
     */
    public void signal(String executionId){
        logger.info("流程继续执行。执行对象id:{}",executionId);
        runtimeService.signal(executionId);
    }

    /**
     * 查询流程实例的执行对象
     * @param processInstanceId 流程实例id
     * @param activityId 活动对象id
     * @return 执行对象
     */
    public Execution getExecutionByActivityId(String processInstanceId,String activityId){
        Execution execution = runtimeService.createExecutionQuery()//创建执行对象查询
                .processInstanceId(processInstanceId)//使用流程实例ID查询
                .activityId(activityId)//当前活动的id，对应.bpmn文件中的活动节点id的属性值
                .singleResult();
        logger.info("查询流程实例的执行对象。流程实例id：{} ；活动对象id：{} ；执行对象：{}",processInstanceId,activityId,execution);
        return execution;
    }

    /**
     * 可以分配个人任务从一个人到另一个人（认领任务）
     * @param taskId 任务ID
     * @param assignee 指定的办理人
     */
    public void setAssigneeTask(String taskId,String assignee){
        logger.info("分配个人任务。任务id：{} ；指定办理人：{}",taskId,assignee);
        taskService.setAssignee(taskId, assignee);
    }

    /**
     * 查询当前人的组任务(作为候选人)
     * @param candidateUser 办理人
     * @return 组任务列表
     */
    public List<Task> findMyGroupTask(String candidateUser){
        List<Task> list = taskService.createTaskQuery()//创建任务查询对象
                .taskCandidateUser(candidateUser)//组任务的办理人查询
                .list();
        logger.info("查询当前人的组任务.办理人:{} ;组任务列表：{}",candidateUser,list);
        return list;//返回列表
    }

    /**
     * 查询任务办理人表(包括候选人和受托人)
     * @param taskId 任务ID
     * @return 任务办理人表
     */
    public List<HistoricIdentityLink> getIdentityLinksForTask(String taskId){
        List<HistoricIdentityLink> historicIdentityLinksForTask = historyService.getHistoricIdentityLinksForTask(taskId);
        logger.info("查询任务办理人表(包括候选人和受托人)。任务id：{} ；任务办理人：{}",taskId,historicIdentityLinksForTask);
        return historicIdentityLinksForTask;
    }
    /**
     * 查询任务办理人表(包括候选人和受托人)
     * @param processInstanceId 流程实例ID
     * @return 任务办理人表
     */
    public List<HistoricIdentityLink> findHistoryPersonTask(String processInstanceId){
        List<HistoricIdentityLink> historicIdentityLinksForProcessInstance = historyService.getHistoricIdentityLinksForProcessInstance(processInstanceId);
        logger.info("查询任务办理人表(包括候选人和受托人)。流程实例id：{} ；任务办理人：{}",processInstanceId,historicIdentityLinksForProcessInstance);
        return historicIdentityLinksForProcessInstance;
    }

    /**
     * 拾取任务，将组任务分给个人任务，指定任务的办理人字段
     * @param taskId 任务ID
     * @param userId 分配的个人任务（可以是组任务中的成员，也可以是非组任务的成员）
     */
    public void claim(String taskId,String userId){
        logger.info("拾取任务.任务id：{} ；办理人：{}",taskId,userId);
        taskService.claim(taskId, userId);
    }
    /**
     * 将个人任务回退到组任务，前提，之前一定是个组任务
     * @param taskId 任务ID
     */
    public void setAssignee(String taskId){
        logger.info("将个人任务回退到组任务.任务id：{}",taskId);
        taskService.setAssignee(taskId, null);
    }

    /**向组任务中添加成员*/
    public void addGroupUser(String taskId,String userId){
        logger.info("向组任务中添加成员。任务id：{} ；办理人：{}",taskId,userId);
        taskService.addCandidateUser(taskId, userId);
    }

    /**从组任务中删除成员*/
    public void deleteGroupUser(String taskId,String userId){
        logger.info("向组任务中删除成员。任务id：{} ；办理人：{}",taskId,userId);
        taskService.deleteCandidateUser(taskId, userId);
    }

    /**
     * 添加用户角色组
     * @param groups 角色
     * @param users 用户
     * @param memberShips 关联关系
     */
    public void createMembership(List<GroupEntity> groups, List<UserEntity> users, Map<String,String> memberShips){
        logger.info("添加用户角色组.角色：{} ；用户：{} ；关系：{}",groups,users,memberShips);
        for(GroupEntity group:groups){
            identityService.saveGroup(group);
        }
        for(UserEntity user:users){
            identityService.saveUser(user);
        }
//        Set<String> keys = memberShips.keySet();
//        for(String key : keys) {
//            identityService.createMembership(key, memberShips.get(key));
//        }
        for(Map.Entry<String,String> e:memberShips.entrySet()){
            identityService.createMembership(e.getKey(),e.getValue());
        }
    }
}
