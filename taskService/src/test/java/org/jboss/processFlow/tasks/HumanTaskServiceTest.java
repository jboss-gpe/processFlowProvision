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
            .addPackages(true, "antlr")
            .addPackages(true, "com/google")
            .addPackages(true, "com/sun/codemodel")
            .addPackages(true, "com/thoughtworks/xstream")
            .addPackages(true, "javax/annotation/processing")
            .addPackages(true, "org/antlr")
            .addPackages(true, "org/aopalliance")
            .addPackages(true, "org/apache/commons/pool")
            .addPackages(true, "org/drools")
            .addPackages(true, "org/eclipse/jdt")
            .addPackages(true, "org/jboss/bpm")
            .addPackages(true, "org/jboss/processFlow")
            .addPackages(true, "org/jbpm")
            .addPackages(true, "org/mvel2")
            .addPackages(true, "org/osgi")
            .addAsResource("META-INF/pfp-Taskorm.xml", "META-INF/pfp-Taskorm.xml")
            .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
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
