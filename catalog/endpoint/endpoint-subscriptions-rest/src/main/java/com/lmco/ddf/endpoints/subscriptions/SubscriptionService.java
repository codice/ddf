/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

package com.lmco.ddf.endpoints.subscriptions;


import java.io.InputStream;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.model.wadl.ElementClass;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.event.EventProcessor;
import ddf.catalog.event.Subscription;
import ddf.catalog.event.SubscriptionNotFoundException;
import ddf.subscriptions.GetAllSubscriptionsResponse;
import ddf.subscriptions.GetSubscriptionIdsResponse;



/**
 * This is a REST Service supporting JSON-formatted queries for Catalog
 * Providers.
 * 
 * This class handles HTTP POST requests for queries of catalog entries matching
 * the query's search criteria. The payload of the request is JSON-formatted and
 * can contain search criteria for Contextual, Entry, Temporal, Spatial,
 * Datatype, or MultiID queries. A Compound query is also supported, which
 * consists of up to one each of Contextual, Entry, Temporal, Spatial, Datatype.
 * (MultiID is not supported in the Compound query).
 * 
 * Searches can be done for the local site only, a specific site (by name), or
 * an enterprise-wide search (which includes all available federated sites and
 * the local site). The scope of the search is determined by the URL used to
 * execute the query.
 * 
 * @author rodgersh
 * 
 */
@Path( "/" )
public class SubscriptionService
{
    /** The logger for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger( SubscriptionService.class );
    
    /** 
     * Time (in milliseconds) to wait for the subscription's DeliveryMethod callback service to come up
     * during subscription restoration.
     */
    private static int DELIVERY_METHOD_SERVICE_TRACKER_TIMEOUT = 10000;
    
    /** The context for this class' bundle */
    private BundleContext context;

    /** The DDF subscription manager invoked to create/delete subscriptions */
    private EventProcessor eventProcessor;
    
    private Unmarshaller unmarshaller;


    /**
     * Constructor for Subscription REST service.
     * 
     * @param EventProcessor the MTS EventProcessor to use to
     *            create/update/delete/query subscriptions
     */
    public SubscriptionService( BundleContext context, EventProcessor EventProcessor )
    {
        this.context = context;
        this.eventProcessor = EventProcessor;
        
        try
        {
            JAXBContext jc = JAXBContext.newInstance( Subscription.class );
            this.unmarshaller = jc.createUnmarshaller();
        }
        catch ( JAXBException e )
        {
            LOGGER.warn( e.getMessage(), e );
        }       

    }


    @POST
    @Path( "/subscription/create" )
    public boolean createSubscription( InputStream message )
    {
        String methodName = "createSubscription";
        LOGGER.debug( "ENTERING: " + methodName );

//        Response response = null;

        try
        {
            if ( message != null )
            {
                String request = IOUtils.toString( message );
                LOGGER.debug( "request:\n" + request );
                
                Subscription subscription = (Subscription) unmarshaller.unmarshal( new StringReader( request ) );
                performCreateSubscription( subscription );
//                performCreateSubscription( request );
                
//                Response.ResponseBuilder responseBuilder = Response.ok();
//                response = responseBuilder.build();
            } 
            else 
            {
                LOGGER.warn( "message is NULL" );
            }
        }
        catch ( Exception e )
        {
            LOGGER.error( "Exception processing create subscription request", e );
//            response = Response.serverError().build();
        }

        LOGGER.debug( "EXITING: " + methodName );

//        return response;
        return true;
    }


    @POST
    @Path( "/subscriptions/create" )
    @Produces( "text/plain" )
    public int createSubscriptions( InputStream message )
    {
        String methodName = "createSubscriptions";
        LOGGER.debug( "ENTERING: " + methodName );

//        Response response = null;
        int numSubscriptionsCreated = 0;

        try
        {
            if ( message != null )
            {
                String request = IOUtils.toString( message );
                LOGGER.debug( "request:\n" + request );
                
                List<Subscription> subscriptions = (List<Subscription>) unmarshaller.unmarshal( new StringReader( request ) );
                LOGGER.debug( "subscriptions.size() = " + subscriptions.size() );
                
                if ( subscriptions != null && !subscriptions.isEmpty() )
                {
                    for ( Subscription subscription : subscriptions )
                    {
                        performCreateSubscription( subscription );
                        numSubscriptionsCreated++;
                    }
                }
                
//                Response.ResponseBuilder responseBuilder = Response.ok();
//                response = responseBuilder.build();
            } 
            else 
            {
                LOGGER.warn( "message is NULL" );
            }
        }
        catch ( Exception e )
        {
            LOGGER.error( "Exception processing create subscription request", e );
//            response = Response.serverError().build();
        }

        LOGGER.debug( "EXITING: " + methodName );

//        return response;
        return numSubscriptionsCreated;
    }
    

