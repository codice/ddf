package ddf.content.data.impl;

import java.util.HashSet;
import java.util.Set;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;

public class ContentMetacardType extends MetacardTypeImpl {

    public static final String RESOURCE_CHECKSUM = "resource-checksum";
    public static final String RESOURCE_CHECKSUM_ALGORITHM = "resource-checksum-algorithm";
    private static  final String CONTENT_METACARD_TYPE_NAME = "content";

    public ContentMetacardType(){
        this(CONTENT_METACARD_TYPE_NAME, new HashSet<>());
    }
    /**
     * Creates a {@code MetacardTypeImpl} with the provided {@code name} and
     * {@link AttributeDescriptor}s.
     *
     * @param name        the name of this {@code MetacardTypeImpl}
     * @param attributeDescriptors the set of descriptors for this {@code MetacardTypeImpl}
     */
    public ContentMetacardType(String name, Set<AttributeDescriptor> attributeDescriptors) {
        super(name, attributeDescriptors);
        descriptors.addAll(BasicTypes.BASIC_METACARD.getAttributeDescriptors());
        descriptors.add(new AttributeDescriptorImpl(RESOURCE_CHECKSUM, true /*Indexed*/,
                true /* Stored */, false /* Tokenized */, false /* Multivalued */, BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(RESOURCE_CHECKSUM_ALGORITHM,true /*Indexed */,
                true /* Stored */,false, /*Tokenized */ false /* Multivalued */,BasicTypes.STRING_TYPE ));
    }
}
