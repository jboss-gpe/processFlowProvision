package org.jboss.processFlow.tasks;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jbpm.task.Task;
import org.jbpm.task.service.ContentData;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class HumanTaskServiceTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
            .addClass(org.apache.log4j.Logger.class)
            .addClass(org.jboss.processFlow.tasks.HumanTaskService.class)
            .addClass(org.jboss.processFlow.PFPBaseService.class)
            .addClass(org.jboss.processFlow.knowledgeService.IBaseKnowledgeSession.class)
            .addClass(org.jboss.processFlow.knowledgeService.MockKnowledgeSessionService.class)
            //.addPackages(true, "antlr")
            //.addAsResource("META-INF/ejb-jar.xml", "META-INF/ejb-jar.xml")
            .addAsResource("META-INF/pfp-Taskorm.xml", "META-INF/pfp-Taskorm.xml")
            .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
            .addAsResource("META-INF/jboss-deployment-structure.xml", "META-INF/jboss-deployment-structure.xml")
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    HumanTaskService htService;

    @Test
    public void addTaskTest() {
        Task taskObj = new Task();
        ContentData inboundTaskVars = null;
        htService.addTask(taskObj, inboundTaskVars);
    }

}
