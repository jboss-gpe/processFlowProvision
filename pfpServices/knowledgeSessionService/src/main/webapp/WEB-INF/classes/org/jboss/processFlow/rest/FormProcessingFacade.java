package org.jboss.processFlow.rest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.processFlow.tasks.ITaskService;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jbpm.integration.console.shared.GuvnorConnectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

@Stateless
@Path("/form")
public class FormProcessingFacade {
    
    private static final Logger log = LoggerFactory.getLogger(FormProcessingFacade.class);
    
    @EJB(lookup="java:global/processFlow-taskService/taskProxy!org.jboss.processFlow.tasks.ITaskService")
    ITaskService taskServiceProxy;
    
    @GET
    @Path("task/{id}/render")
    @Produces({ "text/html" })
    public Response renderTaskUI(@PathParam("id") String taskId) {
        return provideForm(new FormAuthorityRef(taskId));
    } 
    
    @POST
    @Path("task/{id}/{userId}/complete")
    @Produces({ "text/html" })
    @Consumes({ "multipart/form-data" })
    public Response closeTaskWithUI(@Context HttpServletRequest request, @PathParam("id") String taskId, @PathParam("userId") String userId,
            MultipartFormDataInput payload) {
        FieldMapping mapping = createFieldMapping(payload);

        String outcomeDirective = (String) mapping.directives.get("outcome");

        if (outcomeDirective != null) {
            completeTask(Long.valueOf(taskId).longValue(), outcomeDirective, mapping.processVars, userId);
        } else {
            completeTask(Long.valueOf(taskId).longValue(), mapping.processVars, userId);
        }

        return Response.ok("<div style='font-family:sans-serif; padding:10px;'><h3>Successfully processed input</h3><p/>You can now close this window.</div>")
                .build();
    }
    
