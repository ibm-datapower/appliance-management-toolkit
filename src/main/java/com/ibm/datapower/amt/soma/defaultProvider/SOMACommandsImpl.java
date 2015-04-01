/**
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.ibm.datapower.amt.soma.defaultProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.amp.DeviceContext;
import com.ibm.datapower.amt.clientAPI.Manager;
import com.ibm.datapower.amt.logging.LoggerHelper;
import com.ibm.datapower.amt.soma.SOAPHelper;
import com.ibm.datapower.amt.soma.SOAPHelperFactory;
import com.ibm.datapower.amt.soma.SOMACommands;
import com.ibm.datapower.amt.soma.SOMAException;
import com.ibm.datapower.amt.soma.SOMAIOException;
import com.ibm.datapower.amt.soma.Status;

/**
 * Implements the SOMACommands interface specified in
 * {@link com.ibm.datapower.amt.soma.SOMACommands}. The device has a schema which describes
 * the format and allowed values for the SOAP messages, see
 * store:/app-mgmt-protocol.xsd and store:/app-mgmt-protocol.wsdl.
 * <p>
 * 
 * @see com.ibm.datapower.amt.soma.SOMACommands
 */

public class SOMACommandsImpl implements SOMACommands {
    private SOAPHelper soapHelper;
    
