package cn.test.a_helloworld;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.test.ActivitiUtil;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations ={"/application.xml"})
public class HelloWorld {
	
	private ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();

	/**部署流程定义*/
	@Test
	public void deploymentProcessDefinition(){
		
		Deployment deployment = processEngine.getRepositoryService()//与流程定义和部署对象相关的Service
						.createDeployment()//创建一个部署对象
						.name("helloworld入门程序")//添加部署的名称
						.addClasspathResource("diagrams/helloworld.bpmn")//从classpath的资源中加载，一次只能加载一个文件
						.addClasspathResource("diagrams/helloworld.png")//从classpath的资源中加载，一次只能加载一个文件
						.deploy();//完成部署
		System.out.println("部署ID："+deployment.getId());//1
		System.out.println("部署名称："+deployment.getName());//helloworld入门程序  
	}
	
	/**启动流程实例*/
	@Test
	public void startProcessInstance(){
		//流程定义的key
		String processDefinitionKey = "helloworld22222";
		Map<String,Object> var = new HashMap<>();
		var.put("userId", "张三2");
		ProcessInstance pi = processEngine.getRuntimeService()//与正在执行的流程实例和执行对象相关的Service
						.startProcessInstanceByKey(processDefinitionKey,var);//使用流程定义的key启动流程实例，key对应helloworld.bpmn文件中id的属性值，使用key值启动，默认是按照最新版本的流程定义启动
		System.out.println("流程实例ID:"+pi.getId());//流程实例ID    101
		System.out.println("流程定义ID:"+pi.getProcessDefinitionId());//流程定义ID   helloworld:1:4
	}


	@Autowired
	private ActivitiUtil activitiUtil;
	/**查询当前人的个人任务*/
	@Test
	public void findMyPersonalTask(){
		String assignee = "张三2";
		List<Task> list = processEngine.getTaskService()//与正在执行的任务管理相关的Service
						.createTaskQuery()//创建任务查询对象
						.taskAssignee(assignee)//指定个人任务查询，指定办理人
						.list();
		if(list!=null && list.size()>0){
			for(Task task:list){
				System.out.println("任务ID:"+task.getId());
				System.out.println("任务名称:"+task.getName());
				System.out.println("任务的创建时间:"+task.getCreateTime());
				System.out.println("任务的办理人:"+task.getAssignee());
				System.out.println("流程实例ID："+task.getProcessInstanceId());
				System.out.println("执行对象ID:"+task.getExecutionId());
				System.out.println("流程定义ID:"+task.getProcessDefinitionId());
				System.out.println("########################################################");

//				activitiUtil.setVariables(task.getId(),"uuu","asddsa");
//                List<HistoricVariableInstance> historyProcessVariables = activitiUtil.findProcessHistoryVariables(task.getProcessInstanceId());
//                for(HistoricVariableInstance h:historyProcessVariables){
//                    System.out.println(h.getVariableName()+"  "+h.getTaskId());
//                }
            }
		}
	}
	/**查询历史任务*/
	@Test
	public void findHistoryTask(){
//		String assignee = "张三3";
		String processInstanceId = "2501";
		List<HistoricTaskInstance> list = processEngine.getHistoryService()//与历史数据（历史表）相关的Service
						.createHistoricTaskInstanceQuery()//创建历史任务实例查询
						.processInstanceId(processInstanceId)//
//						.taskAssignee(assignee)

						.unfinished()//查找endtime为null的
						.list();
		if(list!=null && list.size()>0){
			for(HistoricTaskInstance hti:list){
				System.out.println(hti.getId()+"    "+hti.getName()+"    "+hti.getProcessInstanceId()+"   "+hti.getStartTime()+"   "+hti.getEndTime()+"   "+hti.getDurationInMillis()+"    "+hti.getAssignee());
				System.out.println("################################");
			}
		}
	}
	/**完成我的任务*/
	@Test
	public void completeMyPersonalTask(){
		//任务ID
		String taskId = "5005";
		processEngine.getTaskService()//与正在执行的任务管理相关的Service
					.complete(taskId,null);
		System.out.println("完成任务：任务ID："+taskId);
	}
	@Test
	public void test(){
        Map<String,Object> var = new HashMap<>();
        var.put("userId", "张三2");
        System.out.println(var);
    }
}
