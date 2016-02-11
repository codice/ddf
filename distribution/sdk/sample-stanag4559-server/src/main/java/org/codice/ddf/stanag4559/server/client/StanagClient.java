package org.codice.ddf.stanag4559.server.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.AccessCriteria;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.AlterationSpec;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.CatalogMgr;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.CatalogMgrHelper;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.DataModelMgr;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.DataModelMgrHelper;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.DeliveryDetails;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.DeliveryManifest;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.DeliveryManifestHolder;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.Destination;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.GeoRegionType;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.GetParametersRequest;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.GetRelatedFilesRequest;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.HitCountRequest;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.Library;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.LibraryHelper;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.LibraryManager;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.MediaType;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.OrderContents;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.OrderMgr;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.OrderMgrHelper;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.OrderRequest;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.PackageElement;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.PackagingSpec;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.ProductDetails;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.ProductMgr;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.ProductMgrHelper;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.Query;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.SortAttribute;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.SubmitQueryRequest;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.TailoringSpec;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.ValidationResults;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.AbsTime;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.AbsTimeHelper;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.Coordinate2d;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.DAG;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.DAGHolder;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.DAGListHolder;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.Date;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.FileLocation;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.NameListHolder;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.NameName;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.NameValue;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.Node;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.NodeType;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.Rectangle;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.Time;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UID.Product;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UID._ProductStub;
import org.omg.CORBA.IntHolder;
import org.omg.CORBA.ORB;

public class StanagClient {

    private static Library library;

    private static CatalogMgr catalogMgr;

    private static OrderMgr orderMgr;

    private static ProductMgr productMgr;

    private static DataModelMgr dataModelMgr;

    private static final AccessCriteria accessCriteria = new AccessCriteria("", "", "");

    private static final String SERVER_PATH = "http://localhost:";

    private static final String IOR_PATH = "/data/ior.txt";

    ORB orb;

    public StanagClient(ORB orb) {
        this.orb = orb;
    }

    public void initLibrary(String iorFilePath) {
        org.omg.CORBA.Object obj = orb.string_to_object(iorFilePath);
        if (obj == null) {
            System.err.println("Cannot read " + iorFilePath);
        }
        library = LibraryHelper.narrow(obj);
    }

    public String[] getManagerTypes() throws Exception {
        String libraryName = library.get_library_description().library_name;
        String[] types = library.get_manager_types();
        System.out.println("Got Manager Types from " + libraryName + " : ");
        for (int i = 0; i < types.length; i++) {
            System.out.println("\t" + types[i]);
        }
        System.out.println();
        return types;
    }

    public void initManagers() throws Exception {
        // Get Mandatory Managers
        System.out.println("Getting CatalogMgr from source...");
        LibraryManager libraryManager = library.get_manager("CatalogMgr", accessCriteria);
        catalogMgr = CatalogMgrHelper.narrow(libraryManager);
        System.out.println("Source returned : " + catalogMgr.getClass() + "\n");

        System.out.println("Getting OrderMgr from source...");
        libraryManager = library.get_manager("OrderMgr", accessCriteria);
        orderMgr = OrderMgrHelper.narrow(libraryManager);
        System.out.println("Source returned : " + orderMgr.getClass() + "\n");

        System.out.println("Getting ProductMgr from source...");
        libraryManager = library.get_manager("ProductMgr", accessCriteria);
        productMgr = ProductMgrHelper.narrow(libraryManager);
        System.out.println("Source returned : " + productMgr.getClass() + "\n");

        System.out.println("Getting DataModelMgr from source...");
        libraryManager = library.get_manager("DataModelMgr", accessCriteria);
        dataModelMgr = DataModelMgrHelper.narrow(libraryManager);
        System.out.println("Source returned : " + dataModelMgr.getClass() + "\n");
    }

    public int getHitCount(Query query) throws Exception {
        System.out.println("Getting Hit Count From Query...");
        HitCountRequest hitCountRequest = catalogMgr.hit_count(query, new NameValue[0]);
        IntHolder intHolder = new IntHolder();
        hitCountRequest.complete(intHolder);
        System.out.println("Server responded with " + intHolder.value + " hit(s).\n");
        return intHolder.value;
    }

