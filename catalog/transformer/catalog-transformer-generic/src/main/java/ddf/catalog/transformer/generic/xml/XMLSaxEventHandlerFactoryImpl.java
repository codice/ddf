package ddf.catalog.transformer.generic.xml;


public class XMLSaxEventHandlerFactoryImpl implements SaxEventHandlerFactory {

    private static final String VERSION = "1.0";

    private static final String ID = "XML_Handler";

    private static final String TITLE = "XMLSaxEventHandler Factory";

    private static final String DESCRIPTION = "Factory that returns a SaxEventHandler to help parse XML Metacards";

    private static final String ORGANIZATION = "Codice";
    @Override
    public SaxEventHandler getNewSaxEventHandler() {
        return new XMLSaxEventHandlerImpl();
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getOrganization() {
        return ORGANIZATION;
    }
}
