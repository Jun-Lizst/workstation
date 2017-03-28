package org.janelia.it.workstation.browser.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.shared.utils.ReflectionsHelper;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.facade.impl.ejb.LegacyFacadeImpl;
import org.janelia.it.workstation.browser.api.facade.interfaces.DomainFacade;
import org.janelia.it.workstation.browser.api.facade.interfaces.LegacyFacade;
import org.janelia.it.workstation.browser.api.facade.interfaces.OntologyFacade;
import org.janelia.it.workstation.browser.api.facade.interfaces.SampleFacade;
import org.janelia.it.workstation.browser.api.facade.interfaces.SubjectFacade;
import org.janelia.it.workstation.browser.api.facade.interfaces.WorkspaceFacade;
import org.janelia.it.workstation.browser.api.sage_responder.SageResponderRestClient;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.lifecycle.RunAsEvent;
import org.janelia.it.workstation.browser.events.model.PreferenceChangeEvent;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * Singleton for managing the Domain Model and related data access. 
 * 
 * Listens for session events and invalidates every object in the model if the current user changes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainMgr {

    private static final Logger log = LoggerFactory.getLogger(DomainMgr.class);

    private static final String DOMAIN_FACADE_PACKAGE_NAME = ConsoleProperties.getInstance().getProperty("domain.facade.package");
    
    // Singleton
    private static DomainMgr instance;
    public static DomainMgr getDomainMgr() {
        if (instance==null) {            
            instance = new DomainMgr();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }

    private DomainFacade domainFacade;
    private OntologyFacade ontologyFacade;
    private SampleFacade sampleFacade;
    private SubjectFacade subjectFacade;
    private WorkspaceFacade workspaceFacade;
    private SageResponderRestClient sageClient;
    private LegacyFacade legacyFacade;
    
    private DomainModel model;
    private Map<String,Preference> preferenceMap;
    
    private DomainMgr() {
        log.info("Initializing Domain Manager");
        try {
            final Reflections reflections = ReflectionsHelper.getReflections(DOMAIN_FACADE_PACKAGE_NAME, getClass());
            domainFacade = getNewInstance(reflections, DomainFacade.class);
            ontologyFacade = getNewInstance(reflections, OntologyFacade.class);
            sampleFacade = getNewInstance(reflections, SampleFacade.class);
            subjectFacade = getNewInstance(reflections, SubjectFacade.class);
            workspaceFacade = getNewInstance(reflections, WorkspaceFacade.class);
            sageClient = new SageResponderRestClient();
            legacyFacade = new LegacyFacadeImpl();
            
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
        }
    }
    
    private <T> T getNewInstance(Reflections reflections, Class<T> clazz) {
        for(Class<? extends T> implClass : reflections.getSubTypesOf(clazz)) {
            try {
                return implClass.newInstance();
            }
            catch (InstantiationException | IllegalAccessException e) {
                log.error("Cannot instantiate "+implClass.getName(),e);
            }
        }
        throw new IllegalStateException("No implementation for "+clazz.getName()+" found in "+DOMAIN_FACADE_PACKAGE_NAME);
    }

    public DomainFacade getDomainFacade() {
        return domainFacade;
    }

    public OntologyFacade getOntologyFacade() {
        return ontologyFacade;
    }

    public SampleFacade getSampleFacade() {
        return sampleFacade;
    }

    public SubjectFacade getSubjectFacade() {
        return subjectFacade;
    }

    public WorkspaceFacade getWorkspaceFacade() {
        return workspaceFacade;
    }
    
    public SageResponderRestClient getSageClient() {
        return sageClient;
    }

    public LegacyFacade getLegacyFacade() {
        return legacyFacade;
    }

    @Subscribe
    public void runAsUserChanged(RunAsEvent event) {
        log.info("User changed, resetting model");
        model.invalidateAll();
        preferenceMap = null;
    }
    
    /**
     * Returns a lazy domain model instance. 
     * @return domain model
     */
    public DomainModel getModel() {
        if (model == null) {
            model = new DomainModel(domainFacade, ontologyFacade, sampleFacade, subjectFacade, workspaceFacade);
        }
        return model;
    }
    
    /**
     * Queries the backend and returns a list of subjects sorted by: 
     * groups then users, alphabetical by full name. 
     * @return sorted list of subjects
     */
    public List<Subject> getSubjects() throws Exception {
        List<Subject> subjects = subjectFacade.getSubjects();
        DomainUtils.sortSubjects(subjects);
        return subjects;
    }

    private void loadPreferences() throws Exception {
        if (preferenceMap==null) {
            preferenceMap = new HashMap<>();
            for (Preference preference : subjectFacade.getPreferences()) {
                preferenceMap.put(getPreferenceMapKey(preference), preference);
            }
            log.info("Loaded {} user preferences", preferenceMap.size());
        }
    }
    
    /**
     * Queries the backend and returns the list of preferences for the given subject.
     * @param category
     * @param key
     * @return
     */
    public Preference getPreference(String category, String key) throws Exception {
        loadPreferences();
        String mapKey = category+":"+key;
        return preferenceMap.get(mapKey);
    }

    public Object getPreferenceValue(String category, String key, Object defaultValue) throws Exception {
        Preference preference = getPreference(category, key);
        if (preference==null) return defaultValue;
        return preference.getValue();
    }

    public List<Preference> getPreferences(String category) throws Exception {
        loadPreferences();
        List<Preference> categoryPreferences = new ArrayList<>();
        for(Preference preference : preferenceMap.values()) {
            if (preference.getCategory().equals(category)) {
                categoryPreferences.add(preference);
            }
        }
        return categoryPreferences;
    }
    
    /**
     * Saves the given preference. 
     * @param preference
     * @throws Exception
     */
    public void savePreference(Preference preference) throws Exception {
        Preference updated = subjectFacade.savePreference(preference);
        preferenceMap.put(getPreferenceMapKey(preference), updated);
        notifyPreferenceChanged(updated);
        log.info("Saved preference in category {} with {}={}",preference.getCategory(),preference.getKey(),preference.getValue());
    }

    /**
     * Set the given preference value, creating the preference if necessary.
     * @param category
     * @param key
     * @param value
     * @throws Exception
     */
    public void setPreference(String category, String key, Object value) throws Exception {
        Preference preference = DomainMgr.getDomainMgr().getPreference(category, key);
        if (preference==null) {
            preference = new Preference(AccessManager.getSubjectKey(), category, key, value);
        }
        else {
            preference.setValue(value);
        }
        savePreference(preference);
    }

    public Map<String,String> loadPreferencesAsMap(String category) throws Exception {
        List<Preference> titlePreferences = DomainMgr.getDomainMgr().getPreferences(category);
        Map<String,String> map = new HashMap<>();
        for(Preference preference : titlePreferences) {
            map.put(preference.getKey(), (String)preference.getValue());
        }
        return map;
    }

    public void saveMapAsPreferences(Map<String,String> map, String category) throws Exception {
        for(String key : map.keySet()) {
            String value = map.get(key);
            if (value!=null) {
                Preference preference = DomainMgr.getDomainMgr().getPreference(category, key);
                if (preference==null) {
                    preference = new Preference(AccessManager.getSubjectKey(), category, key, value);
                    DomainMgr.getDomainMgr().savePreference(preference);
                }
                else if (!StringUtils.areEqual(preference.getValue(), value)) {
                    preference.setValue(value);
                    DomainMgr.getDomainMgr().savePreference(preference);
                }
            }
        }
    }

    private String getPreferenceMapKey(Preference preference) {
        return preference.getCategory()+":"+preference.getKey();
    }
    
    private void notifyPreferenceChanged(Preference preference) {
        if (log.isTraceEnabled()) {
            log.trace("Generating PreferenceChangeEvent for {}", preference);
        }
        Events.getInstance().postOnEventBus(new PreferenceChangeEvent(preference));
    }
}