    public DAG[] submit_query(Query query) throws Exception {
        System.out.println("Submitting Query To Server...");
        DAGListHolder dagListHolder = new DAGListHolder();
        SubmitQueryRequest submitQueryRequest = catalogMgr.submit_query(query,
                new String[0],
                new SortAttribute[0],
                new NameValue[0]);
        submitQueryRequest.complete_DAG_results(dagListHolder);
        System.out.println("Server Responded with " + dagListHolder.value.length + " result(s).\n");
        return dagListHolder.value;
    }

    public void processAndPrintResults(DAG[] results) {
        System.out.println("Printing DAG Attribute Results...");
        for (int i = 0; i < results.length; i++) {
            printDAGAttributes(results[i]);
            try {
                retrieveProductFromDAG(results[i]);
            } catch (MalformedURLException e) {
                System.out.println("Invalid URL used for product retrieval.");
            }

        }
    }

    public void printDAGAttributes(DAG dag) {
        for (int i = 0; i < dag.nodes.length; i++) {
            Node node = dag.nodes[i];
            if (node.node_type.equals(NodeType.ATTRIBUTE_NODE)) {

                String result = null;

                if (node.value.type()
                        .toString()
                        .contains("unbounded string")) {
                    result = node.value.extract_string();
                } else if (node.value.type()
                        .toString()
                        .contains("ulong")) {
                    result = "" + node.value.extract_ulong();
                } else if (node.value.type()
                        .toString()
                        .contains("double")) {
                    result = "" + node.value.extract_double();
                } else if (node.value.type()
                        .toString()
                        .contains("AbsTime")) {
                    AbsTime absTime = AbsTimeHelper.extract(node.value);
                    String absDate =
                            absTime.aDate.month + "/" + absTime.aDate.day + "/" + absTime.aDate.year
                                    + ":";
                    String absHour = absTime.aTime.hour + ":" + absTime.aTime.minute + ":"
                            + absTime.aTime.second;
                    result = absDate + absHour;
                }
                System.out.printf("%25s : %s%n", node.attribute_name, result);
            }
        }
        System.out.println();
    }

    public void retrieveProductFromDAG(DAG dag) throws MalformedURLException {
        System.out.println("Downloading products...");
        for (int i = 0; i < dag.nodes.length; i++) {
            Node node = dag.nodes[i];
            if (node.attribute_name.equals("productUrl")) {

                String url = node.value.extract_string();
                URL fileDownload = new URL(url);
                String productPath = "product.jpg";
                System.out.println("Downloading product : " + url);
                try (FileOutputStream outputStream = new FileOutputStream(new File(productPath));
                        BufferedInputStream inputStream = new BufferedInputStream(fileDownload.openStream());
                ) {

                    byte[] data = new byte[1024];
                    int count;
                    while ((count = inputStream.read(data, 0, 1024)) != -1) {
                        outputStream.write(data, 0, count);
                    }

                    System.out.println("Successfully downloaded product from " + url + ".\n");
                    Files.deleteIfExists(Paths.get(productPath));

                } catch (IOException e) {
                    System.out.println("Unable to download product from " + url + ".\n");
                    e.printStackTrace();
                }
            }
        }
    }

    public void validate_order(ORB orb) throws Exception {
        System.out.println("Sending a Validate Order Request to Server...");

        NameValue[] properties = {new NameValue("", orb.create_any())};
        OrderContents order = createOrder(orb);
        ValidationResults validationResults = orderMgr.validate_order(order, properties);

        System.out.println("Validation Results: ");
        System.out.println("\tValid : " + validationResults.valid + "\n\tWarning : "
                + validationResults.warning + "\n\tDetails : " + validationResults.details + "\n");
    }

