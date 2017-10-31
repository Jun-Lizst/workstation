package org.janelia.it.workstation.ab2;


import java.util.List;

import org.janelia.it.jacs.integration.framework.domain.DomainObjectAcceptor;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.ab2.api.AB2RestClient;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.event.AB2SampleAddedEvent;
import org.janelia.it.workstation.ab2.model.AB2DomainObject;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceProvider(service = DomainObjectAcceptor.class, path=DomainObjectAcceptor.DOMAIN_OBJECT_LOOKUP_PATH)
public class AB2DomainObjectAcceptor implements DomainObjectAcceptor  {

    private static final Logger logger = LoggerFactory.getLogger(AB2DomainObjectAcceptor.class);

    private static final int MENU_ORDER = 200;

    public AB2DomainObjectAcceptor() {
    }

    @Override
    public void acceptDomainObject(DomainObject dObj) {
        logger.info("acceptDomainObject() dObj type="+dObj.getClass().getName());
        AB2TopComponent ab2TopComponent=AB2TopComponent.findComp();
        if (ab2TopComponent!=null) {
            if (!ab2TopComponent.isOpened()) {
                ab2TopComponent.open();
            }
            if (ab2TopComponent.isOpened()) {
                ab2TopComponent.requestActive();
            }
            ab2TopComponent.loadDomainObject(dObj, true);
        }
    }

    @Override
    public String getActionLabel() {
        return "  Open In AB2";
    }

    @Override
    public boolean isCompatible(DomainObject dObj) {
        logger.trace(dObj.getType() + " called " + dObj.getName() + " class: " + dObj.getClass().getSimpleName());
        if (dObj instanceof Sample) {
            return true;
        } else if (dObj instanceof AB2DomainObject) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isEnabled(DomainObject dObj) {
        return true;
    }

    @Override
    public Integer getOrder() {
        return MENU_ORDER;
    }

    @Override
    public boolean isPrecededBySeparator() {
        return false;
    }

    @Override
    public boolean isSucceededBySeparator() {
        return false;
    }

}