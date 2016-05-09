package org.janelia.it.workstation.gui.browser.api.facade.impl.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.jacs.shared.solr.SolrJsonResults;
import org.janelia.it.jacs.shared.solr.SolrParams;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.WorkspaceFacade;

public class WorkspaceFacadeImpl extends RESTClientImpl implements WorkspaceFacade {

    private RESTClientManager manager;
    
    public WorkspaceFacadeImpl() {
        this.manager = RESTClientManager.getInstance();
    }

    /**
     * Performs a search against the SolrServer and returns the results.
     * @param query the query to execute against the search server
     * @return the search results
     * @throws Exception something went wrong
     */
    @Override
    public SolrJsonResults performSearch(SolrParams query) {
        Response response = manager.getSearchEndpoint()
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making search request to the server: " + query)) {
            return null;
        }
        SolrJsonResults results = response.readEntity(SolrJsonResults.class);
        return results;
    }

    @Override
    public Workspace getDefaultWorkspace() {
        Response response = manager.getWorkspaceEndpoint()
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getDefaultWorkspace from server")) {
            return null;
        }
        Workspace workspace = response.readEntity(Workspace.class);
        return workspace;
    }

    @Override
    public Collection<Workspace> getWorkspaces() {
        Response response = manager.getWorkspacesEndpoint()
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getWorkspaces from server")) {
            return null;
        }

        Collection<Workspace> workspace = response.readEntity(new GenericType<List<Workspace>>() {});
        return workspace;
    }


    @Override
    public TreeNode create(TreeNode treeNode) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(treeNode);
        query.setSubjectKey(AccessManager.getSubjectKey());
        Response response = manager.getTreeNodeEndpoint()
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request createTreeNode to server: " + treeNode)) {
            return null;
        }
        TreeNode newTreeNode = response.readEntity(TreeNode.class);
        return newTreeNode;
    }

    @Override
    public Filter create(Filter filter) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(filter);
        query.setSubjectKey(AccessManager.getSubjectKey());
        Response response = manager.getFilterEndpoint()
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request createFilter to server: " + filter)) {
            return null;
        }
        Filter newFilter = response.readEntity(Filter.class);
        return newFilter;
    }

    @Override
    public Filter update(Filter filter) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(filter);
        query.setSubjectKey(AccessManager.getSubjectKey());
        Response response = manager.getFilterEndpoint()
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request updateFilter to server: " + filter)) {
            return null;
        }
        Filter newFilter = response.readEntity(Filter.class);
        return newFilter;
    }

    @Override
    public TreeNode addChildren(TreeNode treeNode, Collection<Reference> references, Integer index) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(treeNode);
        query.setReferences(new ArrayList<>(references));
        Response response = manager.getTreeNodeEndpoint()
                .path("children")
                .request("application/json")
                .put(Entity.json(query));
        TreeNode updatedTreeNode = response.readEntity(TreeNode.class);
        if (checkBadResponse(response.getStatus(), "problem making request addChildrenToTreeNode to server: " + treeNode + "," + references)) {
            return null;
        }
        return updatedTreeNode;
    }


    @Override
    public TreeNode removeChildren(TreeNode treeNode, Collection<Reference> references) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(treeNode);
        query.setReferences(new ArrayList<>(references));
        Response response = manager.getTreeNodeEndpoint()
                .path("children")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request removeChildrenFromTreeNode to server: " + treeNode + "," + references)) {
            return null;
        }
        TreeNode updatedTreeNode = response.readEntity(TreeNode.class);
        return updatedTreeNode;
    }

    @Override
    public TreeNode reorderChildren(TreeNode treeNode, int[] order) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(treeNode);
        List<Integer> orderList = new ArrayList<>();
        for (int i=0; i<order.length; i++) {
            orderList.add(new Integer(order[i]));
        }
        query.setOrdering(orderList);
        Response response = manager.getTreeNodeEndpoint()
                .path("reorder")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request reorderChildrenInTreeNode to server: " + treeNode + "," + order)) {
            return null;
        }
        TreeNode sortedTreeNode = response.readEntity(TreeNode.class);
        return sortedTreeNode;
    }

    @Override
    public List<Reference> getContainerReferences(DomainObject object) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(object);
        Response response = manager.getDomainObjectEndpoint()
                .path("references")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request to get Ancestors from server for: " + object.getId())) {
            return null;
        }
        List<Reference> references = response.readEntity(new GenericType<List<Reference>>() {});
        return references;
    }
}