    @SuppressWarnings("unchecked")
    public void completeTask(long taskId, Map data, String userId) {
        try {
            taskServiceProxy.completeTask(taskId, data, userId);
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    @SuppressWarnings("unchecked")
    public void completeTask(long taskId, String outcome, Map data, String userId) {
        if ("jbpm_skip_task".equalsIgnoreCase(outcome)) {
            taskServiceProxy.skipTask(taskId, userId, null);
        } else {
            data.put("outcome", outcome);
            completeTask(taskId, data, userId);
        }
    }
    
    private FieldMapping createFieldMapping(MultipartFormDataInput payload) {
        FieldMapping mapping = new FieldMapping();

        Map formData = payload.getFormData();
        Iterator partNames = formData.keySet().iterator();

        while (partNames.hasNext()) {
            final String partName = (String) partNames.next();
            InputPart part = (InputPart) formData.get(partName);
            final MediaType mediaType = part.getMediaType();

            String mType = mediaType.getType();
            String mSubtype = mediaType.getSubtype();
            try {
                if (("text".equals(mType)) && ("plain".equals(mSubtype))) {
                    if (mapping.isReserved(partName))
                        mapping.directives.put(partName, part.getBodyAsString());
                    else {
                        mapping.processVars.put(partName, part.getBodyAsString());
                    }
                } else {
                    final byte[] data = part.getBodyAsString().getBytes();
                    DataHandler dh = new DataHandler(new DataSource() {
                        public InputStream getInputStream() throws IOException {
                            return new ByteArrayInputStream(data);
                        }

                        public OutputStream getOutputStream() throws IOException {
                            throw new RuntimeException("This is a readonly DataHandler");
                        }

                        public String getContentType() {
                            return mediaType.getType();
                        }

                        public String getName() {
                            return partName;
                        }
                    });
                    mapping.processVars.put(partName, dh);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return mapping;
    }
    
    private Response provideForm(FormAuthorityRef authorityRef) {
        DataHandler dh = provideFormDataHandler(authorityRef);

        if (null == dh) {
            throw new RuntimeException("No UI associated with " + authorityRef.getType() + " " + authorityRef.getReferenceId());
        }

        return Response.ok(dh.getDataSource()).type("text/html").build();
    } 
    
    private DataHandler provideFormDataHandler(FormAuthorityRef ref) {
        String englishTaskName = taskServiceProxy.getTaskName(new Long(ref.getReferenceId()), "en-UK");

        InputStream template = getTemplate(englishTaskName);
        if (template == null)
            throw new  RuntimeException("provideForm() unable to locate FreeMarker Template with name = "+englishTaskName);

        // get processInstance --> task variable Map
        Map<?,?> documentContentMap = taskServiceProxy.getTaskContent(new Long(ref.getReferenceId()), true);

        Map<String, Object> renderContext = null; 
        //renderContext.put("content", documentContentMap);
        if(documentContentMap.size() != 0) {
            renderContext = new HashMap<String, Object>();
            for (Map.Entry<?, ?> entry: documentContentMap.entrySet()) {
                if (entry.getKey() instanceof String) {
                    renderContext.put((String) entry.getKey(), entry.getValue());
                } else {
                    log.warn("provideForm() processInstance --> task variables includes a non-String variable .. will skip : "+entry.getKey()+" : "+entry.getValue());
                }
            }
        } else {
            log.warn("provideForm() processInstance --> task variables map is empty and will be passed a null rootMap !!!");
        }

        // merge template with process variables
        try {
            return processTemplate(englishTaskName, template, renderContext);
        } catch(RuntimeException x) {
            log.error("provideForm() the following are the contents of the processInstance --> task variables Map being used to create a FreeMarker template :");
            if(renderContext != null && renderContext.size() > 0) {
                for(Map.Entry<String, Object> entry : renderContext.entrySet()) {
                    log.error("\tkey = "+entry.getKey()+" : value = "+entry.getValue());
                }
            } else {
                log.error("provideForm() processInstance --> task variables map is empty and was passed a null rootMap !!!");
            }
            throw x;
        }
    }
    
    private InputStream getTemplate(String name) {
        // try to find on classpath
        InputStream nameTaskformResult = FormProcessingFacade.class.getResourceAsStream("/" + name + "-taskform.ftl");
        if (nameTaskformResult != null) {
            return nameTaskformResult;
        } else {
            InputStream nameResult = FormProcessingFacade.class.getResourceAsStream("/" + name + ".ftl");
            if (nameResult != null) {
                return nameResult;
            }
        }
        // try to find in guvnor repository
        GuvnorConnectionUtils guvnorUtils = new GuvnorConnectionUtils();
        if(guvnorUtils.guvnorExists()) {
            try {
                String templateName;
                if(guvnorUtils.templateExistsInRepo(name + "-taskform")) {
                    templateName = name + "-taskform";
                } else if(guvnorUtils.templateExistsInRepo(name)) {
                    templateName = name;
                } else {
                    return null;
                }
                return guvnorUtils.getFormTemplateFromGuvnor(templateName);
            } catch (Throwable t) {
                log.error("Could not load process template from Guvnor: " + t.getMessage());
                return null;
            }
        } else {
            log.warn("Could not connect to Guvnor.");
            return null;
        }
    }
    
    private DataHandler processTemplate(final String name, InputStream src, Map<String, Object> renderContext) {
        DataHandler merged = null;
        try {
            freemarker.log.Logger.selectLoggerLibrary(freemarker.log.Logger.LIBRARY_NONE);
            freemarker.template.Configuration cfg = new freemarker.template.Configuration();
            cfg.setObjectWrapper(new DefaultObjectWrapper());
            cfg.setTemplateUpdateDelay(0);
            Template temp = new Template(name, new InputStreamReader(src), cfg);
            final ByteArrayOutputStream bout = new ByteArrayOutputStream();
            Writer out = new OutputStreamWriter(bout);
            temp.process(renderContext, out);
            out.flush();
            merged = new DataHandler(new DataSource() {
                public InputStream getInputStream() throws IOException {
                    return new ByteArrayInputStream(bout.toByteArray());
                }
                public OutputStream getOutputStream() throws IOException {
                    return bout;
                }
                public String getContentType() {
                    return "*/*";
                }
                public String getName() {
                    return name + "_DataSource";
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to process form template", e);
        }
        return merged;
    }
    
    private class FieldMapping {
        final String[] reservedNames = { "outcome", "form" };

        Map<String, Object> processVars = new HashMap<String, Object>();
        Map<String, String> directives = new HashMap<String, String>();

        private FieldMapping() {
        }

        public boolean isReserved(String name) {
            boolean result = false;
            for (String s : this.reservedNames) {
                if (s.equals(name)) {
                    result = true;
                    break;
                }
            }
            return result;
        }
    }
    


}
