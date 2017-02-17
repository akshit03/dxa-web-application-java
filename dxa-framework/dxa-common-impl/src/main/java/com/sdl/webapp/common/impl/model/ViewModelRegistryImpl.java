package com.sdl.webapp.common.impl.model;

import com.sdl.webapp.common.api.mapping.semantic.SemanticMappingRegistry;
import com.sdl.webapp.common.api.model.EntityModel;
import com.sdl.webapp.common.api.model.MvcData;
import com.sdl.webapp.common.api.model.ViewModel;
import com.sdl.webapp.common.api.model.ViewModelRegistry;
import com.sdl.webapp.common.api.model.mvcdata.MvcDataCreator;
import com.sdl.webapp.common.exceptions.DxaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.sdl.webapp.common.api.model.mvcdata.DefaultsMvcData.getDefaultAreaName;

@Component
public class ViewModelRegistryImpl implements ViewModelRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ViewModelRegistryImpl.class);

    private static final Map<MvcData, Class<? extends ViewModel>> viewEntityClassMap = new HashMap<>();

    private static Lock lock = new ReentrantLock();

    @Autowired
    private SemanticMappingRegistry semanticMappingRegistry;

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends ViewModel> getViewEntityClass(final String viewName) throws DxaException {

        final String areaName;
        final String scopedViewName;
        if (!viewName.contains(":")) { // default module
            areaName = getDefaultAreaName();
            scopedViewName = viewName;
        } else {
            String[] parts = viewName.split(":");
            areaName = parts[0];
            scopedViewName = parts[1];
        }

        return viewEntityClassMap.entrySet().stream()
                .filter(mvcData -> mvcData.getKey().getAreaName().equals(areaName)
                        && mvcData.getKey().getViewName().equals(scopedViewName))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElseThrow(() -> new DxaException(String.format("Could not find a view model for the view name %s", viewName)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends ViewModel> getMappedModelTypes(Set<String> semanticTypeNames) {
        Class<? extends ViewModel> entityClass = null;
        for (String fullyQualifiedName : semanticTypeNames) {
            entityClass = getMappedModelTypes(fullyQualifiedName);
            if (entityClass != null) {
                break;
            }
        }
        if (entityClass == null) {
            LOG.error("Cannot determine entity type for semantic schema names: '{}'. Please make sure " +
                    "that an entry is registered for this view name in the ViewModelRegistry.", semanticTypeNames);
        }
        return entityClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends ViewModel> getMappedModelTypes(String semanticTypeName) {

        Class<? extends ViewModel> retval = this.semanticMappingRegistry.getEntityClassByFullyQualifiedName(semanticTypeName);
        if (retval != null) {
            return retval;
        }
        //Fallback
        MvcData mvcData = MvcDataCreator.creator().fromQualifiedName(semanticTypeName).create();
        return getViewModelType(mvcData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends ViewModel> getViewModelType(final MvcData viewData) {
        Set<Map.Entry<MvcData, Class<? extends ViewModel>>> entries = viewEntityClassMap.entrySet();

        Optional<? extends Class<? extends ViewModel>> entry =
                entries.stream()
                        .filter(mvcData -> {
                            MvcData key = mvcData.getKey();
                            return key.getViewName().equals(viewData.getViewName()) &&
                                    key.getControllerName().equals(viewData.getControllerName()) &&
                                    key.getAreaName().equals(viewData.getAreaName());
                        })
                        .map(Map.Entry::getValue)
                        .findFirst();

        if (entry.isPresent()) {
            return entry.get();
        }

        return entries.stream()
                .filter(mvcData -> {
                    MvcData key = mvcData.getKey();
                    return key.getViewName().equals(viewData.getViewName()) &&
                            (key.getControllerName().equals(viewData.getControllerName()) || viewData.getControllerName() == null);
                })
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerViewModel(MvcData viewData, Class<? extends ViewModel> entityClass) {
        try {
            if (lock.tryLock(10, TimeUnit.SECONDS)) {
                if (viewData != null) {
                    if (viewEntityClassMap.containsKey(viewData)) {
                        LOG.warn("View {} registered multiple times.", viewData);
                        return;
                    }
                    viewEntityClassMap.put(viewData, entityClass);
                }
                semanticMappingRegistry.registerEntity((Class<? extends EntityModel>) entityClass);
            }
        } catch (InterruptedException e) {
            LOG.warn(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }
}
