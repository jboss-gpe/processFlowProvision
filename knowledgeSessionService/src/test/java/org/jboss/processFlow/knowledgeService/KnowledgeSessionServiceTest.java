package org.jboss.processFlow.knowledgeService;

import static org.junit.Assert.assertEquals;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.Test;
import org.junit.runner.RunWith;
import javax.ejb.EJB;

//mvn clean -DfailIfNoTests=false -Dtest=org.jboss.processFlow.knowledgeService.KnowledgeSessionServiceTest test
@RunWith(Arquillian.class)
public class KnowledgeSessionServiceTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
            .addClass(org.apache.log4j.Logger.class)
            .addClass(org.jboss.processFlow.PFPBaseService.class)
            .addClass(org.jboss.as.osgi.deployment.OSGiDeploymentAttachment.class)
            .addClass(org.jboss.osgi.deployment.deployer.Deployment.class)
            .addClass(org.jboss.as.server.deployment.DeploymentUnit.class)
            .addClass(org.jboss.osgi.spi.Attachments.class)
            .addClass(org.jboss.osgi.vfs.VirtualFile.class)
            .addClass(org.jboss.as.server.deployment.AttachmentKey.class)
            .addPackages(false, "org.jboss.processFlow.knowledgeService")
            //.addAsResource("META-INF/ejb-jar.xml", "META-INF/ejb-jar.xml")
            .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
            .addAsResource("META-INF/JBPMorm-JPA2.xml", "META-INF/JBPorm-JPA2.xml")
            .addAsResource("META-INF/jboss-deployment-structure.xml", "META-INF/jboss-deployment-structure.xml")

            // enables the CDITestEnricher of Arquillian, which is far more capable than the EJBTestEnricher
            // can handle @Inject annotation as well as @Resource and @EJB annotations (see section on Resources injection in CDI spec
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @EJB(lookup="java:module/prodKSessionProxy!org.jboss.processFlow.knowledgeService.IKnowledgeSessionService")
    IKnowledgeSessionService kProxy;

    @Test
    public void dumpSessionStatusInfoTest() {
        System.out.println("dumpSessionInfoTest() info = "+kProxy.dumpSessionStatusInfo());
    }

}
