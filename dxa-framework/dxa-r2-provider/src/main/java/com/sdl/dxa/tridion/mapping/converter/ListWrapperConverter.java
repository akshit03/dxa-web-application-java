package com.sdl.dxa.tridion.mapping.converter;

import com.sdl.dxa.api.datamodel.model.util.ListWrapper;
import com.sdl.dxa.tridion.mapping.ModelBuilderPipeline;
import com.sdl.dxa.tridion.mapping.impl.DefaultSemanticFieldDataProvider;
import com.sdl.webapp.common.api.mapping.semantic.config.SemanticField;
import com.sdl.webapp.tridion.fields.exceptions.FieldConverterException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@Component
public class ListWrapperConverter implements SourceConverter<ListWrapper> {

    // can't be in a constructor because of circular dependency
    @SuppressWarnings("SpringAutowiredFieldsWarningInspection")
    @Autowired
    private SourceConverterFactory sourceConverterFactory;

    @Override
    public List<Class<? extends ListWrapper>> getTypes() {
        return Arrays.asList(ListWrapper.class,
                ListWrapper.ContentModelDataListWrapper.class,
                ListWrapper.KeywordModelDataListWrapper.class,
                ListWrapper.EntityModelDataListWrapper.class,
                ListWrapper.RichTextDataListWrapper.class);
    }

    @Override
    public Object convert(ListWrapper toConvert, TypeInformation targetType, SemanticField semanticField, ModelBuilderPipeline pipeline,
                          DefaultSemanticFieldDataProvider dataProvider) throws FieldConverterException {
        TypeDescriptor elementType = TypeDescriptor.valueOf(targetType.getObjectType());

        if (targetType.isCollection()) {
            Collection<Object> result = targetType.getCollectionType() == List.class ? new ArrayList<>() : new HashSet<>();

            for (Object value : ((ListWrapper<?>) toConvert).getValues()) {
                result.add(convertValue(value, elementType, semanticField, pipeline, dataProvider));
            }

            return result;
        } else {
            return convertValue(toConvert.getValues().get(0), elementType, semanticField, pipeline, dataProvider);
        }
    }

    @NotNull
    private Object convertValue(Object toConvert, TypeDescriptor targetType, SemanticField semanticField,
                                ModelBuilderPipeline pipeline, DefaultSemanticFieldDataProvider dataProvider) throws FieldConverterException {
        return sourceConverterFactory.convert(toConvert, targetType, semanticField, pipeline, dataProvider.embedded(toConvert));
    }
}
