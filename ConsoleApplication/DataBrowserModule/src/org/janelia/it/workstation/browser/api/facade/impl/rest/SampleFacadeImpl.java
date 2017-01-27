package org.janelia.it.workstation.browser.api.facade.impl.rest;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.janelia.it.jacs.model.domain.enums.PipelineStatus;
import org.janelia.it.jacs.model.domain.orders.IntakeOrder;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.LineRelease;
import org.janelia.it.jacs.model.domain.sample.StatusTransition;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.facade.interfaces.SampleFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleFacadeImpl extends RESTClientImpl implements SampleFacade {

    private static final Logger log = LoggerFactory.getLogger(SampleFacadeImpl.class);
    private RESTClientManager manager;

    public SampleFacadeImpl() {
        super(log);
        this.manager = RESTClientManager.getInstance();
    }

    @Override
    public Collection<DataSet> getDataSets() throws Exception {
        Response response = manager.getDataSetEndpoint()
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getDataSets from server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(new GenericType<List<DataSet>>() {});
    }

    @Override
    public DataSet create(DataSet dataSet) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(dataSet);
        Response response = manager.getDataSetEndpoint()
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request createDataSet from server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(DataSet.class);
    }

    @Override
    public DataSet update(DataSet dataSet) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(dataSet);
        query.setSubjectKey(AccessManager.getSubjectKey());
        Response response = manager.getDataSetEndpoint()
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request updateDataSet to server: " + dataSet)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(DataSet.class);
    }

    @Override
    public void remove(DataSet dataSet) throws Exception {
        Response response = manager.getDataSetEndpoint()
                .queryParam("dataSetId", dataSet.getId())
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .delete();
        if (checkBadResponse(response.getStatus(), "problem making request removeDataSet from server: " + dataSet)) {
            throw new WebApplicationException(response);
        }
    }

    @Override
    public Collection<LSMImage> getLsmsForSample(Long sampleId) throws Exception {
        Response response = manager.getSampleEndpoint()
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("sampleId", sampleId)
                .path("lsms")
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request to get Lsm For Sample: " + sampleId)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity((new GenericType<List<LSMImage>>() {}));
    }

    @Override
    public List<LineRelease> getLineReleases() throws Exception {
        Response response = manager.getReleaseEndpoint()
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request to get line releases")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity((new GenericType<List<LineRelease>>() {}));
    }

    @Override
    public LineRelease createLineRelease(String name, Date releaseDate, Integer lagTimeMonths, List<String> dataSets) throws Exception {
        DomainQuery query = new DomainQuery();

        LineRelease release = new LineRelease();
        release.setName(name);
        release.setReleaseDate(releaseDate);
        release.setLagTimeMonths(lagTimeMonths);
        release.setDataSets(dataSets);

        query.setDomainObject(release);
        query.setSubjectKey(AccessManager.getSubjectKey());
        Response response = manager.getReleaseEndpoint()
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request createLineRelease to server: " + release)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(LineRelease.class);
    }

    @Override
    public LineRelease update(LineRelease release) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(release);
        query.setSubjectKey(AccessManager.getSubjectKey());
        Response response = manager.getReleaseEndpoint()
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request updateLineRelease to server: " + release)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(LineRelease.class);
    }

    @Override
    public void remove(LineRelease release) throws Exception {
        Response response = manager.getReleaseEndpoint()
                .queryParam("releaseId", release.getId())
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .delete();
        if (checkBadResponse(response.getStatus(), "problem making request removeRelease from server: " + release)) {
            throw new WebApplicationException(response);
        }
    }

    public void addStatusTransition(StatusTransition transition) throws Exception {
        Response response = manager.getSampleEndpoint()
                .path("transitions")
                .request("application/json")
                .put(Entity.json(transition));
        if (checkBadResponse(response.getStatus(), "problem making request to add a pipeline status transition: " + transition.getSampleId())) {
            throw new WebApplicationException(response);
        }
    }


    public void addIntakeOrder(IntakeOrder order) throws Exception {
        Response response = manager.getSampleEndpoint()
                .path("intakeorder")
                .request("application/json")
                .put(Entity.json(order));
        if (checkBadResponse(response.getStatus(), "problem making request to create an intake order for reprocessing pipelines: " + order.getOrderNo())) {
            throw new WebApplicationException(response);
        }
    }
}