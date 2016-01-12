package ddf.catalog.transformer.generic.xml;

import ddf.catalog.util.Describable;

public interface SaxEventHandlerFactory extends Describable {

    SaxEventHandler getNewSaxEventHandler();

}