    // Update subscription actually consists of deleting existing current subscription and creating a new subscription.
    // The subscriptionId passed in should match the subscription ID in the message's ServiceCallback.subscriptionId element,
    // but this is not checked or enforced.
    @PUT
    @Path( "/subscription/update/{subscriptionId}" )
    public Response updateSubscription( @PathParam( "subscriptionId" ) String subscriptionId, InputStream message )
    {
        String methodName = "updateSubscription";
        LOGGER.debug( "ENTERING: " + methodName );

        LOGGER.debug( "subscriptionId:\n" + subscriptionId );

        Response response = null;
        
        try
        {
            if ( message != null )
            {
                String request = IOUtils.toString( message );
                LOGGER.debug( "request:\n" + request );

                // Delete the existing subscription
                eventProcessor.deleteSubscription( subscriptionId );
                
                Subscription subscription = (Subscription) unmarshaller.unmarshal( new StringReader( request ) );
                performCreateSubscription( subscription );
//                performCreateSubscription( request );

                Response.ResponseBuilder responseBuilder = Response.ok();
                response = responseBuilder.build();
            } 
            else 
            {
                LOGGER.warn( "message is NULL" );
            }
        }
        catch ( Exception e )
        {
            LOGGER.error( "Exception processing update subscription request", e );
            response = Response.serverError().build();
        }

        LOGGER.debug( "EXITING: " + methodName );

        return response;
    }


    @DELETE
    @Path( "/subscription/delete/{subscriptionId}" )
    @Produces( "text/plain" )
    public boolean deleteSubscription( @PathParam( "subscriptionId" ) String subscriptionId )
    {
        String methodName = "deleteSubscription";
        LOGGER.debug( "ENTERING: " + methodName );

        LOGGER.debug( "subscriptionId:\n" + subscriptionId );
        
        boolean status = false;

//        Response response = null;
        
        // Delete the existing subscription
        try {
			eventProcessor.deleteSubscription( subscriptionId);
            status = true;

		} catch (SubscriptionNotFoundException e) {
	        LOGGER.error( "SubscriptionNotFoundExeption", e );

		}
        
        
        
//        Response.ResponseBuilder responseBuilder = Response.noContent();
//        response = responseBuilder.build();

        LOGGER.debug( "EXITING: " + methodName );

//        return response;
        return status;
    }


    @DELETE
    @Path( "/subscriptions/delete" )
    @Produces( "text/plain" )
    public int deleteSubscriptions( @QueryParam("subscriptionIds") List<String> subscriptionIds )
    {
        String methodName = "deleteSubscriptions";
        LOGGER.debug( "ENTERING: " + methodName );
        
        LOGGER.debug( "subscriptionIds.size() = " + subscriptionIds.size() );

//        Response response = null;
        int numSubscriptionsDeleted = 0;
        
        // Delete the existing subscriptions
        if ( subscriptionIds != null && !subscriptionIds.isEmpty() )
        {
            for ( String subscriptionId : subscriptionIds )
            {
                LOGGER.debug( "Deleting subscription for ID = " + subscriptionId );
                 try {
					eventProcessor.deleteSubscription( subscriptionId );
                    numSubscriptionsDeleted++;
				} catch (SubscriptionNotFoundException e) {
			        LOGGER.error( "SubscriptionNotFoundExeption", e );

				}
                
                
            }
        }
        
//        Response.ResponseBuilder responseBuilder = Response.noContent();
//        response = responseBuilder.build();

        LOGGER.debug( "EXITING: " + methodName );

//        return response;
        return numSubscriptionsDeleted;
    }