    public static final String COPYRIGHT_2013 = Constants.COPYRIGHT_2013;        
    private static final String CLASS_NAME = SOMACommandsImpl.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }
    
    public SOMACommandsImpl(String soapHelperImplementationClassName) 
    throws SOMAException{        
        final String METHOD_NAME = "SOMACommandsImpl"; //$NON-NLS-1$        
        soapHelper = SOAPHelperFactory.getSOAPHelper(soapHelperImplementationClassName);        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "soapHelper object created"); //$NON-NLS-1$        
    }
    
    /**
     * 
     */
    public Map<String, List<Status>> getAllStatus(DeviceContext device, String domainName)
    throws SOMAException, SOMAIOException {
    	final String METHOD_NAME = "getAllStatus"; //$NON-NLS-1$    	
    	logger.entering(CLASS_NAME, METHOD_NAME, new Object[]{device, domainName});        
        
        String sMessage = "<man:request domain=\"" + domainName + "\" xmlns:man=\"http://www.datapower.com/schemas/management\"> " + 
        					"<man:get-status /> </man:request>"; 
        	
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "SOMA Request created"); //$NON-NLS-1$        
        // Send request to device
        StringBuffer outMessage = new StringBuffer(sMessage);
        Node responseNode=null;
		try {
			responseNode = soapHelper.call(device, outMessage);
		} catch (SOMAIOException e1) {
			Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.soma.defaultProvider.SOAPHelper.ioExConnect",params);
        	SOMAIOException ex = new SOMAIOException(message,e1,"wamt.soma.defaultProvider.SOAPHelper.ioExConnect",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;                   
		} catch (SOMAException e1) {
			Object[] params = {device.getHostname()};
			String message = Messages.getString("wamt.soma.defaultProvider.SOAPHelper.envNotSchemaValid",params);
            SOMAException e = new SOMAException(message,"wamt.soma.defaultProvider.SOAPHelper.envNotSchemaValid",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw e;
		}        
        outMessage.delete(0,outMessage.length());
        outMessage = null;
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "SOMA Response received"); //$NON-NLS-1$                
        if (responseNode == null){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.soma.defaultProvider.CommandsImpl.noResponse",params); 
        	SOMAException e = new SOMAException(message,"wamt.soma.defaultProvider.CommandsImpl.noResponse",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw e;
        }        
        
        // Parse the response
        NodeList ResponseNodeList = responseNode.getChildNodes();
        if (ResponseNodeList == null){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.soma.defaultProvider.CommandsImpl.noState",params); 
        	SOMAException e = new SOMAException(message,"wamt.soma.defaultProvider.CommandsImpl.noState",params); //$NON-NLS-1$ //$NON-NLS-2$            	
            throw e;
        }
                
        Map<String, List<Status>> resultMap = new HashMap<String, List<Status>>();                
        for (int i=0; i < ResponseNodeList.getLength(); i++) {
			String sStatus = ResponseNodeList.item(i).getNodeName();
			if ( sStatus.equals("dp:status") ) {
				NodeList StatusNodeList = ResponseNodeList.item(i).getChildNodes();
				String sOldOpName = ""; // Set to empty string for the first time to get status
				List<Status> resultList = null;
				for (int j=0; j < StatusNodeList.getLength(); j++) {
					// Get name of SOMA operation					
					String sOpName = StatusNodeList.item(j).getNodeName();		
					//System.out.println(">>>" + sOpName);
					if ( !sOpName.equals(sOldOpName) ) {
						// Get the status of next operation
						if ( sOldOpName.length() > 0 )
							resultMap.put(sOldOpName, resultList); // Save the previous status of operation						 
						resultList = null;	
						sOldOpName = sOpName;
					}
					if ( resultList == null )
						resultList = new ArrayList<Status>();
					
					// Geet key/value pair of SOMA status
					StatusImpl somaStatus = new StatusImpl();
					NodeList nodesList = StatusNodeList.item(j).getChildNodes(); 					
					for (int k=0; k < nodesList.getLength(); k++) {
						Node node = nodesList.item(k);   
						if ( node.getNodeType() == Node.ELEMENT_NODE ) {
							String sKey = node.getNodeName();							
							if ( node.getFirstChild() != null ) {
								String sValue = node.getFirstChild().getNodeValue();	
								sValue = sValue.trim();								
								somaStatus.add(sKey, sValue);
							}
							else
								somaStatus.add(sKey, "");
						}
					}
					resultList.add(somaStatus);
				}
				// Add the last one
				resultMap.put(sOldOpName, resultList);
			}
			else if ( sStatus.equals("dp:result") ) { // Something wrong, can not get status
				NodeList resultNodeList = ResponseNodeList.item(i).getChildNodes();
				String reason = resultNodeList.item(0).getNodeValue(); // Get the reason
				Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort()), reason};
	        	String message = Messages.getString("wamt.soma.defaultProvider.CommandsImpl.errorGetStatus",params); 
	        	SOMAException e = new SOMAException(message,"wamt.soma.defaultProvider.CommandsImpl.errorGetStatus",params); //$NON-NLS-1$ //$NON-NLS-2$            	
	            throw e;
			}
		}        
        logger.exiting(CLASS_NAME, METHOD_NAME, resultMap.size());
    	return resultMap;
    }
    
    /**
     * 
     */
    public List<Status> getStatus(DeviceContext device, String domainName, String OpName)
    throws SOMAIOException, SOMAException {		
    	final String METHOD_NAME = "getStatus"; //$NON-NLS-1$                
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[]{device, domainName, OpName});
        
        String sMessage = "<man:request domain=\"" + domainName + "\" xmlns:man=\"http://www.datapower.com/schemas/management\"> " + 
        					"<man:get-status class=\"" + OpName + "\"/>" + 
        					"</man:request>"; 
        	
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "SOMA Request created"); //$NON-NLS-1$        
        // Send request to device
        StringBuffer outMessage = new StringBuffer(sMessage);
        Node responseNode = null;        
        try {
			responseNode = soapHelper.call(device, outMessage);
		} catch (SOMAIOException e1) {
			Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.soma.defaultProvider.SOAPHelper.ioExConnect",params);
        	SOMAIOException ex = new SOMAIOException(message,e1,"wamt.soma.defaultProvider.SOAPHelper.ioExConnect",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;                   
		} catch (SOMAException e1) {
			Object[] params = {device.getHostname()};
			String message = Messages.getString("wamt.soma.defaultProvider.SOAPHelper.envNotSchemaValid",params);
            SOMAException e = new SOMAException(message,"wamt.soma.defaultProvider.SOAPHelper.envNotSchemaValid",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw e;
		} 
        outMessage.delete(0,outMessage.length());
        outMessage = null;
                
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "SOMA Response received"); //$NON-NLS-1$
        if (responseNode == null){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.soma.defaultProvider.CommandsImpl.noResponse",params);
        	SOMAException e = new SOMAException(message,"wamt.soma.defaultProvider.CommandsImpl.noResponse",params); //$NON-NLS-1$ //$NON-NLS-2$
        	logger.throwing(CLASS_NAME, METHOD_NAME, e);
        	throw e;
        }
        
        // Parse the response
        NodeList ResponseNodeList = responseNode.getChildNodes();
        if (ResponseNodeList == null){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.soma.defaultProvider.CommandsImpl.noState",params);
        	SOMAException e = new SOMAException(message,"wamt.soma.defaultProvider.CommandsImpl.noState",params); //$NON-NLS-1$ //$NON-NLS-2$
        	throw e;
        }
            
        List<Status> result = new ArrayList<Status>();
        for (int i = 0; i < ResponseNodeList.getLength(); i++) {
        	String sStatus = ResponseNodeList.item(i).getNodeName();
        	if ( sStatus.equals("dp:status") ) {
        		NodeList nodesSOMA = ResponseNodeList.item(i).getChildNodes();        		
        		for (int j = 0; j < nodesSOMA.getLength(); j++) {
        			// Get status of SOMA
        			StatusImpl status = new StatusImpl();
        			NodeList nodes = nodesSOMA.item(j).getChildNodes();
        			for (int k = 0; k < nodes.getLength(); k++) {
        				Node node = nodes.item(k);
        				if ( node.getNodeType() == Node.ELEMENT_NODE ) {
        					String sKey = node.getNodeName();
        					if ( node.getFirstChild() != null ) {
        						String sValue = node.getFirstChild().getNodeValue();
        						sValue = sValue.trim();							
								status.add(sKey, sValue);
        					}
        					else
        						status.add(sKey, "");
        				}
        			}
        			result.add(status);
        		}
        	}
        	else if ( sStatus.equals("dp:result") ) { 
				// Something wrong, can not get status
				NodeList resultNodeList = ResponseNodeList.item(i).getChildNodes();
				Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort()), resultNodeList.item(0).getNodeValue()};
	        	String message = Messages.getString("wamt.soma.defaultProvider.CommandsImpl.errorGetStatus",params); 
	        	SOMAException e = new SOMAException(message,"wamt.soma.defaultProvider.CommandsImpl.errorGetStatus",params); //$NON-NLS-1$ //$NON-NLS-2$            	
	            throw e;
			}
        }
        logger.exiting(CLASS_NAME, METHOD_NAME, result.size());
        return result;
    }
}
