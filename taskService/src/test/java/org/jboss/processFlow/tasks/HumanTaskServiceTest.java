package org.jboss.processFlow.tasks;

import org.jbpm.task.Task;
import org.jbpm.task.service.ContentData;

import static org.junit.Assert.assertEquals;
import javax.ejb.EJB;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.Test;
import org.junit.runner.RunWith;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class HumanTaskServiceTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
            .addClass(HumanTaskService.class)
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