    public PackageElement[] order(ORB orb) throws Exception {
        System.out.println("Sending order request...");
        NameValue[] properties = {new NameValue("", orb.create_any())};
        OrderContents order = createOrder(orb);

        OrderRequest validationResults = orderMgr.order(order, properties);
        System.out.println("Completing OrderRequest...");

        DeliveryManifestHolder deliveryManifestHolder = new DeliveryManifestHolder();
        validationResults.complete(deliveryManifestHolder);
        DeliveryManifest deliveryManifest = deliveryManifestHolder.value;

        System.out.println("Completed Order :");
        System.out.println(deliveryManifest.package_name);

        PackageElement[] elements = deliveryManifest.elements;
        for (int i = 0; i < elements.length; i++) {

            String[] files = elements[i].files;

            for (int c = 0; c < files.length; c++) {
                System.out.println("\t" + files[c]);
            }

        }
        System.out.println();
        return elements;

    }

    public void get_parameters(ORB orb, Product product) throws Exception {
        System.out.println("Sending Get Parameters Request to Server...");

        String[] desired_parameters = {"param1", "param2"};
        NameValue[] properties = {new NameValue("", orb.create_any())};

        GetParametersRequest parametersRequest = productMgr.get_parameters(product,
                desired_parameters,
                properties);
        System.out.println("Completing GetParameters Request ...");

        DAGHolder dagHolder = new DAGHolder();
        parametersRequest.complete(dagHolder);

        DAG dag = dagHolder.value;
        System.out.println("Resulting Parameters From Server :");
        printDAGAttributes(dag);
        System.out.println();
    }

    public void get_related_file_types(Product product) throws Exception {
        System.out.println("Sending Get Related File Types Request...");
        String[] related_file_types = productMgr.get_related_file_types(product);
        System.out.println("Related File Types : ");
        for (int i = 0; i < related_file_types.length; i++) {
            System.out.println(related_file_types[i]);
        }
        System.out.println();
    }

    public void get_related_files(ORB orb, Product product) throws Exception {
        System.out.println("Sending Get Related Files Request...");

        FileLocation fileLocation = new FileLocation("", "", "", "", "");
        NameValue[] properties = {new NameValue("", orb.create_any())};
        Product[] products = {product};

        GetRelatedFilesRequest relatedFilesRequest = productMgr.get_related_files(products,
                fileLocation,
                "",
                properties);
        System.out.println("Completing GetRelatedFilesRequest...");

        NameListHolder locations = new NameListHolder();

        relatedFilesRequest.complete(locations);

        System.out.println("Location List : ");
        String[] locationList = locations.value;
        for (int i = 0; i < locationList.length; i++) {
            System.out.println(locationList[i]);
        }
        System.out.println();
    }

    public OrderContents createOrder(ORB orb) throws Exception {
        NameName nameName[] = {new NameName("", "")};

        TailoringSpec tailoringSpec = new TailoringSpec(nameName);
        PackagingSpec pSpec = new PackagingSpec("", "");
        AbsTime needByDate = new AbsTime(new Date((short) 2, (short) 10, (short) 16),
                new Time((short) 10, (short) 0, (short) 0));

        MediaType[] mTypes = {new MediaType("", (short) 1)};
        String[] benums = {""};
        AlterationSpec aSpec = new AlterationSpec("",
                orb.create_any(),
                new Rectangle(new Coordinate2d(1.1, 1.1), new Coordinate2d(2.2, 2.2)),
                GeoRegionType.NULL_REGION);
        Product product = new _ProductStub();

        Destination destination = new Destination();
        destination.e_dest("");

        ProductDetails[] productDetails = {new ProductDetails(mTypes, benums, aSpec, product, "")};
        DeliveryDetails[] deliveryDetails = {new DeliveryDetails(destination, "", "")};

        OrderContents order = new OrderContents("DDF",
                tailoringSpec,
                pSpec,
                needByDate,
                "Give me an order!",
                (short) 1,
                productDetails,
                deliveryDetails);

        return order;
    }

    public String getIorTextFile(int port) throws Exception {
        String url = SERVER_PATH + port + IOR_PATH;
        System.out.println("Downloading IOR File From Server...");
        String myString = "";

        try {
            URL fileDownload = new URL(url);
            BufferedInputStream inputStream = new BufferedInputStream(fileDownload.openStream());
            myString = IOUtils.toString(inputStream, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (StringUtils.isNotBlank(myString)) {
            System.out.println("Successfully Downloaded IOR File From Server.\n");
            return myString;
        }

        throw new Exception("Error recieving IOR File");
    }

}