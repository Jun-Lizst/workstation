package org.janelia.it.workstation.gui.browser.api.facade.interfaces;

import java.util.Collection;
import java.util.List;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;

/**
 * Interface for client implementations providing domain object access. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainFacade {

    public List<Subject> getSubjects();
        
    public DomainObject getDomainObject(Class<? extends DomainObject> domainClass, Long id);
    
    public DomainObject getDomainObject(Reference reference);
    
    public List<DomainObject> getDomainObjects(List<Reference> references);
    
    public List<DomainObject> getDomainObjects(String type, Collection<Long> ids);
    
    public List<Annotation> getAnnotations(Collection<Long> targetIds);
    
    public Workspace getDefaultWorkspace();
    
    public Collection<Workspace> getWorkspaces();
    
    public Collection<Ontology> getOntologies();
    
    public void changePermissions(String type, Collection<Long> ids, String granteeKey, String rights, boolean grant) throws Exception;
    
    public TreeNode create(TreeNode treeNode) throws Exception;
    
    public ObjectSet create(ObjectSet objectSet) throws Exception;
    
    public Filter create(Filter filter) throws Exception;
    
    public Filter update(Filter filter) throws Exception;
    
    public TreeNode reorderChildren(TreeNode treeNode, int[] order) throws Exception;       
    
    public TreeNode addChildren(TreeNode treeNode, Collection<Reference> references) throws Exception;
    
    public TreeNode removeChildren(TreeNode treeNode, Collection<Reference> references) throws Exception;
    
    public ObjectSet addMembers(ObjectSet objectSet, Collection<Reference> references) throws Exception;
    
    public ObjectSet removeMembers(ObjectSet objectSet, Collection<Reference> references) throws Exception;
    
    public DomainObject updateProperty(DomainObject domainObject, String propName, String propValue);
    
}