    @GET
    @Path( "/subscription/{subscriptionId}" )
    @Produces( "text/xml" )
//    public Response getSubscription( @PathParam( "subscriptionId" ) String subscriptionId )
    public String getSubscription( @PathParam( "subscriptionId" ) String subscriptionId )
        throws SubscriptionServiceException
    {
        String methodName = "getSubscription - HUGH";
        LOGGER.debug( "ENTERING: " + methodName );

//        Response response = null;
//
//        String subscriptionXml = EventProcessor.getSubscription( subscriptionId );
//        
//        logger.debug( "subscriptionXml = " + subscriptionXml );
//        
//        Response.ResponseBuilder responseBuilder = Response.ok( subscriptionXml );
//        response = responseBuilder.build();
        
        String subscriptionXml = null; 
            
//        try
//        {
//  TODO: How do you get a subscription?          subscriptionXml = eventProcessor.getSubscription( subscriptionId );
//        }
//        catch ( Exception e )
//        {
//            throw new SubscriptionServiceException( e.getMessage(), e );
//        }
//        
        LOGGER.debug( "EXITING: " + methodName );

//        return response;
        return subscriptionXml;
    }


    // Get all subscriptions
    @GET
    @Path( "/subscriptions" )
    @Produces( "text/xml" )
    @ElementClass( response=ddf.subscriptions.GetAllSubscriptionsResponse.class )
//    public Response getSubscriptions()
    public GetAllSubscriptionsResponse getSubscriptions()
        throws SubscriptionServiceException
    {
        String methodName = "getSubscriptions - HUGH";
        LOGGER.debug( "ENTERING: " + methodName );

//        Response response = null;
        GetAllSubscriptionsResponse getAllSubscriptionsResponse = new GetAllSubscriptionsResponse();
        
//        try
//        {
//            Collection<String> subscriptions = eventProcessor.getAllSubscriptions();
//            List<String> subscriptionsList = getAllSubscriptionsResponse.getSubscriptions();
//            subscriptionsList.addAll( subscriptions );
//        }
//        catch ( Exception e )
//        {
//            throw new SubscriptionServiceException( e.getMessage(), e );
//        }
        
//        try
//        {
//            JAXBContext jc = JAXBContext.newInstance( GetAllSubscriptionsResponse.class );
//            Marshaller marshaller = jc.createMarshaller();
//            
//            //Marshal object into XML file
//            ByteArrayOutputStream bos = new ByteArrayOutputStream();
//            marshaller.marshal( getAllSubscriptionsResponse, bos );
//            logger.debug( "GetAllSubscriptionsResponse XML:\n" + bos.toString() );
//            
//            Response.ResponseBuilder responseBuilder = Response.ok( bos.toString() );
//            response = responseBuilder.build();
//        }
//        catch ( JAXBException e )
//        {
//            logger.warn( e.getMessage(), e );
//        }

        LOGGER.debug( "EXITING: " + methodName );

//        return response;
        return getAllSubscriptionsResponse;
    }


