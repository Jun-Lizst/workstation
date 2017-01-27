package org.janelia.it.workstation.api.entity_model.fundtype;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 3:57 PM
 */

import org.janelia.it.workstation.shared.util.MTObservable;

import java.util.*;

/**
 * This is a model of the threads that are actively loading in the system
 */
public class ActiveThreadModel extends MTObservable {
    static private ActiveThreadModel activeThreadModel;

    private Map statusObjects = Collections.synchronizedMap(new HashMap());

    private ActiveThreadModel() {
    }

    public static ActiveThreadModel getActiveThreadModel() {
        if (activeThreadModel == null) activeThreadModel = new ActiveThreadModel();
        return activeThreadModel;
    }

    public TaskRequestStatus[] getActiveLoadRequestStatusObjects() {
        Set activeEntries = null;
        synchronized (statusObjects) {
            activeEntries = statusObjects.entrySet();
        }
        List statusObjects = new ArrayList();
        for (Object activeEntry : activeEntries) {
            statusObjects.add(((Map.Entry) activeEntry).getValue());
        }
        return (TaskRequestStatus[]) statusObjects.toArray(new TaskRequestStatus[statusObjects.size()]);
    }

    public int getActiveThreadCount() {
        return statusObjects.size();
    }

    public void addObserver(Observer observer) {
        super.addObserver(observer);
    }

    public void addObserver(Observer observer, boolean bringUpToDate) {
        addObserver(observer);
        for (Iterator it = statusObjects.keySet().iterator(); it.hasNext(); ) {
            observer.update(this, statusObjects.get(it.next()));
        }
    }

    void addActiveTaskRequestStatus(TaskRequestStatus taskRequestStatus) {
        statusObjects.put(taskRequestStatus, taskRequestStatus);
        setChanged();
        notifyObservers(taskRequestStatus);
        clearChanged();
    }

    void removeActiveTaskRequestStatus(TaskRequestStatus taskRequestStatus) {
        statusObjects.remove(taskRequestStatus);
        setChanged();
        notifyObservers(taskRequestStatus);
        clearChanged();
    }


}