    // Get all subscription IDs
    @GET
    @Path( "/subscriptions/ids" )
    @Produces( "text/xml" )
    @ElementClass( response=ddf.subscriptions.GetSubscriptionIdsResponse.class )
//    public Response getSubscriptionIds()
    public GetSubscriptionIdsResponse getSubscriptionIds()
        throws SubscriptionServiceException
    {
        String methodName = "getSubscriptionIds - HUGH";
        LOGGER.debug( "ENTERING: " + methodName );

//        Response response = null;
        GetSubscriptionIdsResponse getSubscriptionIdsResponse = new GetSubscriptionIdsResponse();
        
//        try
//        {
//            Set<String> subscriptionIds = eventProcessor.getSubscriptionIds();
//            List<String> subscriptionIdsList = getSubscriptionIdsResponse.getSubscriptionIds();
//            subscriptionIdsList.addAll( subscriptionIds );
//        }
//        catch ( Exception e )
//        {
//            throw new SubscriptionServiceException( e.getMessage(), e );
//        }
        
//        try
//        {
//            JAXBContext jc = JAXBContext.newInstance( GetSubscriptionIdsResponse.class );
//            Marshaller marshaller = jc.createMarshaller();
//            
//            //Marshal object into XML file
//            ByteArrayOutputStream bos = new ByteArrayOutputStream();
//            marshaller.marshal( getSubscriptionIdsResponse, bos );
//            logger.debug( "GetSubscriptionIdsResponse XML:\n" + bos.toString() );
//            
//            Response.ResponseBuilder responseBuilder = Response.ok( bos.toString() );
//            response = responseBuilder.build();
//        }
//        catch ( JAXBException e )
//        {
//            logger.warn( e.getMessage(), e );
//        }

        LOGGER.debug( "EXITING: " + methodName );

//        return response;
        return getSubscriptionIdsResponse;
    }

    
    //private void performCreateSubscription( String request )
    private void performCreateSubscription( Subscription subscription )
    {
        String methodName = "performCreateSubscription";
        LOGGER.debug( "ENTERING: " + methodName );
        //TODO UPDATE to new OGC FILTER
//        
//        try
//        {
//            //Subscription subscription = (Subscription) unmarshaller.unmarshal( new StringReader( request ) );
//            
//            //TODO make persistSubscription an arg to this URL/method - default to true for now
//            Subject user = null;
//            
//            //TODO Determine how to implement DeliveryMethod
//            String callbackUrl = subscription.getServiceCallback().getCallbackUrl();
//            String subscriptionId = subscription.getServiceCallback().getSubscriptionId();
//            //HUGH DeliveryMethod deliveryMethod = new WebServiceAdapter( callbackUrl, subscriptionId );
//            
//            // Retrieve the Managed Service Factory PID from the subscription's callback info configuration.
//            // This PID is used to instantiate the subscription's DeliveryMethod callback service
//            String serviceCallbackFactoryPid = subscription.getServiceCallback().getFactoryPid();
//            logger.info( "serviceCallbackFactoryPid = " + serviceCallbackFactoryPid );
//            
//            ServiceReference configAdminRef = context.getServiceReference( ConfigurationAdmin.class.getName() );
//                
//            if ( serviceCallbackFactoryPid != null && configAdminRef != null )
//            {
//                ConfigurationAdmin configAdmin = (ConfigurationAdmin) context.getService( configAdminRef );
//
//                // Instantiate the DeliveryMethod callback service for this subscription
//                Configuration deliveryMethodConfig = configAdmin.createFactoryConfiguration( serviceCallbackFactoryPid, null );
//                logger.info( "Adding DeliveryMethod props to dictionary" );
//
//                // Configure the properties for the DeliveryMethod callback service
//                Dictionary<String,String> deliveryMethodProps = new Hashtable<String,String>();
//                deliveryMethodProps.put( "subscriptionId", subscription.getServiceCallback().getSubscriptionId() );
//                deliveryMethodProps.put( "callbackUrl", subscription.getServiceCallback().getCallbackUrl() );
//                deliveryMethodConfig.update( deliveryMethodProps );
//                logger.info( "Done configuring DeliveryMethod for " + serviceCallbackFactoryPid );
//                                                  
//                try
//                {
//                    // Setup a ServiceTracker to monitor for the creation of the DeliveryMethod
//                    // service callback services for each subscription being restored
//                    ServiceTracker deliveryMethodServiceTracker = new ServiceTracker( context, DeliveryMethod.class.getName(), null );
//                    
//                    // Start the DeliveryMethod service tracker to detect when the DeliveryMethod callback service
//                    // just instantiated is registered and functional
//                    deliveryMethodServiceTracker.open();
//                    logger.info( "Waiting up to " + DELIVERY_METHOD_SERVICE_TRACKER_TIMEOUT + " for DeliveryMethod service to be detected" );
//                    DeliveryMethod deliveryMethod = (DeliveryMethod) deliveryMethodServiceTracker.waitForService( DELIVERY_METHOD_SERVICE_TRACKER_TIMEOUT );
//                    
//                    if ( deliveryMethod != null )
//                    {
//                        // Detected the DeliveryMethod callback service is up - go create the subscription.
//                        // (The "true" argument indicates this subscription should be persisted.
//                        .createSubscription( user, subscription, deliveryMethod, true );
//                        logger.info( "Subscription created for id = " + subscription.getServiceCallback().getSubscriptionId() );
//                    }
//                    else
//                    {
//                        logger.info( "ServiceTracker never found a DeliveryMethod - cannot create subscription for ID = " + 
//                            subscription.getServiceCallback().getSubscriptionId() );
//                    }
//                    
//                    deliveryMethodServiceTracker.close();
//                }
//                catch ( InterruptedException e )
//                {
//                  logger.error( "Unable to setup DeliveryMethod ServiceTracker", e );
//                }
//            }
//        }
////        catch ( JAXBException e )
////        {
////            logger.warn( e.getMessage(), e );
////        }
//        catch ( IOException e )
//        {
//            logger.warn( "Error creating subscription", e );
//        }
//        
        LOGGER.debug( "EXITING: " + methodName );
    }
    
}
